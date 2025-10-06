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
        return fetchTraces(service, lookback, null, null);
    }

    public Uni<JsonObject> fetchTraces(String service, String lookback, Integer limit, Boolean prettyPrint) {
        String path = base.getPath() + cfg.jaegerQueryPath();
        var request = client.get(path)
                .addQueryParam("service", service != null ? service : cfg.serviceName())
                .addQueryParam("lookback", lookback != null ? lookback : cfg.lookbackDefault());

        if (limit != null) {
            request.addQueryParam("limit", String.valueOf(limit));
        }

        if (prettyPrint != null && prettyPrint) {
            request.addQueryParam("prettyPrint", "true");
        }

        return request.send()
                .onItem().transform(HttpResponse::bodyAsJsonObject);
    }
}
