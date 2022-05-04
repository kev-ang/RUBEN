package at.sti2.model.benchmark_result;

import at.sti2.configuration.TestCaseConfiguration;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BenchmarkTestCaseResult {
    private String name;
    private Map<String, BenchmarkQueryResult> queryResults;

    public BenchmarkTestCaseResult(TestCaseConfiguration testCase){
        this(testCase.getName(), new HashMap<>());
    }

    public void addQueryResult(BenchmarkQueryResult benchmarkQueryResult){
        queryResults.put(benchmarkQueryResult.getQuery(), benchmarkQueryResult);
    }

}
