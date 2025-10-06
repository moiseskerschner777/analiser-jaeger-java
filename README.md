# Jaeger Performance Analysis Solution

## Overview

This solution implements a comprehensive Java performance analyzer that correlates Jaeger tracing data with actual Java source code to provide detailed performance assessments and recommendations. The system does **not** use Kafka as required, instead directly accessing Jaeger through its HTTP API.

## Architecture

### Core Components

1. **JaegerClient** - Fetches trace data directly from Jaeger HTTP API (port 16686)
2. **CodeIndexer** - Parses Java projects and indexes all methods with their fully qualified names
3. **Aggregator** - Processes Jaeger trace data and extracts performance metrics
4. **PerformanceAnalyzer** - Correlates trace data with source code and provides analysis
5. **CLI Commands** - User interface for different analysis operations

### Key Features

- **No Kafka Dependency**: Direct HTTP communication with Jaeger
- **Complete Method Indexing**: Analyzes all Java methods, not just REST endpoints
- **Performance Correlation**: Matches Jaeger operation names with Java method signatures
- **Intelligent Analysis**: Provides performance assessments and specific recommendations
- **Multiple Input Sources**: Supports both JSON files and live Jaeger data

## Building the Application

### Prerequisites

- Java 17 or higher
- Maven 3.6.3 or higher

### Build Instructions

To generate the JAR file, run the following command in the project root:

```bash
mvn clean package -DskipTests
```

This will create the executable JAR at `target/quarkus-app/quarkus-run.jar`.

### Running the Application

After building, you can run the application using:

```bash
java -jar target/quarkus-app/quarkus-run.jar analyze \
  --project /home/moises/Documents/moises/quarkus-iris-monitor-system/quarkus-iris-monitor-system \
  --service monitoring-system \
  --lookback 1h
```

## Usage

### Command Line Interface

The system provides several commands:

#### 1. Analyze Command (New - Main Feature)
```bash
java -jar target/analiser-jaeger-java-dev.jar analyze \
  --project /home/moises/Documents/moises/quarkus-iris-monitor-system/quarkus-iris-monitor-system \
  --service monitoring-system \
  --lookback 1h
```

#### 2. Other Existing Commands
- `fetch` - Retrieve raw traces from Jaeger
- `features` - Aggregate spans into performance features
- `score` - Train isolation forest for anomaly detection

## Implementation Details

### Enhanced CodeIndexer

The original CodeIndexer only captured REST endpoints. The enhanced version:

- **Captures All Methods**: Indexes every method in the Java project
- **Fully Qualified Names**: Builds complete method signatures matching Jaeger format
- **Method Body Extraction**: Stores source code for analysis
- **Package Resolution**: Handles package declarations correctly

### PerformanceAnalyzer

New service that provides the core functionality:

- **Method Correlation**: Matches Jaeger operation names with indexed methods
- **Performance Assessment**: Categorizes methods as GOOD/MODERATE/POOR
- **Duration Analysis**: Evaluates method execution times (>10ms = moderate, >100ms = high)
- **Error Rate Analysis**: Identifies methods with high failure rates
- **Code Pattern Analysis**: Detects performance anti-patterns in source code

### Analysis Logic

The analyzer evaluates methods based on:

1. **Execution Duration**:
   - Good: < 10ms
   - Moderate: 10ms - 100ms  
   - Poor: > 100ms

2. **Error Rates**:
   - Good: < 5%
   - Moderate: 5% - 10%
   - Poor: > 10%

3. **Code Patterns**:
   - SQL IN clauses (potential N+1 queries)
   - Complex stream operations
   - Native queries without proper indexing

## Example Analysis

Given the provided Jaeger trace data:

```json
{
  "operationName": "org.iris.patient.repository.PatientRepository.findMedicationTextByPatient",
  "duration": 551
}
```

The analyzer will:

1. **Locate the Method**: Find `findMedicationTextByPatient` in the indexed Java project
2. **Extract Performance**: Duration = 551μs (0.551ms)
3. **Assess Performance**: GOOD (under 10ms threshold)
4. **Analyze Code**: Detect SQL patterns and stream operations
5. **Generate Recommendations**: Specific advice based on code structure

### Expected Output

```
Method: org.iris.patient.repository.PatientRepository.findMedicationTextByPatient
File: src/main/java/org/iris/patient/repository/PatientRepository.java:42
Performance Assessment: GOOD
Duration: 551μs (0.551ms)
Call Count: 1
Error Rate: 0.00%
Recommendations:
  - Native query detected. Ensure proper indexing on queried columns for optimal performance.
  - Multiple stream operations detected. Consider optimizing the stream pipeline for better performance.

Method Body Preview:
  public List<String> findMedicationTextByPatient(String patientKey) {
      List<String> resourceJsonList = em.createNativeQuery("""
          SELECT ResourceString
          FROM HSFHIR_X0001_R.Rsrc
          WHERE Key IN (
  ... (truncated)

SUMMARY:
Total methods analyzed: 4
Good performance: 4 (100.0%)
Moderate performance: 0 (0.0%)
Poor performance: 0 (0.0%)
```

**All analysis reports are displayed directly in your terminal/console** when you run the analyze command. The tool outputs the performance analysis in real-time to `stdout`.
