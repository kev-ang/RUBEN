package at.sti2.engines;

import at.sti2.benchmark.BenchmarkUtils;
import at.sti2.configuration.TestCaseConfiguration;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class Stardog implements BenchmarkEngine {

    private static final String ENGINE_NAME = "Stardog";

    private static final String DATABASE_IDENTIFIER = "OpenRuleBenchDatabase";
    private static final String SERVER_URL = "http://localhost:5820";
    private static final String USER = "admin";
    private static final String PASSWORD = "admin";

    private static final String NAMESPACE = "http://sti2.at/";

    private AdminConnection adminConnection;
    private Connection databaseConnection;

    public Stardog() {
        log.info("Setting up docker container for stardog evaluation.");
        try {
            BenchmarkUtils.startContainerForEngine(ENGINE_NAME);
            log.info(
                "Started docker container, waiting for 30 seconds to make sure it is started...");
            Thread.sleep(30 * 1000);
            log.info("Stardog should be started now!");
        } catch (Exception e) {
            log.error("Error setting up docker for stardog!", e);
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public void setSettings(Map<String, Object> settings) {
    }

    @Override
    public void prepare(TestCaseConfiguration testCase) {
        String absoluteDataPath =
            BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".stardog",
                                       false);
        if (StringUtils.isNotEmpty(absoluteDataPath)) {
            try {
                adminConnection =
                    AdminConnectionConfiguration.toServer(SERVER_URL)
                                                .credentials(USER, PASSWORD)
                                                .connect();

                dropDatabase(adminConnection);
                adminConnection.newDatabase(DATABASE_IDENTIFIER).create();

                databaseConnection =
                    ConnectionConfiguration.to(DATABASE_IDENTIFIER)
                                           .server(SERVER_URL)
                                           .credentials(USER, PASSWORD)
                                           .connect();

                databaseConnection.begin();
                List<Statement> statements =
                    prepareStatements(absoluteDataPath);
                databaseConnection.add().graph(statements);

                String absoluteRulePath =
                    BenchmarkUtils.getFilePath(ENGINE_NAME, testCase, ".ttl",
                                               false);
                log.info("Loading rule from path: {}", absoluteRulePath);
                databaseConnection.add().io()
                                  .file(new File(absoluteRulePath).toPath());
                databaseConnection.commit();
            } catch (Exception e) {
                log.error("Error while preparing stardog!", e);
            }
        }
    }

    @Override
    public int executeQuery(String query) {
        SelectQuery aQuery =
            databaseConnection
                .select("select * where {" + query + "}")
                .reasoning(true).timeout(30 * 60 * 1000);
        SelectQueryResult result = aQuery.execute();
        int numberOfResults = (int) result.stream().count();
        result.close();
        return numberOfResults;
    }

    @Override
    public void cleanUp() {
        dropDatabase(adminConnection);
    }

    @Override
    public void shutDown() {
        try {
            BenchmarkUtils.stopContainerForEngine(ENGINE_NAME);
        } catch (IOException e) {
            log.error("Error stopping docker container for stardog!", e);
        }
    }

    private void dropDatabase(AdminConnection aAdminConnection) {
        if (aAdminConnection.list().contains(DATABASE_IDENTIFIER)) {
            aAdminConnection.drop(DATABASE_IDENTIFIER);
        }
    }

    private List<Statement> prepareStatements(String absoluteDataPath)
        throws IOException {
        List<Statement> statements = new ArrayList<>();
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
