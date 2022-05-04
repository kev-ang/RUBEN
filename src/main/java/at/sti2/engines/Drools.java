package at.sti2.engines;

import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.model.benchmark_result.BenchmarkQueryResult;
import at.sti2.model.query.Query;
import at.sti2.model.query.QueryContainer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message.Level;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.internal.io.ResourceFactory;

@Slf4j
public class Drools implements BenchmarkEngine {

  private static final String ENGINE_NAME = "Drools";

  private KieContainer kieContainer;

  private KieSession kieSession;

  @Override
  public String getEngineName() {
    return ENGINE_NAME;
  }

  @Override
  public void setSettings(Map<String, Object> settings) {}

  @Override
  public void prepare(TestCaseConfiguration testCase) {
    String absoluteDataFilePath =
        BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".drools", false);
    String absoluteRuleFilePath = BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".drl", false);

    if (StringUtils.isNotEmpty(absoluteDataFilePath)) {
      KieServices kieServices = KieServices.Factory.get();
      KieFileSystem kfs = kieServices.newKieFileSystem();

      log.info("Loading rules from path: {}", absoluteRuleFilePath);
      kfs.write(ResourceFactory.newFileResource(absoluteRuleFilePath));

      kieServices
          .getRepository()
          .addKieModule(() -> kieServices.getRepository().getDefaultReleaseId());

      KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

      kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());

      if (kieBuilder.getResults().hasMessages(Level.ERROR)) {
        log.error("Error while preparing Drools!");
      }
      try {
        kieSession = kieContainer.newKieSession();
        log.info("Loading data from path: {}", absoluteDataFilePath);
        List<Object> dataObjects = prepareDataObjects(absoluteDataFilePath);
        dataObjects.forEach(kieSession::insert);
      } catch (Exception e) {
        log.error("Error loading data into Drools!", e);
      }
    }
  }

  @Override
  public Map<String, BenchmarkQueryResult> executeQueries(TestCaseConfiguration testCase) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Map<String, BenchmarkQueryResult> testCaseResults = new HashMap<>();
    String queryFileClassPath =
        BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, "_queries.json", true);
    QueryContainer queryContainer = BenchmarkUtils.load(queryFileClassPath, QueryContainer.class);
    if (queryContainer != null) {
      for (Query query : queryContainer.getQueries()) {
        log.info("Evaluating query: {}", query.getQuery());

        Future<QueryResults> resultFuture = null;
        try {
          long start = System.currentTimeMillis();
          resultFuture = executor.submit(new DroolsQueryTask(query.getQuery()));
          QueryResults results = resultFuture.get(30, TimeUnit.MINUTES);
          long end = System.currentTimeMillis();
          int numberOfResults = results == null ? 0 : results.size();
          testCaseResults.put(
              query.getName(),
              new BenchmarkQueryResult(query.getQuery(), (end - start), numberOfResults));
        } catch (TimeoutException e) {
          resultFuture.cancel(true);
          log.info("TIMEOUT!");
        } catch (Exception e) {
          log.error("Error evaluating query {} with Drools!", query.getName(), e);
        }
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
    if (kieSession != null) {
      kieSession.dispose();
    }
    if (kieContainer != null) {
      kieContainer.dispose();
    }
  }

  @Override
  public void shutDown() {}

  private List<Object> prepareDataObjects(String dataFilePath) throws IOException {
    List<Object> dataObjects = null;
    FileReader dataInput = new FileReader(dataFilePath);
    BufferedReader bufRead = new BufferedReader(dataInput);
    String dataClassType = bufRead.readLine();
    switch (dataClassType) {
      case "DataClass2":
        dataObjects = prepareDC2(bufRead);
        break;
      case "DataClass3":
        dataObjects = prepareDC3(bufRead);
        break;
      case "DataClass5":
        dataObjects = prepareDC5(bufRead);
        break;
      case "DataClass6":
        dataObjects = prepareDC6(bufRead);
        break;
      case "SG_TC":
        dataObjects = prepareSameGeneration(bufRead);
        break;
      case "Stratified_Negation":
        dataObjects = prepareStratifiedNegation(bufRead);
        break;
    }
    bufRead.close();
    return dataObjects;
  }

  private List<Object> prepareDC2(BufferedReader bufRead) throws IOException {
    List<Object> dC2Object = new ArrayList<>();
    String first, line = bufRead.readLine();
    while (line != null) {
      first = bufRead.readLine();
      dC2Object.add(new DataClass2(line, first));
      line = bufRead.readLine();
    }
    return dC2Object;
  }

  private List<Object> prepareDC3(BufferedReader bufRead) throws IOException {
    List<Object> dC3Object = new ArrayList<>();
    String first, second, line = bufRead.readLine();
    while (line != null) {
      first = bufRead.readLine();
      second = bufRead.readLine();
      dC3Object.add(new DataClass3(line, first, second));
      line = bufRead.readLine();
    }
    return dC3Object;
  }

  private List<Object> prepareDC5(BufferedReader bufRead) throws IOException {
    List<Object> dC5Object = new ArrayList<>();
    String first, second, third, line = bufRead.readLine();
    while (line != null) {
      first = bufRead.readLine();
      second = bufRead.readLine();
      third = bufRead.readLine();
      dC5Object.add(new DataClass5(line, first, second, third, "", ""));
      line = bufRead.readLine();
    }
    return dC5Object;
  }

  private List<Object> prepareDC6(BufferedReader bufRead) throws IOException {
    List<Object> dC6Object = new ArrayList<>();
    String first, second, third, fourth, fifth, sixth, line = bufRead.readLine();
    while (line != null) {
      first = bufRead.readLine();
      second = bufRead.readLine();
      third = bufRead.readLine();
      fourth = bufRead.readLine();
      fifth = bufRead.readLine();
      sixth = bufRead.readLine();
      dC6Object.add(new DataClass6(line, first, second, third, fourth, fifth, sixth));
      line = bufRead.readLine();
    }
    return dC6Object;
  }

  private List<Object> prepareSameGeneration(BufferedReader bufRead) throws IOException {
    List<Object> sameGenerationObjects = new ArrayList<>();
    String first, second, line = bufRead.readLine();
    while (line != null) {
      if (line.compareTo("par") == 0) {
        first = bufRead.readLine();
        second = bufRead.readLine();
        sameGenerationObjects.add(new ClassPar(first, second));
      } else if (line.compareTo("sib") == 0) {
        first = bufRead.readLine();
        second = bufRead.readLine();
        sameGenerationObjects.add(new ClassSib(first, second));
      }
      line = bufRead.readLine();
    }
    return sameGenerationObjects;
  }

  private List<Object> prepareStratifiedNegation(BufferedReader bufRead) throws IOException {
    List<Object> stratifiedNegationObjects = new ArrayList<>();
    String first, second, line = bufRead.readLine();
    while (line != null) {
      if (line.compareTo("move") == 0) {
        first = bufRead.readLine();
        second = bufRead.readLine();
        stratifiedNegationObjects.add(new Move(first, second));
      }
      else if (line.compareTo("par") == 0) {
        first = bufRead.readLine();
        second = bufRead.readLine();
        stratifiedNegationObjects.add(new ClassPar(first, second));
      }
      else if (line.compareTo("sib") == 0) {
        first = bufRead.readLine();
        second = bufRead.readLine();
        stratifiedNegationObjects.add(new ClassSib(first, second));
      }
      line = bufRead.readLine();
    }
    return stratifiedNegationObjects;
  }

  public static class DataClass2 {
    public String name;
    public String first;

    public DataClass2() {}

    public DataClass2(String nameIn, String firstIn) {
      this.name = nameIn;
      this.first = firstIn;
    }

    public String getName() {
      return this.name;
    }

    public String getFirst() {
      return this.first;
    }

    public String toString() {
      return this.name + "(" + this.first + ").";
    }
  }

  public static class DataClass3 {
    public String name;
    public String first;
    public String second;

    public DataClass3() {}

    public DataClass3(String nameIn, String firstIn, String secondIn) {
      this.name = nameIn;
      this.first = firstIn;
      this.second = secondIn;
    }

    public String getName() {
      return this.name;
    }

    public String getFirst() {
      return this.first;
    }

    public String getSecond() {
      return this.second;
    }

    public String toString() {
      return this.name + "(" + this.first + ", " + this.second + ").";
    }
  }

  public static class DataClass5 {
    public String name;
    public String first;
    public String second;
    public String third;
    public String fourth;
    public String fifth;

    public DataClass5() {}

    public DataClass5(
        String nameIn,
        String firstIn,
        String secondIn,
        String thirdIn,
        String fourthIn,
        String fifthIn) {
      this.name = nameIn;
      this.first = firstIn;
      this.second = secondIn;
      this.third = thirdIn;
      this.fourth = fourthIn;
      this.fifth = fifthIn;
    }

    public String getName() {
      return this.name;
    }

    public String getFirst() {
      return this.first;
    }

    public String getSecond() {
      return this.second;
    }

    public String getThird() {
      return this.third;
    }

    public String getFourth() {
      return this.fourth;
    }

    public String getFifth() {
      return this.fifth;
    }

    public String toString() {
      return this.name
          + "("
          + this.first
          + ", "
          + this.second
          + ", "
          + this.third
          + ", "
          + this.fourth
          + ", "
          + this.fifth
          + ").";
    }
  }

  public static class DataClass6 {
    public String name;
    public String first;
    public String second;
    public String third;
    public String fourth;
    public String fifth;
    public String sixth;

    public DataClass6() {}

    public DataClass6(
        String nameIn,
        String firstIn,
        String secondIn,
        String thirdIn,
        String fourthIn,
        String fifthIn,
        String sixthIn) {
      this.name = nameIn;
      this.first = firstIn;
      this.second = secondIn;
      this.third = thirdIn;
      this.fourth = fourthIn;
      this.fifth = fifthIn;
      this.sixth = sixthIn;
    }

    public String getName() {
      return this.name;
    }

    public String getFirst() {
      return this.first;
    }

    public String getSecond() {
      return this.second;
    }

    public String getThird() {
      return this.third;
    }

    public String getFourth() {
      return this.fourth;
    }

    public String getFifth() {
      return this.fifth;
    }

    public String getSixth() {
      return this.sixth;
    }

    public String toString() {
      return this.name
          + "("
          + this.first
          + ", "
          + this.second
          + ", "
          + this.third
          + ", "
          + this.fourth
          + ", "
          + this.fifth
          + ").";
    }
  }

  public static class General {
    private String first;
    private String second;

    public General() {}

    public General(String firstIn, String secondIn) {
      this.first = firstIn;
      this.second = secondIn;
    }

    public String getFirst() {
      return this.first;
    }

    public String getSecond() {
      return this.second;
    }
  }

  public static class ClassPar extends General {
    public ClassPar() {
      super();
    }

    public ClassPar(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "par(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class ClassSib extends General {
    public ClassSib() {
      super();
    }

    public ClassSib(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "sib(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class ClassSG extends General {
    public ClassSG() {
      super();
    }

    public ClassSG(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "sg(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class ClassTC extends General {
    public ClassTC() {
      super();
    }

    public ClassTC(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "tc(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class ClassSG2 extends General {
    public ClassSG2() {
      super();
    }

    public ClassSG2(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "sg2(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class ClassNonSG extends General {
    public ClassNonSG() {
      super();
    }

    public ClassNonSG(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "nonsg(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class Move extends General {
    public Move() {
      super();
    }

    public Move(String firstIn, String secondIn) {
      super(firstIn, secondIn);
    }

    public String toString() {
      return "move(" + this.getFirst() + ", " + this.getSecond() + ").";
    }
  }

  public static class Win {
    private String first;

    public Win() {}

    public Win(String firstIn) {
      this.first = firstIn;
    }

    public String getFirst() {
      return this.first;
    }

    public String toString() {
      return "win(" + this.getFirst() + ").";
    }
  }

  class DroolsQueryTask implements Callable<QueryResults> {

    private String dataClassName;

    public DroolsQueryTask(String dataClassName) {
      this.dataClassName = dataClassName;
    }

    @Override
    public QueryResults call() throws Exception {
      kieSession.fireAllRules();
      if (StringUtils.isNotEmpty(dataClassName)) {
        return kieSession.getQueryResults("selectQuery", dataClassName);
      }
      return null;
    }
  }
}
