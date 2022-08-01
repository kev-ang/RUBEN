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