package io.perfseer.aggregate;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class Aggregator {

    public static class Feature {
        public String key; // endpoint or db op
        public long count;
        public double errorRate;
        public double p50;
        public double p95;
        public double p99;
        public Map<String,Object> toMap(){
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("key", key);
            m.put("count", count);
            m.put("errorRate", errorRate);
            m.put("p50", p50);
            m.put("p95", p95);
            m.put("p99", p99);
            return m;
        }
    }

    public List<Feature> aggregate(JsonObject jaegerResponse) {
        // Minimal parsing: Jaeger /api/traces data: { data: [ {spans:[{operationName, duration, tags:[]}]} ] }
        Map<String, List<Long>> latenciesByOp = new HashMap<>();
        Map<String, Long> errorsByOp = new HashMap<>();
        Map<String, Long> countsByOp = new HashMap<>();

        JsonArray data = jaegerResponse.getJsonArray("data", new JsonArray());
        for (int i=0;i<data.size();i++){
            JsonObject trace = data.getJsonObject(i);
            JsonArray spans = trace.getJsonArray("spans", new JsonArray());
            for (int j=0;j<spans.size();j++){
                JsonObject span = spans.getJsonObject(j);
                String op = span.getString("operationName", "unknown");
                long duration = span.getLong("duration", 0L);
                countsByOp.put(op, countsByOp.getOrDefault(op,0L)+1);
                latenciesByOp.computeIfAbsent(op,k->new ArrayList<>()).add(duration);
                boolean error = false;
                JsonArray tags = span.getJsonArray("tags", new JsonArray());
                for (int t=0;t<tags.size();t++){
                    JsonObject tag = tags.getJsonObject(t);
                    if ("error".equals(tag.getString("key")) && Boolean.TRUE.equals(tag.getValue("value"))) {
                        error = true; break;
                    }
                }
                if (error) errorsByOp.put(op, errorsByOp.getOrDefault(op,0L)+1);
            }
        }

        List<Feature> features = new ArrayList<>();
        for (String op: countsByOp.keySet()){
            Feature f = new Feature();
            f.key = op;
            f.count = countsByOp.getOrDefault(op,0L);
            long err = errorsByOp.getOrDefault(op,0L);
            f.errorRate = f.count>0 ? ((double)err)/f.count : 0.0;
            List<Long> lats = latenciesByOp.getOrDefault(op, Collections.emptyList());
            Collections.sort(lats);
            f.p50 = percentile(lats, 0.50);
            f.p95 = percentile(lats, 0.95);
            f.p99 = percentile(lats, 0.99);
            features.add(f);
        }
        return features;
    }

    private double percentile(List<Long> list, double p){
        if (list.isEmpty()) return 0;
        int idx = (int)Math.ceil(p*list.size()) - 1;
        idx = Math.max(0, Math.min(idx, list.size()-1));
        return list.get(idx);
    }
}
