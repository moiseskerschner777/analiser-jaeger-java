package io.perfseer.api;

import io.perfseer.aggregate.Aggregator;
import io.perfseer.ingest.JaegerClient;
import io.perfseer.ml.IsolationScorer;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api")
public class ScoreResource {

    @Inject
    JaegerClient jaeger;
    @Inject
    Aggregator aggregator;
    @Inject
    IsolationScorer scorer;

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Object> health(){
        Map<String,Object> m = new HashMap<>();
        m.put("status", "ok");
        return m;
    }

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Map<String,Object>>> features(@QueryParam("service") String service,
                                                  @QueryParam("lookback") String lookback){
        return jaeger.fetchTraces(service, lookback)
                .onItem().transform(aggregator::aggregate)
                .onItem().transform(list -> list.stream().map(Aggregator.Feature::toMap).collect(Collectors.toList()));
    }

    @GET
    @Path("/score")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Map<String,Object>>> score(@QueryParam("service") String service,
                                               @QueryParam("lookbackBaseline") String lookbackBaseline,
                                               @QueryParam("lookbackCurrent") String lookbackCurrent){
        Uni<List<Aggregator.Feature>> baseline = jaeger.fetchTraces(service, lookbackBaseline)
                .onItem().transform(aggregator::aggregate);
        Uni<List<Aggregator.Feature>> current = jaeger.fetchTraces(service, lookbackCurrent)
                .onItem().transform(aggregator::aggregate);
        return Uni.combine().all().unis(baseline, current).asTuple()
                .onItem().transform(tuple -> {
                    var model = scorer.train(tuple.getItem1());
                    var scored = scorer.score(model, tuple.getItem2());
                    return scored.stream().map(sf -> {
                        Map<String,Object> m = new HashMap<>();
                        m.put("key", sf.key);
                        m.put("score", sf.score);
                        m.put("features", sf.featureValues);
                        return m;
                    }).collect(Collectors.toList());
                });
    }
}
