package at.sti2.model.benchmark_result;

import at.sti2.configuration.TestCaseConfiguration;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TestCaseResult {

    private String name;

    private long materializationTime;
    private Map<String, QueryResult> queryResults;

    public TestCaseResult(TestCaseConfiguration testCase) {
        this(testCase.getName(), -1, new HashMap<>());
    }

    public void addQueryResult(QueryResult queryResult) {
        queryResults.put(queryResult.getQuery(), queryResult);
    }

}