package at.sti2.utils;

import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.RuleEngine;
import at.sti2.model.benchmark_result.BenchmarkResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class BenchmarkUtils {

    private static final ObjectMapper om = new ObjectMapper();

    public static <T> T load(String path, Class<T> parseToClass) {
        if (fileExists(path)) {
            try {
                InputStream
                    fileInputStream = new FileInputStream(path);
                String fileContent =
                    IOUtils.toString(
                        fileInputStream, Charset.defaultCharset());
                return om.readValue(fileContent, parseToClass);
            } catch (Exception e) {
                log.error("Error loading file content!", e);
                throw new IllegalStateException(
                    "Can not load and parse file content!");
            }
        }
        return null;
    }

    public static RuleEngine loadBenchmarkEngine(
        ReasoningEngineConfiguration reasoningEngineConfiguration) {
        try {
            RuleEngine ruleEngine =
                (RuleEngine)
                    Class.forName(reasoningEngineConfiguration.getClasspath())
                         .getDeclaredConstructor()
                         .newInstance();
            ruleEngine.setEngineName(
                reasoningEngineConfiguration.getName());
            ruleEngine.setSettings(
                reasoningEngineConfiguration.getSettings());
            return ruleEngine;
        } catch (Exception e) {
            log.error("Error loading benchmark engine class!", e);
            throw new IllegalStateException(
                "Can not load benchmarking engine!");
        }
    }

    public static void writeResults(BenchmarkResult benchmarkResult) {
        try {
            om.writerWithDefaultPrettyPrinter()
              .writeValue(new File("Results.json"), benchmarkResult);
        } catch (IOException e) {
            log.error("Error while writing benchmark results to file!", e);
        }
    }

    public static String getFilePath(String testDataPath,
                                     String engineName,
                                     TestCaseConfiguration testCase,
                                     String fileEnding) {
        return
            String.join(
                "/",
                testDataPath,
                engineName,
                testCase.getCategory(),
                testCase.getTestName(),
                testCase.getTestCaseIdentifier() + fileEnding);
    }

    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }
}