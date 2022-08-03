package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.utils.BenchmarkUtils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.codehaus.plexus.util.StringUtils;

@Slf4j
public class Jena implements RuleEngine {

    private String engineName;

    private static final String NAMESPACE = "";

    private Model model = ModelFactory.createDefaultModel();
    private Reasoner reasoner;

    private InfModel infModel;

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
        String absoluteDataPath =
            BenchmarkUtils.getFilePath(testDataPath, engineName, testCase,
                                       ".jena");
        if (BenchmarkUtils.fileExists(absoluteDataPath)) {
            try {
                log.info("Loading data from path: {}", absoluteDataPath);
                FileReader dataInput = new FileReader(absoluteDataPath);
                BufferedReader bufRead = new BufferedReader(dataInput);
                String first, second, line = bufRead.readLine();
                while (line != null) {
                    first = bufRead.readLine();
                    second = bufRead.readLine();
                    Property p = model.createProperty(NAMESPACE, line);
                    model.createResource(first)
                         .addProperty(p, model.createResource(second));
                    line = bufRead.readLine();
                }
                bufRead.close();

                String absoluteRulePath =
                    BenchmarkUtils.getFilePath(testDataPath, engineName,
                                               testCase, ".rules");
                log.info("Loading rules from path: {}", absoluteRulePath);
                List<Rule> rules =
                    Rule.rulesFromURL("file:" + absoluteRulePath);
                reasoner = new GenericRuleReasoner(rules);
                infModel = ModelFactory.createInfModel(reasoner, model);
            } catch (Exception e) {
                log.error("Error preparing data for jena!", e);
            }
        }
    }

    @Override
    public int executeQuery(String query) {
        JenaQuery jenaQuery = parseQuery(query);
        Iterator<Statement> statementIterator =
            infModel.listStatements(
                jenaQuery.getSubject(), jenaQuery.getProperty(),
                jenaQuery.getObject());
        return countQueryResults(statementIterator);
    }

    @Override
    public void cleanUp() {
        model = ModelFactory.createDefaultModel();
        reasoner = null;
        infModel = null;
    }

    @Override
    public void shutDown() {
    }

    private JenaQuery parseQuery(String query) {
        Resource subject = null;
        Property property = null;
        Resource object = null;
        String[] splittedQueryString = query.split(",");

        // Subject
        if (StringUtils.isNotEmpty(splittedQueryString[0])) {
            subject = model.createResource(splittedQueryString[0]);
        }

        // Property
        if (StringUtils.isNotEmpty(splittedQueryString[1])) {
            property = model.createProperty(NAMESPACE, splittedQueryString[1]);
        }

        // Object
        if (splittedQueryString.length == 3 &&
            StringUtils.isNotEmpty(splittedQueryString[2])) {
            object = model.createResource(splittedQueryString[2]);
        }

        return new JenaQuery(subject, property, object);
    }

    private int countQueryResults(Iterator<Statement> statementIterator) {
        int count = 0;
        while (statementIterator.hasNext() && !Thread.interrupted()) {
            count++;
            statementIterator.next();
        }
        return count;
    }

    @AllArgsConstructor
    @Data
    static class JenaQuery {

        private Resource subject;
        private Property property;
        private Resource object;
    }
}