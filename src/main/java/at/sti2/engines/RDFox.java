package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.utils.BenchmarkUtils;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.ConnectionFactory;
import tech.oxfordsemantic.jrdfox.client.Cursor;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.client.TransactionType;
import tech.oxfordsemantic.jrdfox.client.UpdateType;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

@Slf4j
public class RDFox implements RuleEngine {

    private static final String LICENSE_KEY_PATH =
        "/Users/Kevin/Documents/GitHub/Kev_ang/RuleEngineBenchmark/src/main/resources/RDFox.lic";

    private static final String SERVER_DIRECTORY_PATH =
        "/Users/Kevin/Documents/GitHub/Kev_ang/RuleEngineBenchmark/data/rdfox";

    private static final String LOCAL_SERVER_URL = "rdfox:local";

    private static final String LOCAL_SERVER_USER = "admin";

    private static final String LOCAL_SERVER_PASSWORD = "admin";

    private static final String DATASTORE_IDENTIFIER = "RDFoxRuben";

    private static final String NAMESPACE = "http://sti2.at/";

    private String engineName;

    private ServerConnection serverConnection;

    private DataStoreConnection dataStoreConnection;

    Prefixes prefixes;

    public RDFox() {
        Map<String, String> serverParams = new HashMap<>();
        serverParams.put("license-file", LICENSE_KEY_PATH);
        try {
            Map<String, String> serverParameters = new HashMap<>();
            serverParameters.put("persist-ds", "file");
            serverParameters.put("persist-roles", "file");
            serverParameters.put("server-directory", SERVER_DIRECTORY_PATH);
            serverParameters.put("license-file", LICENSE_KEY_PATH);
            String[] warnings =
                ConnectionFactory.startLocalServer(serverParameters);
            /* ... handle warnings ... */

            if (ConnectionFactory.getNumberOfLocalServerRoles() == 0) {
                System.out.println("Initializing server directory...");
                ConnectionFactory.createFirstLocalServerRole(LOCAL_SERVER_USER,
                                                             LOCAL_SERVER_PASSWORD);
            } else {
                System.out.println("Server directory is already initialized!");
            }

            serverConnection =
                ConnectionFactory.newServerConnection(LOCAL_SERVER_URL,
                                                      LOCAL_SERVER_USER,
                                                      LOCAL_SERVER_PASSWORD);

            if (!serverConnection.containsDataStore(DATASTORE_IDENTIFIER)) {
                serverConnection.createDataStore(DATASTORE_IDENTIFIER,
                                                 Collections.emptyMap());
            }
            serverConnection.setNumberOfThreads(2);

            prefixes = new Prefixes();
            prefixes.declareStandardPrefixes();
            prefixes.declarePrefix(":", NAMESPACE);
        } catch (JRDFoxException e) {
            log.error("Error while starting local RDFox Server", e);
        }
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
            BenchmarkUtils.getFilePath(testDataPath, engineName,
                                       testCase, ".nt");
        if (BenchmarkUtils.fileExists(absoluteDataPath)) {
            try {
                dataStoreConnection = serverConnection.newDataStoreConnection(
                    DATASTORE_IDENTIFIER);

                /*
                Add all rules before the facts or add rules and facts in an arbitrary order but grouped in a single transaction.
                This will usually increase the performance of the first reasoning operation.

                https://www.oxfordsemantic.tech/blog/the-dos-and-donts-of-rule-and-query-writing
                 */

                String absoluteRulePath =
                    BenchmarkUtils.getFilePath(testDataPath, engineName,
                                               testCase, ".rls");

                System.out.println("Importing rules from a file...");
                loadData(absoluteRulePath);

                long start = System.currentTimeMillis();
                log.info("Importing RDF Data from file {}", absoluteDataPath);
                loadData(absoluteDataPath);
                long end = System.currentTimeMillis();
                log.info("Loading took: {} ms", (end - start));

                log.info("Number of tuples after import: " +
                         getTriplesCount(dataStoreConnection, "all"));

            } catch (Exception e) {
                log.error("Error while preparing RDFox!", e);
            }
        }
    }

    @Override
    public int executeQuery(String query) {
        try (Cursor cursor = dataStoreConnection.createCursor(null,
                                                              prefixes,
                                                              "select * where {" +
                                                              query + "}",
                                                              Collections.emptyMap())) {
            int numberOfRows = 0;
            for (long multiplicity = cursor.open(); multiplicity != 0;
                 multiplicity = cursor.advance()) {
                ++numberOfRows;
            }
            return numberOfRows;
        } catch (JRDFoxException e) {
            log.error("Error while evaluating RDFox query!", e);
        }
        return -1;
    }

    @Override
    public void cleanUp() {
        try {
            dataStoreConnection.clear();
        } catch (JRDFoxException e) {
            log.error("Error while cleaning datastore!", e);
        }
    }

    @Override
    public void shutDown() {
        try {
            dataStoreConnection.clear();
            dataStoreConnection.close();
            serverConnection.close();
        } catch (JRDFoxException e) {
            log.error("Error while shutting down RDFox!", e);
        }
    }

    protected static long getTriplesCount(
        DataStoreConnection dataStoreConnection, String factDomain)
        throws JRDFoxException {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("fact-domain", factDomain);
        try (Cursor cursor = dataStoreConnection.createCursor(null,
                                                              Prefixes.s_emptyPrefixes,
                                                              "SELECT ?X ?Y ?Z WHERE { ?X ?Y ?Z }",
                                                              parameters)) {
            dataStoreConnection.beginTransaction(TransactionType.READ_ONLY);
            try {
                long result = 0;
                for (long multiplicity = cursor.open(); multiplicity != 0;
                     multiplicity = cursor.advance()) {
                    result += multiplicity;
                }
                return result;
            } finally {
                dataStoreConnection.rollbackTransaction();
            }
        }
    }

    private void loadData(String dataPath)
        throws JRDFoxException, IOException {
        try (InputStream inputStream = new BufferedInputStream(
            new FileInputStream(dataPath))) {
            dataStoreConnection.importData(UpdateType.ADDITION,
                                           prefixes,
                                           inputStream);
        }
    }
}