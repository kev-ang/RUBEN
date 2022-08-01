package at.sti2.model.benchmark_result;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RuleEngineResult {

    private String name;
    private Map<String, TestCaseResult> benchmarkTestCaseResults;

    public RuleEngineResult(String name) {
        this(name, new HashMap<>());
    }

    public void addTestCaseResult(TestCaseResult testCaseResult) {
        benchmarkTestCaseResults.put(testCaseResult.getName(), testCaseResult);
    }
}