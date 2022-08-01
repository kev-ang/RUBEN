package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import java.util.Map;

public interface RuleEngine {

    String getEngineIdentifier();

    String getEngineName();

    void setEngineName(String engineName);

    void setSettings(Map<String, Object> settings);

    void prepare(String testDataPath, TestCaseConfiguration testCase);

    int executeQuery(String query) throws Exception;

    void cleanUp();

    void shutDown();
}