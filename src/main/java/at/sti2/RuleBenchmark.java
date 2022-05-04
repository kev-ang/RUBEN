package at.sti2;

import at.sti2.benchmark.BenchmarkTest;
import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.BenchmarkConfiguration;
import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.engines.BenchmarkEngine;
import at.sti2.model.benchmark_result.BenchmarkEngineResult;
import at.sti2.model.benchmark_result.BenchmarkResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuleBenchmark {

  private static final String BENCHMARK_ENGINE_CONFIGURATION_FILE = "/Benchmark_Configuration.json";

  public void runEvaluation() {
    BenchmarkResult benchmarkResult = new BenchmarkResult();
    BenchmarkConfiguration benchmarkConfiguration =
        BenchmarkUtils.load(BENCHMARK_ENGINE_CONFIGURATION_FILE, BenchmarkConfiguration.class);

    for (ReasoningEngineConfiguration benchmarkEngineConfig : benchmarkConfiguration.getBenchmarkEngines()) {
      BenchmarkEngine benchmarkEngine = BenchmarkUtils.loadBenchmarkEngine(benchmarkEngineConfig);

      BenchmarkEngineResult benchmarkEngineResult =
          BenchmarkTest.execute(benchmarkEngine, benchmarkConfiguration.getTestCases());
      benchmarkResult.addBenchmarkEngineResult(benchmarkEngineResult);
    }
    BenchmarkUtils.writeResults(benchmarkResult);
    log.info("DONE!");
  }

  public static void main(String[] args) {
    RuleBenchmark benchmark = new RuleBenchmark();
    benchmark.runEvaluation();
  }
}
