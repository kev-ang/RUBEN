package at.sti2.engines;

import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.TestCaseConfiguration;
import com.semedy.reasoner.api.core.Configuration;
import com.semedy.reasoner.api.core.Configuration.StorageType;
import com.semedy.reasoner.api.core.Core;
import com.semedy.reasoner.api.core.DeductiveDatabase;
import com.semedy.reasoner.api.core.ResultBuffer;
import com.semedy.reasoner.api.core.ResultEnumerator;
import com.semedy.reasoner.api.reasoning.InterruptFlag;
import com.semedy.reasoner.api.reasoning.SemReasonerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SemReasoner implements BenchmarkEngine {

    private static final String ENGINE_NAME = "SemReasoner";

    private Map<String, Object> settings;

    private Core core;
    private DeductiveDatabase ddb;

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    @Override
    public void prepare(TestCaseConfiguration testCase) {
        String absoluteDataFilePath =
            BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".fct", false);
        if (StringUtils.isNotEmpty(absoluteDataFilePath)) {
            try {
                String absoluteRuleFilePath =
                    BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".rls",
                                               false);
                Configuration configuration = new Configuration();
                configuration.setEDBStorageType(getStorageType());
                configuration.setTopDownReasoning(isTopDownReasoningEnabled());
                configuration.setEDBDir(getEDBPath());
                configuration.setTempDir(getTempDirPath());
                core = new Core(configuration);
                ddb = new DeductiveDatabase(core);

                log.info(
                    "Loading data from path: {}\n Loading rules from path: {}",
                    absoluteDataFilePath,
                    absoluteRuleFilePath);
                ddb.load(absoluteDataFilePath);
                if (absoluteRuleFilePath != null) {
                    ddb.load(absoluteRuleFilePath);
                }
                log.info("Loading data and rules was successfully!");
            } catch (Exception e) {
                log.error("Error while preparing SemReasoner!", e);
            }
        }
    }

    @Override
    public int executeQuery(String query) throws Exception {
        ResultEnumerator resultEnumerator =
            ddb.query(new InterruptFlag(), query, null);
        Map<String, List<Object>> queryResults =
            collectQueryResults(resultEnumerator);
        return countQueryResults(queryResults);
    }

    @Override
    public void cleanUp() {
        try {
            log.info("Clean up SemReasoner!");
            Core core = new Core(new Configuration());
            ddb = new DeductiveDatabase(core);
        } catch (Exception e) {
            log.error("Error while cleaning up SemReasoner!", e);
        }
    }

    @Override
    public void shutDown() {
        try {
            core.shutdown();
            while (!core.isShutDownFinished()) {
                Thread.sleep(500);
            }
            FileUtils.deleteDirectory(new File("SemReasonerData/"));
        } catch (SemReasonerException | InterruptedException | IOException e) {
            log.error("Error shutting down SemReasoner!", e);
        }
    }

    private Map<String, List<Object>> collectQueryResults(
        ResultEnumerator enumerator)
        throws IOException, SemReasonerException {
        Map<String, List<Object>> queryResults = new HashMap<>();
        enumerator.nextElement();
        ResultBuffer buffer = enumerator.getBuffer();
        List<String> queryVariables = getQueryVariables(buffer);
        while (enumerator.hasMoreElements()) {
            enumerator.nextElement();
            queryVariables.forEach(
                v -> {
                    try {
                        queryResults.computeIfAbsent(v, o -> new ArrayList<>())
                                    .add(buffer.get(v));
                    } catch (Exception e) {
                        throw new IllegalStateException(
                            "Error retrieving variable value from buffer!");
                    }
                });
        }
        return queryResults;
    }

    private int countQueryResults(Map<String, List<Object>> results) {
        if (!results.isEmpty()) {
            return results.values().stream().findFirst().get().size();
        }
        return 0;
    }

    private List<String> getQueryVariables(ResultBuffer buffer)
        throws IOException, SemReasonerException {
        List<String> variables = new ArrayList<>();
        for (var i = 0; i < buffer.length(); i++) {
            variables.add((String) buffer.get(i));
        }
        return variables;
    }

    private StorageType getStorageType() {
        if (!settings.isEmpty() && settings.containsKey("storage_mode")) {
            switch ((String) settings.get("storage_mode")) {
                case "MEMORY":
                    return StorageType.RAM;
                case "MIXED":
                    return StorageType.MIXED;
                default:
                    return StorageType.PERSISTENT;
            }
        }
        return StorageType.PERSISTENT;
    }

    private String getEDBPath() {

        String edbFolder = settings.get("edb_folder").toString();
        File edbFolderFile = new File(edbFolder);
        try {
            if (edbFolderFile.exists()) {
                FileUtils.deleteDirectory(edbFolderFile);
            }
        } catch (Exception e) {
            log.error("Error deleting edb folder!", e);
        }
        edbFolderFile.mkdirs();
        return edbFolder;
    }

    private String getTempDirPath() {

        String edbFolder = settings.get("temp_folder").toString();
        File edbFolderFile = new File(edbFolder);
        try {
            if (edbFolderFile.exists()) {
                FileUtils.deleteDirectory(edbFolderFile);
            }
        } catch (Exception e) {
            log.error("Error deleting temp folder!", e);
        }
        edbFolderFile.mkdirs();
        return edbFolder;
    }

    private boolean isTopDownReasoningEnabled() {
        if (!settings.isEmpty() && settings.containsKey("top_down_reasoning")) {
            return (boolean) settings.get("top_down_reasoning");
        }
        return false;
    }
}
