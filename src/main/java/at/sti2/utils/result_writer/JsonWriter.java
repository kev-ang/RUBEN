package at.sti2.utils.result_writer;

import at.sti2.model.benchmark_result.BenchmarkResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON result writer. The output is a single JSON file containing all results.
 *
 * @author kevin.angele@sti2.at
 */
@Slf4j
public class JsonWriter implements ResultWriter {

    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public void writeResult(BenchmarkResult benchmarkResult) {
        try {
            om.writerWithDefaultPrettyPrinter()
              .writeValue(new File("Results.json"), benchmarkResult);
        } catch (IOException e) {
            log.error("Error while writing benchmark results to file!", e);
        }
    }
}