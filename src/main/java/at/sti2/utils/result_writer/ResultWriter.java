package at.sti2.utils.result_writer;

import at.sti2.model.benchmark_result.BenchmarkResult;

public interface ResultWriter {

    void writeResult(BenchmarkResult benchmarkResult);

}