package at.sti2.engines;

import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.model.benchmark_result.BenchmarkQueryResult;
import at.sti2.model.query.Query;
import at.sti2.model.query.QueryContainer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
public class Jena implements BenchmarkEngine {

  private static final String ENGINE_NAME = "Jena";

  private static final String NAMESPACE = "";

  private Model model = ModelFactory.createDefaultModel();
  private Reasoner reasoner;

  private InfModel infModel;

  @Override
  public String getEngineName() {
    return ENGINE_NAME;
  }

  @Override
  public void setSettings(Map<String, Object> settings) {}

  @Override
  public void prepare(TestCaseConfiguration testCase) {
    try {
      String absoluteDataPath = BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".jena", false);
      log.info("Loading data from path: {}", absoluteDataPath);
      FileReader dataInput = new FileReader(absoluteDataPath);
      BufferedReader bufRead = new BufferedReader(dataInput);
      String first, second, line = bufRead.readLine();
      while (line != null) {
        first = bufRead.readLine();
        second = bufRead.readLine();
        Property p = model.createProperty(NAMESPACE, line);
        model.createResource(first).addProperty(p, model.createResource(second));
        line = bufRead.readLine();
      }
      bufRead.close();

      String absoluteRulePath = BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".rules", false);
      log.info("Loading rules from path: {}", absoluteRulePath);
      List<Rule> rules = Rule.rulesFromURL("file:" + absoluteRulePath);
      reasoner = new GenericRuleReasoner(rules);
      infModel = ModelFactory.createInfModel(reasoner, model);
    } catch (Exception e) {
      log.error("Error preparing data for jena!", e);
    }
  }

  @Override
  public Map<String, BenchmarkQueryResult> executeQueries(TestCaseConfiguration testCase) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Map<String, BenchmarkQueryResult> testCaseResults = new HashMap<>();
    String queryFileClassPath =
        BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, "_queries.json", true);
    QueryContainer queryContainer = BenchmarkUtils.load(queryFileClassPath, QueryContainer.class);
    for (Query query : queryContainer.getQueries()) {
      JenaQuery jenaQuery = parseQuery(query.getQuery());
      log.info("Evaluating query: {}", jenaQuery);
      long start = System.currentTimeMillis();
      Future<Integer> numberOfResultsFuture = executor.submit(new JenaQueryTask(jenaQuery));
      try {
        int numberOfResults = numberOfResultsFuture.get(30, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();
        testCaseResults.put(
            query.getName(),
            new BenchmarkQueryResult(query.getQuery(), (end - start), numberOfResults));
      } catch (TimeoutException e) {
        numberOfResultsFuture.cancel(true);
        log.error("TIMEOUT!");
      } catch (Exception e) {
        log.error("Error evaluating query {} with Jena!", query.getName(), e);
      }
    }
    executor.shutdownNow();
    return testCaseResults;
  }

  @Override
  public int executeQuery(String query) {
    return 0;
  }

  @Override
  public void cleanUp() {
    model = ModelFactory.createDefaultModel();
    reasoner = null;
    infModel = null;
  }

  @Override
  public void shutDown() {}

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
    if (splittedQueryString.length == 3 && StringUtils.isNotEmpty(splittedQueryString[2])) {
      object = model.createResource(splittedQueryString[2]);
    }

    return new JenaQuery(subject, property, object);
  }

  @AllArgsConstructor
  @Data
  static class JenaQuery {
    private Resource subject;
    private Property property;
    private Resource object;
  }

  class JenaQueryTask implements Callable<Integer> {

    private final JenaQuery jenaQuery;

    public JenaQueryTask(JenaQuery jenaQuery) {
      this.jenaQuery = jenaQuery;
    }

    @Override
    public Integer call() throws Exception {
      Iterator<Statement> statementIterator =
          infModel.listStatements(
              jenaQuery.getSubject(), jenaQuery.getProperty(), jenaQuery.getObject());
      return countQueryResults(statementIterator);
    }

    private int countQueryResults(Iterator<Statement> statementIterator) {
      int count = 0;
      while (statementIterator.hasNext() && !Thread.interrupted()) {
        count++;
        statementIterator.next();
      }
      return count;
    }
  }
}
