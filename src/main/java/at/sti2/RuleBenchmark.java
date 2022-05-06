package at.sti2;

import at.sti2.benchmark.BenchmarkTest;
import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.BenchmarkConfiguration;
import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.engines.BenchmarkEngine;
import at.sti2.model.benchmark_result.BenchmarkEngineResult;
import at.sti2.model.benchmark_result.BenchmarkResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleBenchmark {

    public void runEvaluation(String pathToConfiguration) {
        BenchmarkResult benchmarkResult = new BenchmarkResult();
        BenchmarkConfiguration benchmarkConfiguration =
            BenchmarkUtils.load(pathToConfiguration,
                                BenchmarkConfiguration.class);

        for (ReasoningEngineConfiguration benchmarkEngineConfig : benchmarkConfiguration.getBenchmarkEngines()) {
            BenchmarkEngine benchmarkEngine =
                BenchmarkUtils.loadBenchmarkEngine(benchmarkEngineConfig);

            BenchmarkEngineResult benchmarkEngineResult =
                BenchmarkTest.execute(benchmarkConfiguration.getTestDataPath(),
                                      benchmarkEngine,
                                      benchmarkConfiguration.getTestCases());
            benchmarkResult.addBenchmarkEngineResult(benchmarkEngineResult);
        }
        BenchmarkUtils.writeResults(benchmarkResult);
        log.info("DONE!");
    }

    public static void main(String[] args) {
        RuleBenchmark benchmark = new RuleBenchmark();
        benchmark.runEvaluation(args[0]);
    }
}
