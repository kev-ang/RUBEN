package at.sti2.benchmark;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.RuleEngine;
import at.sti2.model.benchmark_result.BenchmarkEngineResult;
import at.sti2.model.benchmark_result.BenchmarkQueryResult;
import at.sti2.model.benchmark_result.BenchmarkTestCaseResult;
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

@Slf4j
public class BenchmarkExecutor {

    public static BenchmarkEngineResult execute(String testDataPath,
                                                RuleEngine engine,
                                                List<TestCaseConfiguration> testCases) {
        BenchmarkEngineResult benchmarkEngineResult =
            new BenchmarkEngineResult(engine.getEngineName());

        log.info("Starting evaluation using benchmarking engine \"{}\" ...",
                 engine.getEngineName());

        for (TestCaseConfiguration testCase : testCases) {
            BenchmarkTestCaseResult benchmarkTestCaseResult =
                new BenchmarkTestCaseResult(testCase);
            benchmarkEngineResult.addTestCaseResult(benchmarkTestCaseResult);

            log.info("... running test case {}", testCase.getName());
            engine.prepare(testDataPath, testCase);
            Map<String, BenchmarkQueryResult> result =
                executeTestCase(testDataPath, engine, testCase);
            benchmarkTestCaseResult.getQueryResults().putAll(result);
            engine.cleanUp();
        }
        engine.shutDown();
        return benchmarkEngineResult;
    }

    private static Map<String, BenchmarkQueryResult> executeTestCase(
        String testDataPath,
        RuleEngine engine, TestCaseConfiguration testCase) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Map<String, BenchmarkQueryResult> testCaseResults = new HashMap<>();
        String queryFileClassPath =
            BenchmarkUtils.getFilePath(testDataPath,
                                       engine.getEngineIdentifier(),
                                       testCase,
                                       "_queries.json");
        QueryContainer queryContainer =
            BenchmarkUtils.load(queryFileClassPath, QueryContainer.class);
        if (queryContainer != null) {
            executeTestCaseQueries(engine, executor, testCaseResults,
                                   queryContainer);
        }
        executor.shutdownNow();
        return testCaseResults;
    }

    private static void executeTestCaseQueries(RuleEngine engine,
                                               ExecutorService executor,
                                               Map<String, BenchmarkQueryResult> testCaseResults,
                                               QueryContainer queryContainer) {
        for (Query query : queryContainer.getQueries()) {
            log.info("Evaluating query: {}", query.getQuery());
            BenchmarkQueryResult queryResultObject =
                new BenchmarkQueryResult(query.getName());
            Future<Integer> resultFuture = null;
            for (var i = 0; i < 2; i++) {
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
                }catch(Error e){
                    queryResultObject.setException(e.getMessage());
                    log.error("Error evaluating query {} with {}", query.getName(), engine.getEngineName(), e);
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