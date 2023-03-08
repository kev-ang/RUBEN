package at.sti2.utils;

import at.sti2.configuration.RuleEngineConfiguration;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.engines.RuleEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

/**
 * Utils required for loading files.
 */
@Slf4j
public class BenchmarkUtils {

    private static final ObjectMapper om = new ObjectMapper();

    /**
     * Load a file and parse to a given class.
     *
     * @param path         of the file to be loaded.
     * @param parseToClass class the file should be parsed to
     * @param <T>          type of the return value
     * @return parsed data
     */
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

    /**
     * Initialize class for rule engine using reflection.
     *
     * @param ruleEngineConfiguration configuration of the rule engine to be
     *                                loaded.
     * @return instance of a rule engine.
     */
    public static RuleEngine loadBenchmarkEngine(
        RuleEngineConfiguration ruleEngineConfiguration) {
        try {
            RuleEngine ruleEngine =
                (RuleEngine)
                    Class.forName(ruleEngineConfiguration.getClasspath())
                         .getDeclaredConstructor()
                         .newInstance();
            ruleEngine.setEngineName(
                ruleEngineConfiguration.getName());
            ruleEngine.setSettings(
                ruleEngineConfiguration.getSettings());
            return ruleEngine;
        } catch (Exception e) {
            log.error("Error loading benchmark engine class!", e);
            throw new IllegalStateException(
                "Can not load benchmarking engine!");
        }
    }

    /**
     * Get path of a file based on the provided information.
     *
     * @param testDataPath root directory containing the test data
     * @param engineName   name of the engine the file should be fetched for
     * @param testCase     current test case
     * @param fileEnding   required file ending
     * @return file path
     */
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

    /**
     * Check if a file for the given file path exists.
     *
     * @param filePath to be checked
     * @return true if file exists, false otherwise
     */
    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }

    public static String getEngineDataFolder(RuleEngine engine) {
        if (engine.getSettings() != null &&
            engine.getSettings().containsKey("dataFolderName")) {
            return (String) engine.getSettings().get("dataFolderName");
        }
        return engine.getEngineName();
    }
}