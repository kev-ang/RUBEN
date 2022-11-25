package at.sti2.utils.result_writer;

import at.sti2.model.benchmark_result.BenchmarkResult;
import at.sti2.model.benchmark_result.QueryResult;
import at.sti2.model.benchmark_result.RuleEngineResult;
import at.sti2.model.benchmark_result.TestCaseResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes dedicated CSV files for each test case.
 *
 * @author kevin.angele@sti2.at
 */
@Slf4j
public class CSVWriter implements ResultWriter {

    @Override
    public void writeResult(BenchmarkResult benchmarkResult) {
        for (Entry<String, RuleEngineResult> engine : benchmarkResult.getBenchmarkEngineResults()
                                                                     .entrySet()) {
            for (Entry<String, TestCaseResult> testCase : engine.getValue()
                                                                .getBenchmarkTestCaseResults()
                                                                .entrySet()) {
                File resultFile = new File(
                    engine.getKey() + "_" + testCase.getKey() + ".csv");
                try (BufferedWriter bw = new BufferedWriter(
                    new FileWriter(resultFile))) {
                    bw.write("Query;NrResults;Time (in ms);Exception");
                    bw.newLine();
                    for (QueryResult currentQuery : testCase.getValue()
                                                            .getQueryResults()
                                                            .values()) {
                        bw.write(currentQuery.getQuery() + ";" +
                                 currentQuery.getNumOfResults() + ";" +
                                 currentQuery.getTimeSpent() + ";" +
                                 currentQuery.getException());
                        bw.newLine();
                    }
                    bw.flush();
                } catch (IOException e) {
                    log.error(
                        "Error writing result file for engine {} and test case {}",
                        engine.getKey(), testCase.getKey());
                }
            }
        }
    }
}