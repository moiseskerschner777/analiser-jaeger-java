package io.perfseer.cli;

import io.perfseer.analyzer.PerformanceAnalyzer;
import io.perfseer.ingest.JaegerClient;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine;

@CommandLine.Command(name = "analyze", description = "Analyze Java project methods using Jaeger trace data")
@Dependent
@Unremovable
public class AnalyzeCommand implements Runnable {
    @Inject JaegerClient jaeger;
    @Inject PerformanceAnalyzer analyzer;

    @CommandLine.Option(names = {"-p", "--project"}, description = "Path to Java project root", required = true)
    String projectPath;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Path to Jaeger JSON file")
    String jaegerFile;

    @CommandLine.Option(names = {"-s", "--service"}, description = "Service name for live Jaeger data")
    String service;

    @CommandLine.Option(names = {"-l", "--lookback"}, description = "Lookback period for live data")
    String lookback;

    @Override
    public void run() {
        try {
            io.vertx.core.json.JsonObject jaegerData;

            if (jaegerFile != null) {
                // Read from file
                String content = java.nio.file.Files.readString(java.nio.file.Paths.get(jaegerFile));
                jaegerData = new io.vertx.core.json.JsonObject(content);
                processAnalysis(jaegerData);
            } else if (service != null) {
                // Fetch from Jaeger
                jaeger.fetchTraces(service, lookback)
                        .onItem().invoke(this::processAnalysis)
                        .await().indefinitely();
            } else {
                System.err.println("Either --file or --service must be specified");
                return;
            }
        } catch (Exception e) {
            System.err.println("Error during analysis: " + e.getMessage());
        }
    }

    private void processAnalysis(io.vertx.core.json.JsonObject jaegerData) {
        List<PerformanceAnalyzer.MethodAnalysis> analyses = analyzer.analyzePerformance(projectPath, jaegerData);

        System.out.println("\n=== PERFORMANCE ANALYSIS REPORT ===\n");

        if (analyses.isEmpty()) {
            System.out.println("No methods found that match Jaeger trace data.");
            return;
        }

        for (PerformanceAnalyzer.MethodAnalysis analysis : analyses) {
            System.out.println("Method: " + analysis.fullyQualifiedName);
            System.out.println("File: " + analysis.fileName + ":" + analysis.lineNumber);
            System.out.println("Performance Assessment: " + analysis.performanceAssessment);
            System.out.println("Duration: " + analysis.duration + "μs (" + (analysis.duration/1000.0) + "ms)");
            System.out.println("Call Count: " + analysis.count);
            System.out.println("Error Rate: " + String.format("%.2f%%", analysis.errorRate * 100));

            if (!analysis.recommendations.isEmpty()) {
                System.out.println("Recommendations:");
                for (String rec : analysis.recommendations) {
                    System.out.println("  - " + rec);
                }
            }

            System.out.println("Method Body Preview:");
            String[] lines = analysis.methodBody.split("\n");
            for (int i = 0; i < Math.min(5, lines.length); i++) {
                System.out.println("  " + lines[i].trim());
            }
            if (lines.length > 5) {
                System.out.println("  ... (truncated)");
            }

            System.out.println("\n" + "=".repeat(80) + "\n");
        }

        // Summary
        long totalMethods = analyses.size();
        long poorMethods = analyses.stream().mapToLong(a -> "POOR".equals(a.performanceAssessment) ? 1 : 0).sum();
        long moderateMethods = analyses.stream().mapToLong(a -> "MODERATE".equals(a.performanceAssessment) ? 1 : 0).sum();
        long goodMethods = analyses.stream().mapToLong(a -> "GOOD".equals(a.performanceAssessment) ? 1 : 0).sum();

        System.out.println("SUMMARY:");
        System.out.println("Total methods analyzed: " + totalMethods);
        System.out.println("Good performance: " + goodMethods + " (" + String.format("%.1f%%", goodMethods*100.0/totalMethods) + ")");
        System.out.println("Moderate performance: " + moderateMethods + " (" + String.format("%.1f%%", moderateMethods*100.0/totalMethods) + ")");
        System.out.println("Poor performance: " + poorMethods + " (" + String.format("%.1f%%", poorMethods*100.0/totalMethods) + ")");
    }
}
