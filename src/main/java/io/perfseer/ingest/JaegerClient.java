package io.perfseer.ingest;

import io.perfseer.config.PerfConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.Map;

@ApplicationScoped
public class JaegerClient {
    private final WebClient client;
    private final PerfConfig cfg;
    private final URI base;

    @Inject
    public JaegerClient(Vertx vertx, PerfConfig cfg) {
        this.cfg = cfg;
        this.base = URI.create(cfg.jaegerUrl());
        WebClientOptions options = new WebClientOptions()
                .setDefaultHost(base.getHost())
                .setDefaultPort(base.getPort() > 0 ? base.getPort() : ("https".equals(base.getScheme()) ? 443 : 80))
                .setSsl("https".equalsIgnoreCase(base.getScheme()));
        this.client = WebClient.create(vertx, options);
    }

    public Uni<JsonObject> fetchTraces(String service, String lookback) {
        String path = base.getPath() + cfg.jaegerQueryPath();
        Map<String, String> query = Map.of(
                "service", service != null ? service : cfg.serviceName(),
                "lookback", lookback != null ? lookback : cfg.lookbackDefault()
        );
        return client.get(path)
                .addQueryParam("service", query.get("service"))
                .addQueryParam("lookback", query.get("lookback"))
                .send()
                .onItem().transform(HttpResponse::bodyAsJsonObject);
    }
}
