package io.perfseer.cli;

import io.perfseer.aggregate.Aggregator;
import io.perfseer.ingest.JaegerClient;
import io.perfseer.ml.IsolationScorer;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.util.Map;
import picocli.CommandLine;

@CommandLine.Command(name = "score", description = "Train isolation forest on baseline and score current")
@Dependent
@Unremovable
public class ScoreCommand implements Runnable {
    @Inject JaegerClient jaeger;
    @Inject Aggregator agg;
    @Inject IsolationScorer scorer;
    @CommandLine.Option(names = {"-s","--service"}) String service;
    @CommandLine.Option(names = {"--baseline"}) String baseline;
    @CommandLine.Option(names = {"--current"}) String current;

    @Override
    public void run() {
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
