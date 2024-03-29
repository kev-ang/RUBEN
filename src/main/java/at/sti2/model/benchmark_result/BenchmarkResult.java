package at.sti2.model.benchmark_result;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BenchmarkResult {

    private Map<String, RuleEngineResult> benchmarkEngineResults;

    public BenchmarkResult() {
        this(new HashMap<>());
    }

    public void addBenchmarkEngineResult(RuleEngineResult ruleEngineResult) {
        benchmarkEngineResults.put(ruleEngineResult.getName(),
                                   ruleEngineResult);
    }
}