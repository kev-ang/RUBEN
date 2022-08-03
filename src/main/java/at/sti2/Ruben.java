package at.sti2;

import at.sti2.benchmark.BenchmarkExecutor;
import at.sti2.configuration.BenchmarkConfiguration;
import at.sti2.configuration.RuleEngineConfiguration;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.RuleEngine;
import at.sti2.model.benchmark_result.BenchmarkResult;
import at.sti2.model.benchmark_result.RuleEngineResult;
import at.sti2.utils.BenchmarkUtils;
import at.sti2.utils.result_writer.CSVWriter;
import at.sti2.utils.result_writer.ResultWriter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Main class loading the configuration and executes the test cases for each of
 * the provided rule engines. Finally, the results are written to a result
 * file.
 *
 * @author kevin.angele@sti2.at
 */
@Slf4j
public class Ruben {

    /**
     * Executes the benchmark for the provided rule engines and test cases.
     *
     * @param pathToConfiguration path to the configuration containing the rule
     *                            engines and test cases
     */
    public void runEvaluation(String pathToConfiguration,
                              ResultWriter resultWriter) {
        BenchmarkResult benchmarkResult = new BenchmarkResult();

        BenchmarkConfiguration benchmarkConfiguration =
            BenchmarkUtils.load(pathToConfiguration,
                                BenchmarkConfiguration.class);

        for (RuleEngineConfiguration ruleEngineConfiguration : benchmarkConfiguration.getEngines()) {
            runEvaluationForEngine(benchmarkResult,
                                   ruleEngineConfiguration,
                                   benchmarkConfiguration.getTestDataPath(),
                                   benchmarkConfiguration.getTestCases());
        }

        resultWriter.writeResult(benchmarkResult);
    }

    private void runEvaluationForEngine(BenchmarkResult benchmarkResult,
                                        RuleEngineConfiguration benchmarkEngineConfig,
                                        String testDataPath,
                                        List<TestCaseConfiguration> testCases) {
        RuleEngine ruleEngine =
            BenchmarkUtils.loadBenchmarkEngine(benchmarkEngineConfig);

        RuleEngineResult ruleEngineResult =
            BenchmarkExecutor.execute(
                testDataPath,
                ruleEngine,
                testCases);
        benchmarkResult.addBenchmarkEngineResult(ruleEngineResult);
    }

    public static void main(String[] args) {
        Ruben benchmark = new Ruben();
        long start = System.currentTimeMillis();
        benchmark.runEvaluation(args[0], new CSVWriter());
        long end = System.currentTimeMillis();
        log.info("Benchmark finished in {} minutes!",
                 ((end - start) / (60 * 1000)));
    }
}