package at.sti2.model.benchmark_result;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BenchmarkEngineResult {
    private String name;
    private Map<String, BenchmarkTestCaseResult> benchmarkTestCaseResults;

    public BenchmarkEngineResult(String name) {
        this(name, new HashMap<>());
    }

    public void addTestCaseResult(BenchmarkTestCaseResult benchmarkTestCaseResult){
        benchmarkTestCaseResults.put(benchmarkTestCaseResult.getName(), benchmarkTestCaseResult);
    }
}
