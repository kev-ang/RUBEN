package at.sti2.benchmark;

import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.BenchmarkEngine;
import at.sti2.model.benchmark_result.BenchmarkResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static BenchmarkEngine loadBenchmarkEngine(
        ReasoningEngineConfiguration reasoningEngineConfiguration) {
        try {
            BenchmarkEngine benchmarkEngine =
                (BenchmarkEngine)
                    Class.forName(reasoningEngineConfiguration.getClasspath())
                         .getDeclaredConstructor()
                         .newInstance();
            benchmarkEngine.setEngineName(
                reasoningEngineConfiguration.getName());
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

    public static void startContainerForEngine(String engineName)
        throws IOException {
        String dockerComposeFilePath = new File(".").getCanonicalPath();
        String dockerComposeCommand = "docker-compose -f " +
                                      dockerComposeFilePath +
                                      "/docker-compose.yml" +
                                      " up -d " +
                                      engineName.toLowerCase();
        log.info("Executing: {}", dockerComposeCommand);
        Process p = Runtime.getRuntime().exec(dockerComposeCommand);
        printProcessInputStream(p.getInputStream());
    }

    public static void stopContainers()
        throws IOException {
        String dockerComposeFilePath = new File(".").getCanonicalPath();
        String dockerComposeCommand = "docker-compose -f " +
                                      dockerComposeFilePath +
                                      "/docker-compose.yml" +
                                      " down -v";
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