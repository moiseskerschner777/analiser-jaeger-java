package io.perfseer.cli;

import io.perfseer.ingest.JaegerClient;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "fetch", description = "Fetch raw traces from Jaeger")
@Dependent
@Unremovable
public class FetchCommand implements Runnable {
    @Inject JaegerClient jaeger;
    @CommandLine.Option(names = {"-s","--service"}) String service;
    @CommandLine.Option(names = {"-l","--lookback"}) String lookback;
    @CommandLine.Option(names = {"--limit"}) Integer limit;
    @CommandLine.Option(names = {"--prettyPrint"}) Boolean prettyPrint;

    @Override
    public void run() {
        Uni<?> u = jaeger.fetchTraces(service, lookback, limit, prettyPrint).onItem().invoke(j -> {
            System.out.println(j.encodePrettily());
        });
        u.await().indefinitely();
    }
}
