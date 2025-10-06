package io.perfseer.indexer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class CodeIndexer {

    private final JavaParser javaParser;

    public CodeIndexer() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.javaParser = new JavaParser(config);
    }

    public static class CodePointer {
        public String file;
        public int line;
        public String symbol;
        public String fullyQualifiedName;
        public String methodBody;
    }

    public List<CodePointer> indexProject(String rootDir) {
        List<CodePointer> res = new ArrayList<>();
        Path root = Paths.get(rootDir);
        if (!Files.exists(root)) return res;
        try (Stream<Path> paths = Files.walk(root)){
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    CompilationUnit cu = javaParser.parse(p).getResult().orElseThrow();
                    cu.findAll(MethodDeclaration.class).forEach(m -> {
                        Optional<ClassOrInterfaceDeclaration> cls = m.findAncestor(ClassOrInterfaceDeclaration.class);
                        String symbol = cls.map(c -> c.getNameAsString()+"#"+m.getNameAsString()).orElse(m.getNameAsString());

                        // Build fully qualified name
                        String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse("");
                        String className = cls.map(c -> c.getNameAsString()).orElse("Unknown");
                        String methodName = m.getNameAsString();
                        String fullyQualifiedName = packageName.isEmpty() ?
                            className + "." + methodName :
                            packageName + "." + className + "." + methodName;

                        CodePointer cp = new CodePointer();
                        cp.file = root.relativize(p).toString();
                        cp.line = m.getBegin().map(pn->pn.line).orElse(1);
                        cp.symbol = symbol;
                        cp.fullyQualifiedName = fullyQualifiedName;
                        cp.methodBody = m.toString();
                        res.add(cp);
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
