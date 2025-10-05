package io.perfseer.indexer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@ApplicationScoped
public class CodeIndexer {

    public static class CodePointer {
        public String file;
        public int line;
        public String symbol;
    }

    public List<CodePointer> indexProject(String rootDir) {
        List<CodePointer> res = new ArrayList<>();
        Path root = Paths.get(rootDir);
        if (!Files.exists(root)) return res;
        try (Stream<Path> paths = Files.walk(root)){
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(p);
                    cu.findAll(MethodDeclaration.class).forEach(m -> {
                        Optional<ClassOrInterfaceDeclaration> cls = m.findAncestor(ClassOrInterfaceDeclaration.class);
                        String symbol = cls.map(c -> c.getNameAsString()+"#"+m.getNameAsString()).orElse(m.getNameAsString());
                        for (AnnotationExpr ann : m.getAnnotations()){
                            String n = ann.getNameAsString();
                            if (n.endsWith("GET")||n.endsWith("POST")||n.endsWith("PUT")||n.endsWith("DELETE")||n.endsWith("Path")){
                                CodePointer cp = new CodePointer();
                                cp.file = root.relativize(p).toString();
                                cp.line = m.getBegin().map(pn->pn.line).orElse(1);
                                cp.symbol = symbol;
                                res.add(cp);
                                break;
                            }
                        }
                    });
                } catch (IOException e) {
                    // ignore file
                }
            });
        } catch (IOException e) {
            // ignore
        }
        return res;
    }
}
