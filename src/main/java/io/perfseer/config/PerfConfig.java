package io.perfseer.config;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@StaticInitSafe
@ConfigMapping(prefix = "perf")
public interface PerfConfig {
    @WithDefault("http://localhost:16686")
    String jaegerUrl();

    @WithDefault("/api/traces")
    String jaegerQueryPath();

    @WithDefault("example-service")
    String serviceName();

    @WithDefault("false")
    boolean kafkaEnabled();

    @WithDefault("localhost:9092")
    String kafkaBootstrapServers();

    @WithDefault("otlp_spans")
    String kafkaTopic();

    @WithDefault("perfseer.db")
    String storageSqlitePath();

    @WithDefault("true")
    boolean routesNormalize();

    @WithDefault("1h")
    String lookbackDefault();
}
