package io.perfseer.ml;

import io.perfseer.aggregate.Aggregator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import smile.anomaly.IsolationForest;

@ApplicationScoped
public class IsolationScorer {

    public static class ScoredFeature {
        public String key;
        public double score; // anomaly score
        public Map<String,Object> featureValues;
    }

    public IsolationForest train(List<Aggregator.Feature> baseline) {
        double[][] X = toMatrix(baseline);
        int ntrees = 100;
        int sampleSize = Math.min(256, X.length);
        IsolationForest forest = new IsolationForest(ntrees, sampleSize);
        forest.fit(X);
        return forest;
    }

    public List<ScoredFeature> score(IsolationForest model, List<Aggregator.Feature> current) {
        double[][] X = toMatrix(current);
        double[] s = model.score(X);
        List<ScoredFeature> out = new ArrayList<>();
        for (int i=0;i<current.size();i++){
            Aggregator.Feature f = current.get(i);
            ScoredFeature sf = new ScoredFeature();
            sf.key = f.key;
            sf.score = s[i];
            Map<String,Object> m = new HashMap<>();
            m.put("count", f.count);
            m.put("errorRate", f.errorRate);
            m.put("p50", f.p50);
            m.put("p95", f.p95);
            m.put("p99", f.p99);
            sf.featureValues = m;
            out.add(sf);
        }
        return out;
    }

    private double[][] toMatrix(List<Aggregator.Feature> list){
        double[][] X = new double[list.size()][];
        for (int i=0;i<list.size();i++){
            Aggregator.Feature f = list.get(i);
            X[i] = new double[]{
                    f.count,
                    f.errorRate,
                    f.p50,
                    f.p95,
                    f.p99
            };
        }
        return X;
    }
}
