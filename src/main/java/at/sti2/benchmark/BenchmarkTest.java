package at.sti2.benchmark;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.BenchmarkEngine;
import at.sti2.exception.MemoryLimitExceededException;
import at.sti2.model.benchmark_result.BenchmarkEngineResult;
import at.sti2.model.benchmark_result.BenchmarkQueryResult;
import at.sti2.model.benchmark_result.BenchmarkTestCaseResult;
import at.sti2.model.query.Query;
import at.sti2.model.query.QueryContainer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BenchmarkTest {

    public static BenchmarkEngineResult execute(
        BenchmarkEngine engine, List<TestCaseConfiguration> testCases) {
        BenchmarkEngineResult benchmarkEngineResult =
            new BenchmarkEngineResult(engine.getEngineName());

        log.info("Starting evaluation using benchmarking engine \"{}\" ...",
                 engine.getEngineName());

        for (TestCaseConfiguration testCase : testCases) {
            BenchmarkTestCaseResult benchmarkTestCaseResult =
                new BenchmarkTestCaseResult(testCase);
            benchmarkEngineResult.addTestCaseResult(benchmarkTestCaseResult);

            log.info("... running test case {}", testCase.getName());
            engine.prepare(testCase);
            Map<String, BenchmarkQueryResult> result =
                executeTestCase(engine, testCase);
            benchmarkTestCaseResult.getQueryResults().putAll(result);
            engine.cleanUp();
        }
        engine.shutDown();
        return benchmarkEngineResult;
    }

    private static Map<String, BenchmarkQueryResult> executeTestCase(
        BenchmarkEngine engine, TestCaseConfiguration testCase) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Map<String, BenchmarkQueryResult> testCaseResults = new HashMap<>();
        String queryFileClassPath =
            BenchmarkUtils.getFilePath(engine.getEngineName(), testCase,
                                       "_queries.json", true);
        QueryContainer queryContainer =
            BenchmarkUtils.load(queryFileClassPath, QueryContainer.class);
        if (queryContainer != null) {
            executeTestCaseQueries(engine, executor, testCaseResults,
                                   queryContainer);
        }
        executor.shutdownNow();
        return testCaseResults;
    }

    private static void executeTestCaseQueries(BenchmarkEngine engine,
                                               ExecutorService executor,
                                               Map<String, BenchmarkQueryResult> testCaseResults,
                                               QueryContainer queryContainer) {
        for (Query query : queryContainer.getQueries()) {
            log.info("Evaluating query: {}", query.getQuery());
            BenchmarkQueryResult queryResultObject =
                new BenchmarkQueryResult(query.getName());
            Future<Integer> resultFuture = null;
            long start = System.currentTimeMillis();
            try {
                resultFuture =
                    executor.submit(
                        new QueryExecutionTask(engine, query.getQuery()));
                Integer numberOfResults =
                    resultFuture.get(30, TimeUnit.MINUTES);
                queryResultObject.setNumOfResults(numberOfResults);
            } catch (TimeoutException e) {
                resultFuture.cancel(true);
                queryResultObject.setException(e);
                log.info("Query evaluation timed out!");
            } catch (MemoryLimitExceededException e) {
                queryResultObject.setException(e);
                log.error("Query evaluation reached memory limit!", e);
            } catch (Exception e) {
                queryResultObject.setException(e);
                log.error("Error evaluating query {} with SemReasoner!",
                          query.getName(), e);
            } finally {
                long end = System.currentTimeMillis();
                queryResultObject.setTimeSpent((end - start));
            }
            testCaseResults.put(
                query.getName(),
                queryResultObject);
        }
    }
}
