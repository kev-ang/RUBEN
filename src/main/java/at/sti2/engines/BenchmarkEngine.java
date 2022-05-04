package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.model.benchmark_result.BenchmarkQueryResult;
import java.util.Map;

public interface BenchmarkEngine {

    String getEngineName();

    void setSettings(Map<String, Object> settings);

    void prepare(TestCaseConfiguration testCase);

    int executeQuery(String query) throws Exception;

    void cleanUp();

    void shutDown();
}
