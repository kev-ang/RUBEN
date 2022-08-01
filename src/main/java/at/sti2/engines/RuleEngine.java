package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import java.util.Map;

/**
 * Interface a rule engine needs to implement to be includable in the
 * benchmarking framework.
 *
 * @author kevin.angele@sti2.at
 */
public interface RuleEngine {

    String getEngineIdentifier();

    /**
     * Returns the name of the engine as defined in the configuration.
     *
     * @return engine name
     */
    String getEngineName();

    /**
     * Allows to set the name of the rule engine.
     *
     * @param engineName to be set
     */
    void setEngineName(String engineName);

    /**
     * Rule engine specific settings are provided via this method. The rule
     * engine gets those settings in a map consisting of key-value pairs.
     *
     * @param settings to be used for the rule engine
     */
    void setSettings(Map<String, Object> settings);

    /**
     * Based on the given test data and the current test case, the rule engine
     * needs to load the relevant data and prepare everything for the execution
     * of queries.
     *
     * @param testDataPath directory containing all the test data.
     * @param testCase     to be prepared.
     */
    void prepare(String testDataPath, TestCaseConfiguration testCase);

    /**
     * Executes a single query. As a return value the number of results need to
     * be returned.
     *
     * @param query to be evaluated
     * @return number of results
     * @throws Exception if something went wrong during the query evaluation
     */
    int executeQuery(String query) throws Exception;

    /**
     * This method is used to clean up the rule engine after the evaluation of a
     * test case. Caches need to be invalidated and the data removed from the
     * rule engine.
     */
    void cleanUp();

    /**
     * Stop all processes initiated by the rule engine. All temporary data that
     * was created during the evaluation needs to be cleaned up.
     */
    void shutDown();
}