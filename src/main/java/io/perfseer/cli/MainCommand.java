package io.perfseer.cli;

import io.perfseer.aggregate.Aggregator;
import io.perfseer.config.PerfConfig;
import io.perfseer.ingest.JaegerClient;
import io.perfseer.ml.IsolationScorer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "perfseer", mixinStandardHelpOptions = true,
        subcommands = {MainCommand.Fetch.class, MainCommand.Features.class, MainCommand.Score.class})
@Dependent
public class MainCommand implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Inject PerfConfig config;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    @CommandLine.Command(name = "fetch", description = "Fetch raw traces from Jaeger")
    public static class Fetch implements Runnable {
        @Inject JaegerClient jaeger;
        @CommandLine.Option(names = {"-s","--service"}) String service;
        @CommandLine.Option(names = {"-l","--lookback"}) String lookback;
        @Override public void run() {
            Uni<?> u = jaeger.fetchTraces(service, lookback).onItem().invoke(j -> {
                System.out.println(j.encodePrettily());
            });
            u.await().indefinitely();
        }
    }

    @CommandLine.Command(name = "features", description = "Aggregate spans into features")
    public static class Features implements Runnable {
        @Inject JaegerClient jaeger; @Inject Aggregator agg;
        @CommandLine.Option(names = {"-s","--service"}) String service;
        @CommandLine.Option(names = {"-l","--lookback"}) String lookback;
        @Override public void run() {
            jaeger.fetchTraces(service, lookback)
                    .onItem().transform(agg::aggregate)
                    .onItem().invoke(list -> list.forEach(f -> System.out.println(f.toMap())))
                    .await().indefinitely();
        }
    }

    @CommandLine.Command(name = "score", description = "Train isolation forest on baseline and score current")
    public static class Score implements Runnable {
        @Inject JaegerClient jaeger; @Inject Aggregator agg; @Inject IsolationScorer scorer;
        @CommandLine.Option(names = {"-s","--service"}) String service;
        @CommandLine.Option(names = {"--baseline"}) String baseline;
        @CommandLine.Option(names = {"--current"}) String current;
        @Override public void run() {
            var baseU = jaeger.fetchTraces(service, baseline).onItem().transform(agg::aggregate);
            var curU = jaeger.fetchTraces(service, current).onItem().transform(agg::aggregate);
            Uni.combine().all().unis(baseU, curU).asTuple()
                    .onItem().invoke(t -> {
                        var model = scorer.train(t.getItem1());
                        var scored = scorer.score(model, t.getItem2());
                        scored.forEach(sf -> System.out.println(Map.of(
                                "key", sf.key,
                                "score", sf.score,
                                "features", sf.featureValues
                        )));
                    }).await().indefinitely();
        }
    }
}
