package at.sti2.benchmark;

import at.sti2.configuration.ReasoningEngineConfiguration;
import at.sti2.engines.BenchmarkEngine;
import at.sti2.engines.SemReasoner;
import at.sti2.model.benchmark_result.BenchmarkResult;
import at.sti2.configuration.TestCaseConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class BenchmarkUtils {

  public static final String REASONING_ENGINE_ROOT = "/reasoning_engines";
  private static final String TEST_CASE_FILE_ENDING = ".json";

  private static final ObjectMapper om = new ObjectMapper();

  public static <T> T load(String classpath, Class<T> parseToClass) {
    try {
      InputStream
          fileInputStream =  BenchmarkUtils.class.getResourceAsStream(classpath);
      if (fileInputStream != null) {
        String fileContent =
            IOUtils.toString(
                fileInputStream, Charset.defaultCharset());
        return om.readValue(fileContent, parseToClass);
      }
      return null;
    } catch (Exception e) {
      log.error("Error loading file content!", e);
      throw new IllegalStateException("Can not load and parse file content!");
    }
  }

  public static List<BenchmarkEngine> loadBenchmarkEngines(
      List<ReasoningEngineConfiguration> engineConfigurations) {
    try {
      List<BenchmarkEngine> benchmarkEngines = new ArrayList<>();
      for (ReasoningEngineConfiguration engineConfiguration : engineConfigurations) {
        BenchmarkEngine benchmarkEngine =
            (BenchmarkEngine)
                Class.forName(engineConfiguration.getClasspath())
                    .getDeclaredConstructor()
                    .newInstance();
        benchmarkEngine.setSettings(engineConfiguration.getSettings());
        benchmarkEngines.add(benchmarkEngine);
      }
      return benchmarkEngines;
    } catch (Exception e) {
      log.error("Error loading benchmark engine classes!", e);
      throw new IllegalStateException("Can not load benchmarking engines!");
    }
  }

  public static List<TestCaseConfiguration> loadTestCases(String testCaseFolderPath) {
    try {
      List<File> testCaseFiles = getTestCaseFiles(testCaseFolderPath, TEST_CASE_FILE_ENDING);

      List<TestCaseConfiguration> testCases = new ArrayList<>();
      if (testCaseFiles != null) {
        for (File testCaseFile : testCaseFiles) {
          String testCaseFileContent =
              IOUtils.toString(new FileInputStream(testCaseFile), Charset.defaultCharset());
          testCases.add(om.readValue(testCaseFileContent, TestCaseConfiguration.class));
        }
      }
      return testCases;
    } catch (Exception e) {
      log.error("Error while loading test cases!", e);
      throw new IllegalStateException("Can not load test cases!");
    }
  }

  private static List<File> getTestCaseFiles(String folderPath, String fileEnding)
      throws IOException {

    String path = BenchmarkUtils.class.getResource(folderPath).getPath();
    File folder = new File(path);
    if (folder.isDirectory()) {
      return Files.walk(folder.toPath())
          .filter(p -> new File(p.toString()).isFile())
          .map(p -> new File(p.toString()))
          .filter(f -> f.getName().endsWith(fileEnding))
          .collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException("No test case folder provided!");
    }
  }

  public static void writeResults(BenchmarkResult benchmarkResult) {
    try {
      log.info(new ObjectMapper().writeValueAsString(benchmarkResult));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    System.out.println("DONE!");
  }

  public static String getFilePath(
      String engineName, TestCaseConfiguration testCase, String fileEnding, boolean isClassPath) {
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
}
