package at.sti2.utils.result_writer;

import at.sti2.model.benchmark_result.BenchmarkResult;

/**
 * Interface that needs to be implemented by a result writer.
 *
 * @author kevin.angele@sti2.at
 */
public interface ResultWriter {

    /**
     * Write result into format specified by the result writer.
     *
     * @param benchmarkResult evaluation results
     */
    void writeResult(BenchmarkResult benchmarkResult);

}