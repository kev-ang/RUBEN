package at.sti2;

import at.sti2.benchmark.BenchmarkExecutor;
import at.sti2.utils.BenchmarkUtils;
import at.sti2.configuration.BenchmarkConfiguration;
import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.engines.RuleEngine;
import at.sti2.model.benchmark_result.BenchmarkEngineResult;
import at.sti2.model.benchmark_result.BenchmarkResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Ruben {

    public void runEvaluation(String pathToConfiguration) {
        BenchmarkResult benchmarkResult = new BenchmarkResult();
        BenchmarkConfiguration benchmarkConfiguration =
            BenchmarkUtils.load(pathToConfiguration,
                                BenchmarkConfiguration.class);

        for (ReasoningEngineConfiguration benchmarkEngineConfig : benchmarkConfiguration.getEngines()) {
            RuleEngine ruleEngine =
                BenchmarkUtils.loadBenchmarkEngine(benchmarkEngineConfig);

            BenchmarkEngineResult benchmarkEngineResult =
                BenchmarkExecutor.execute(benchmarkConfiguration.getTestDataPath(),
                                          ruleEngine,
                                          benchmarkConfiguration.getTestCases());
            benchmarkResult.addBenchmarkEngineResult(benchmarkEngineResult);
        }
        BenchmarkUtils.writeResults(benchmarkResult);
    }

    public static void main(String[] args) {
        Ruben benchmark = new Ruben();
        benchmark.runEvaluation(args[0]);
        log.info("DONE!");
    }
}