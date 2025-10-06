package io.perfseer.analyzer;

import io.perfseer.aggregate.Aggregator;
import io.perfseer.indexer.CodeIndexer;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PerformanceAnalyzer {

    @Inject
    Aggregator aggregator;

    @Inject
    CodeIndexer codeIndexer;

    public static class MethodAnalysis {
        public String methodName;
        public String fullyQualifiedName;
        public String fileName;
        public int lineNumber;
        public String methodBody;
        public long duration; // in microseconds
        public long count;
        public double errorRate;
        public String performanceAssessment;
        public List<String> recommendations;

        public MethodAnalysis() {
            this.recommendations = new ArrayList<>();
        }
    }

    public List<MethodAnalysis> analyzePerformance(String projectRoot, JsonObject jaegerData) {
        // 1. Index all methods in the project
        List<CodeIndexer.CodePointer> codePointers = codeIndexer.indexProject(projectRoot);
        Map<String, CodeIndexer.CodePointer> methodsByFQN = codePointers.stream()
            .collect(Collectors.toMap(cp -> cp.fullyQualifiedName, cp -> cp, (a, b) -> a));

        // 2. Extract performance features from Jaeger data
        List<Aggregator.Feature> features = aggregator.aggregate(jaegerData);
        Map<String, Aggregator.Feature> featuresByOperation = features.stream()
            .collect(Collectors.toMap(f -> f.key, f -> f, (a, b) -> a));

        // 3. Correlate methods with their performance data
        List<MethodAnalysis> analyses = new ArrayList<>();

        for (Aggregator.Feature feature : features) {
            String operationName = feature.key;
            CodeIndexer.CodePointer codePointer = methodsByFQN.get(operationName);

            if (codePointer != null) {
                MethodAnalysis analysis = new MethodAnalysis();
                analysis.methodName = extractMethodName(operationName);
                analysis.fullyQualifiedName = operationName;
                analysis.fileName = codePointer.file;
                analysis.lineNumber = codePointer.line;
                analysis.methodBody = codePointer.methodBody;
                analysis.duration = (long) feature.p50;
                analysis.count = feature.count;
                analysis.errorRate = feature.errorRate;

                // Analyze performance and provide recommendations
                analyzeMethodPerformance(analysis);

                analyses.add(analysis);
            }
        }

        return analyses;
    }

    private void analyzeMethodPerformance(MethodAnalysis analysis) {
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        // Duration-based analysis (in microseconds)
        if (analysis.duration > 100000) { // > 100ms
            issues.add("HIGH_LATENCY");
            recommendations.add("Method duration (" + analysis.duration + "μs) is very high. Consider optimizing database queries or caching.");
        } else if (analysis.duration > 10000) { // > 10ms
            issues.add("MODERATE_LATENCY");
            recommendations.add("Method duration (" + analysis.duration + "μs) is moderately high. Review for potential optimizations.");
        }

        // Error rate analysis
        if (analysis.errorRate > 0.1) { // > 10%
            issues.add("HIGH_ERROR_RATE");
            recommendations.add("High error rate (" + String.format("%.2f", analysis.errorRate * 100) + "%). Check error handling and input validation.");
        } else if (analysis.errorRate > 0.05) { // > 5%
            issues.add("MODERATE_ERROR_RATE");
            recommendations.add("Moderate error rate (" + String.format("%.2f", analysis.errorRate * 100) + "%). Monitor for potential issues.");
        }

        // Code-specific analysis based on method body
        String methodBody = analysis.methodBody.toLowerCase();
        if (methodBody.contains("select") && methodBody.contains("in (")) {
            recommendations.add("SQL query uses IN clause which may be inefficient for large datasets. Consider using JOINs or batch processing.");
        }

        if (methodBody.contains("stream()") && methodBody.contains("filter") && methodBody.contains("distinct")) {
            recommendations.add("Multiple stream operations detected. Consider optimizing the stream pipeline for better performance.");
        }

        if (methodBody.contains("createnativequery")) {
            recommendations.add("Native query detected. Ensure proper indexing on queried columns for optimal performance.");
        }

        // Overall assessment
        if (issues.isEmpty()) {
            analysis.performanceAssessment = "GOOD";
        } else if (issues.contains("HIGH_LATENCY") || issues.contains("HIGH_ERROR_RATE")) {
            analysis.performanceAssessment = "POOR";
        } else {
            analysis.performanceAssessment = "MODERATE";
        }

        analysis.recommendations = recommendations;
    }

    private String extractMethodName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }
}
