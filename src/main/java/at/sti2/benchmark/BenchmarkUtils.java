package at.sti2.benchmark;

import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.BenchmarkEngine;
import at.sti2.engines.SemReasoner;
import at.sti2.engines.Stardog;
import at.sti2.model.benchmark_result.BenchmarkResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class BenchmarkUtils {

    public static final String REASONING_ENGINE_ROOT = "/reasoning_engines";

    private static final ObjectMapper om = new ObjectMapper();

    public static <T> T load(String classpath, Class<T> parseToClass) {
        try {
            InputStream
                fileInputStream =
                BenchmarkUtils.class.getResourceAsStream(classpath);
            if (fileInputStream != null) {
                String fileContent =
                    IOUtils.toString(
                        fileInputStream, Charset.defaultCharset());
                return om.readValue(fileContent, parseToClass);
            }
            return null;
        } catch (Exception e) {
            log.error("Error loading file content!", e);
            throw new IllegalStateException(
                "Can not load and parse file content!");
        }
    }

    public static BenchmarkEngine loadBenchmarkEngine(
        ReasoningEngineConfiguration reasoningEngineConfiguration) {
        try {
            BenchmarkEngine benchmarkEngine =
                (BenchmarkEngine)
                    Class.forName(reasoningEngineConfiguration.getClasspath())
                         .getDeclaredConstructor()
                         .newInstance();
            benchmarkEngine.setSettings(
                reasoningEngineConfiguration.getSettings());
            return benchmarkEngine;
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

    public static String getFilePath(
        String engineName, TestCaseConfiguration testCase, String fileEnding,
        boolean isClassPath) {
        var testFilePath =
            String.join(
                "/",
                BenchmarkUtils.REASONING_ENGINE_ROOT,
                engineName,
                testCase.getCategory(),
                testCase.getTestName(),
                testCase.getTestCaseIdentifier() + fileEnding);
        if (!isClassPath) {
            URL fileURL = SemReasoner.class.getResource(testFilePath);
            if (fileURL != null) {
                return fileURL.getPath();
            }
            return null;
        }
        return testFilePath;
    }

    public static void startContainerForEngine(String engineName)
        throws IOException {
        String dockerComposeCommand = "docker-compose -f " +
                                      BenchmarkUtils.class.getResource(
                                          "/docker-compose.yml").getPath() +
                                      " up -d " + engineName.toLowerCase();
        log.info("Executing: {}", dockerComposeCommand);
        Process p = Runtime.getRuntime().exec(dockerComposeCommand);
        printProcessInputStream(p.getInputStream());
    }

    public static void stopContainerForEngine(String engineName)
        throws IOException {
        String dockerComposeCommand = "docker-compose -f " +
                                      BenchmarkUtils.class.getResource(
                                          "/docker-compose.yml").getPath() +
                                      " down -v " + engineName.toLowerCase();
        log.info("Executing: {}", dockerComposeCommand);
        Process p = Runtime.getRuntime().exec(dockerComposeCommand);
        printProcessInputStream(p.getInputStream());
    }

    private static void printProcessInputStream(InputStream inputStream)
        throws IOException {
        String line;
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader rdr = new BufferedReader(isr);
        while ((line = rdr.readLine()) != null) {
            log.info(line);
        }
    }
}
