package at.sti2.benchmark;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.RuleEngine;
import at.sti2.model.benchmark_result.QueryResult;
import at.sti2.model.benchmark_result.RuleEngineResult;
import at.sti2.model.benchmark_result.TestCaseResult;
import at.sti2.model.query.Query;
import at.sti2.model.query.QueryContainer;
import at.sti2.utils.BenchmarkUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

/**
 * Executes all test cases for the given rule engine. Handles loading the test
 * data, executing the test cases, and shutting down the engine in the end.
 *
 * @author kevin.angele@sti2.at
 */
@Slf4j
public class BenchmarkExecutor {

    private static ExecutorService executor =
        Executors.newSingleThreadExecutor();

    /**
     * Execute all test cases for the given rule engine.
     *
     * @param testDataPath directory containing all the test data
     * @param engine       current engine to be evaluated
     * @param testCases    set of test cases
     * @return result for the given rule engine
     */
    public static RuleEngineResult execute(String testDataPath,
                                           RuleEngine engine,
                                           List<TestCaseConfiguration> testCases) {
        RuleEngineResult ruleEngineResult =
            new RuleEngineResult(engine.getEngineName());

        log.info("Starting evaluation using benchmarking engine \"{}\" ...",
                 engine.getEngineName());

        for (TestCaseConfiguration testCase : testCases) {
            TestCaseResult testCaseResult =
                new TestCaseResult(testCase);
            ruleEngineResult.addTestCaseResult(testCaseResult);

            log.info("... running test case {}", testCase.getName());
            long preparationStart = System.currentTimeMillis();
            engine.prepare(testDataPath, testCase);
            ruleEngineResult.setPreparationTime(
                System.currentTimeMillis() - preparationStart);

            log.info("starting materialization ...");
            long materializationStart = System.currentTimeMillis();
            engine.materialize(testDataPath, testCase);
            testCaseResult.setMaterializationTime(
                System.currentTimeMillis() - materializationStart);

            Map<String, QueryResult> result =
                executeTestCase(testDataPath, engine, testCase);
            testCaseResult.getQueryResults().putAll(result);
            engine.cleanUp();
        }
        engine.shutDown();
        return ruleEngineResult;
    }

    private static Map<String, QueryResult> executeTestCase(
        String testDataPath,
        RuleEngine engine, TestCaseConfiguration testCase) {

        Map<String, QueryResult> testCaseResults = new HashMap<>();

        String queryFileClassPath =
            BenchmarkUtils.getFilePath(testDataPath,
                                       BenchmarkUtils.getEngineDataFolder(
                                           engine),
                                       testCase,
                                       "_queries.json");
        QueryContainer queryContainer =
            BenchmarkUtils.load(queryFileClassPath, QueryContainer.class);
        if (queryContainer != null) {
            executeTestCaseQueries(engine, executor, testCaseResults,
                                   queryContainer);
        }
        return testCaseResults;
    }

    private static void executeTestCaseQueries(RuleEngine engine,
                                               ExecutorService executor,
                                               Map<String, QueryResult> testCaseResults,
                                               QueryContainer queryContainer) {
        for (Query query : queryContainer.getQueries()) {
            log.info("Evaluating query: {}", query.getQuery());
            Future<Integer> resultFuture = null;
            for (var i = 0; i <= 2; i++) {
                QueryResult queryResultObject =
                    new QueryResult(query.getName());
                long start = System.currentTimeMillis();
                try {
                    resultFuture =
                        executor.submit(
                            new QueryExecutionTask(engine, query.getQuery()));
                    Integer numberOfResults =
                        resultFuture.get(15, TimeUnit.MINUTES);

                    queryResultObject.setNumOfResults(numberOfResults);
                } catch (TimeoutException e) {
                    resultFuture.cancel(true);
                    queryResultObject.setException("TIMEOUT");
                    log.info("Query evaluation timed out!");
                    break;
                } catch (ExecutionException e) {
                    queryResultObject.setException(e.getMessage());
                    log.info("Execution exception thrown", e);
                    break;
                } catch (Exception e) {
                    queryResultObject.setException(e.getMessage());
                    log.error("Error evaluating query {} with {}!",
                              query.getName(), engine.getEngineName(), e);
                    break;
                } catch (Error e) {
                    queryResultObject.setException(e.getMessage());
                    log.error("Error evaluating query {} with {}",
                              query.getName(), engine.getEngineName(), e);
                } finally {
                    long end = System.currentTimeMillis();
                    queryResultObject.setTimeSpent((end - start));
                }
                testCaseResults.put(
                    query.getName() + "_" + i,
                    queryResultObject);
            }
        }
    }
}