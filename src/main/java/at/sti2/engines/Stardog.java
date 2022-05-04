package at.sti2.engines;

import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.model.benchmark_result.BenchmarkQueryResult;
import at.sti2.model.query.Query;
import at.sti2.model.query.QueryContainer;
import com.complexible.stardog.api.Connection;
import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.api.SelectQuery;
import com.complexible.stardog.api.admin.AdminConnection;
import com.complexible.stardog.api.admin.AdminConnectionConfiguration;
import com.stardog.stark.Statement;
import com.stardog.stark.Values;
import com.stardog.stark.query.SelectQueryResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Stardog implements BenchmarkEngine {

  private static final String ENGINE_NAME = "Stardog";

  private static final String DATABASE_IDENTIFIER = "OpenRuleBenchDatabase";
  private static final String SERVER_URL = "http://localhost:5820";
  private static final String USER = "admin";
  private static final String PASSWORD = "admin";

  private static final String NAMESPACE = "http://sti2.at/";

  // 30 mins timeout
  private static final long QUERY_TIMEOUT = 30 * 60 * 1000;

  private AdminConnection adminConnection;
  private Connection databaseConnection;

  @Override
  public String getEngineName() {
    return ENGINE_NAME;
  }

  @Override
  public void setSettings(Map<String, Object> settings) {
  }

  @Override
  public void prepare(TestCaseConfiguration testCase) {
    try {
      adminConnection =
          AdminConnectionConfiguration.toServer(SERVER_URL).credentials(USER, PASSWORD).connect();

      dropDatabase(adminConnection);
      adminConnection.newDatabase(DATABASE_IDENTIFIER).create();

      databaseConnection =
          ConnectionConfiguration.to(DATABASE_IDENTIFIER)
              .server(SERVER_URL)
              .credentials(USER, PASSWORD)
              .connect();

      databaseConnection.begin();
      List<Statement> statements = prepareStatements(testCase);
      databaseConnection.add().graph(statements);

      String absoluteRulePath = BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".ttl", false);
      log.info("Loading rule from path: {}", absoluteRulePath);
      databaseConnection.add().io().file(new File(absoluteRulePath).toPath());
      databaseConnection.commit();
    } catch (Exception e) {
      log.error("Error while preparing stardog!", e);
    }
  }

  @Override
  public Map<String, BenchmarkQueryResult> executeQueries(TestCaseConfiguration testCase) {
    Map<String, BenchmarkQueryResult> testCaseResults = new HashMap<>();
    String queryFileClassPath =
        BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, "_queries.json", true);
    QueryContainer queryContainer = BenchmarkUtils.load(queryFileClassPath, QueryContainer.class);

    for (Query query : queryContainer.getQueries()) {
      log.info("Evaluating query: {}", query.getQuery());

      SelectQuery aQuery =
          databaseConnection
              .select("select * where {" + query.getQuery() + "}")
              .reasoning(true);

      long start = System.currentTimeMillis();
      SelectQueryResult result = aQuery.execute();
      long end = System.currentTimeMillis();
      long numberOfResults = result.stream().count();
      log.info(
          "Query evaluation finished in {}ms delivering {} results",
          (end - start),
          numberOfResults);
      result.close();
      testCaseResults.put(
          query.getName(),
          new BenchmarkQueryResult(query.getQuery(), (end - start), (int) numberOfResults));
    }
    return testCaseResults;
  }

  @Override
  public int executeQuery(String query) {
    SelectQuery aQuery =
        databaseConnection
            .select("select * where {" + query + "}")
            .reasoning(true);
    SelectQueryResult result = aQuery.execute();
    return (int) result.stream().count();
  }

  @Override
  public void cleanUp() {
    dropDatabase(adminConnection);
  }

  @Override
  public void shutDown() {}

  private void dropDatabase(AdminConnection aAdminConnection) {
    if (aAdminConnection.list().contains(DATABASE_IDENTIFIER)) {
      aAdminConnection.drop(DATABASE_IDENTIFIER);
    }
  }

  private List<Statement> prepareStatements(TestCaseConfiguration testCase) throws IOException {
    List<Statement> statements = new ArrayList<>();
    String absoluteDataPath = BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".stardog", false);
    log.info("Loading data from path: {}", absoluteDataPath);
    FileReader dataInput = new FileReader(absoluteDataPath);
    BufferedReader bufRead = new BufferedReader(dataInput);
    String first, second, line = bufRead.readLine();
    while (line != null) {
      first = bufRead.readLine();
      second = bufRead.readLine();
      statements.add(
          Values.statement(
              Values.iri(NAMESPACE, first),
              Values.iri(NAMESPACE, line),
              Values.iri(NAMESPACE, second)));
      line = bufRead.readLine();
    }
    bufRead.close();
    return statements;
  }
}
