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
import com.stardog.stark.io.RDFFormats;
import com.stardog.stark.query.SelectQueryResult;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class Stardog implements BenchmarkEngine {

    private static final String ENGINE_IDENTIFIER = "Stardog";

    private static final String DATABASE_IDENTIFIER = "OpenRuleBenchDatabase";
    private static final String SERVER_URL = "http://localhost:5820";
    private static final String USER = "admin";
    private static final String PASSWORD = "admin";

    private static final String NAMESPACE = "http://sti2.at/";

    private AdminConnection adminConnection;
    private Connection databaseConnection;

    private String engineName;

    public Stardog() {
        log.info("Setting up docker container for stardog evaluation.");
        try {
            BenchmarkUtils.startContainerForEngine(ENGINE_IDENTIFIER);
            log.info(
                "Started docker container, waiting for 30 seconds to make sure it is started...");
            Thread.sleep(30 * 1000);
            log.info("Stardog should be started now!");
        } catch (Exception e) {
            log.error("Error setting up docker for stardog!", e);
        }
    }

    @Override
    public String getEngineIdentifier() {
        return ENGINE_IDENTIFIER;
    }

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
            BenchmarkUtils.getFilePath(testDataPath, ENGINE_IDENTIFIER,
                                       testCase, ".nt");
        if (BenchmarkUtils.fileExists(absoluteDataPath)) {
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
                                           .reasoning(true)
                                           .connect();

                databaseConnection.begin();
                List<Statement> statements =
                    prepareStatements(absoluteDataPath);

                //statements.forEach(
                //    statement -> databaseConnection.add().statement(statement));

                databaseConnection.add().io().format(RDFFormats.NTRIPLES)
                                  .stream(
                                      new FileInputStream(absoluteDataPath));

                String absoluteRulePath =
                    BenchmarkUtils.getFilePath(testDataPath, ENGINE_IDENTIFIER,
                                               testCase, ".ttl");
                log.info("Loading rule from path: {}", absoluteRulePath);
                databaseConnection.add().io()
                                  .format(RDFFormats.TURTLE)
                                  .stream(
                                      new FileInputStream(absoluteRulePath));
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
                .timeout(30 * 60 * 1000);
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
            BenchmarkUtils.stopContainers();
        } catch (IOException | URISyntaxException e) {
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