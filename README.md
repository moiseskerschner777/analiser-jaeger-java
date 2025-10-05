Analiser Jaeger Java (perfseer)

A Quarkus 3.25 Java 17 tool to fetch spans from Jaeger (Query API or Kafka OTLP), aggregate features, train an Isolation Forest (Smile) and score current metrics to flag risky endpoints/DB ops. Provides CLI (Picocli) and HTTP endpoints.

Quick start
- Build: mvn clean package -DskipTests
- Run HTTP: java -jar target/quarkus-app/quarkus-run.jar
- CLI example:
  - Fetch: java -jar target/quarkus-app/quarkus-run.jar fetch --service YOUR_SERVICE --lookback 1h
  - Features: java -jar target/quarkus-app/quarkus-run.jar features --service YOUR_SERVICE --lookback 15m
  - Score: java -jar target/quarkus-app/quarkus-run.jar score --service YOUR_SERVICE --baseline 24h --current 1h

Config (env vars or flags)
- perf.jaeger.url (default http://localhost:16686)
- perf.jaeger.queryPath=/api/traces
- perf.serviceName (default example-service)
- perf.kafka.enabled=false
- perf.kafka.bootstrapServers=localhost:9092
- perf.kafka.topic=otlp_spans
- perf.storage.sqlite.path=perfseer.db
- perf.routes.normalize=true
- perf.lookback.default=1h

HTTP endpoints
- GET /api/health
- GET /api/features?service=...&lookback=1h
- GET /api/score?service=...&lookbackBaseline=24h&lookbackCurrent=1h

Notes
- Kafka ingestion is left as an optional extension point. The default path uses Jaeger Query API via Vert.x WebClient.
- SQLite is configured via dependencies; persistence layer is minimal in this initial version.
- JavaParser indexer scans a given Java project root for REST-annotated methods to produce code pointers; wire-in can be extended.
# analiser-jaeger-java
