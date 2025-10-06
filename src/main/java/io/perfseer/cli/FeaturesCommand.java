package io.perfseer.cli;

import io.perfseer.aggregate.Aggregator;
import io.perfseer.ingest.JaegerClient;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "features", description = "Aggregate spans into features")
@Dependent
@Unremovable
public class FeaturesCommand implements Runnable {
    @Inject JaegerClient jaeger;
    @Inject Aggregator agg;
    @CommandLine.Option(names = {"-s","--service"}) String service;
    @CommandLine.Option(names = {"-l","--lookback"}) String lookback;

    @Override
    public void run() {
        jaeger.fetchTraces(service, lookback)
                .onItem().transform(agg::aggregate)
                .onItem().invoke(list -> list.forEach(f -> System.out.println(f.toMap())))
                .await().indefinitely();
    }
}
