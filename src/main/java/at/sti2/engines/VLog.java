package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.utils.BenchmarkUtils;
import java.io.FileInputStream;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.semanticweb.rulewerk.core.model.api.PositiveLiteral;
import org.semanticweb.rulewerk.core.reasoner.KnowledgeBase;
import org.semanticweb.rulewerk.core.reasoner.QueryResultIterator;
import org.semanticweb.rulewerk.core.reasoner.Reasoner;
import org.semanticweb.rulewerk.parser.RuleParser;
import org.semanticweb.rulewerk.reasoner.vlog.VLogReasoner;

@Slf4j
public class VLog implements RuleEngine {

    private String engineName;

    private KnowledgeBase knowledgeBase;

    private Reasoner reasoner;

    @Override
    public String getEngineName() {
        return engineName;
    }

    @Override
    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    @Override
    public void setSettings(Map<String, Object> settings) {

    }

    @Override
    public void prepare(String testDataPath, TestCaseConfiguration testCase) {
        String absoluteRulePath =
            BenchmarkUtils.getFilePath(testDataPath, engineName,
                                       testCase, ".rls");
        String absoluteDataPath =
            BenchmarkUtils.getFilePath(testDataPath, engineName,
                                       testCase, ".fct");

        if (BenchmarkUtils.fileExists(absoluteRulePath) &&
            BenchmarkUtils.fileExists(absoluteDataPath)) {
            try {
                knowledgeBase = RuleParser.parse(
                    new FileInputStream(absoluteRulePath));
                RuleParser.parseInto(knowledgeBase,
                                     new FileInputStream(absoluteDataPath));
                reasoner = new VLogReasoner(knowledgeBase);

                log.info("Start materialization ...");
                long start = System.currentTimeMillis();
                reasoner.reason();
                log.info("Materialization finished in {} ms!",
                         (System.currentTimeMillis() - start));
            } catch (Exception e) {
                log.error("Error while preparing data and rules for VLog!", e);
            }
        }
    }

    @Override
    public int executeQuery(String query) throws Exception {
        int count = 0;
        PositiveLiteral queryLit =
            RuleParser.parsePositiveLiteral(query);
        try (final QueryResultIterator answers = reasoner.answerQuery(
            queryLit,
            true)) {
            while (answers.hasNext()) {
                count++;
                answers.next();
            }
        }
        return count;
    }

    @Override
    public void cleanUp() {
        reasoner.close();
    }

    @Override
    public void shutDown() {
        reasoner.close();
        knowledgeBase = null;
    }
}