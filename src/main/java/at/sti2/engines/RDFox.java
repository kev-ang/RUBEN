package at.sti2.engines;

import at.sti2.configuration.TestCaseConfiguration;
import at.sti2.utils.BenchmarkUtils;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
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

    private static final String LICENSE_KEY_PATH = "/home/user/rdfox.lic";

    private static final String DATASTORE_IDENTIFIER = "RDFoxRuben";

    private static final String NAMESPACE = "http://sti2.at/";

    private String engineName;

    private ServerConnection serverConnection;

    private DataStoreConnection dataStoreConnection;

    Prefixes prefixes = new Prefixes();

    public RDFox() {
        Map<String, String> serverParams = new HashMap<>();
        serverParams.put("license-file", LICENSE_KEY_PATH);
        try {
            String[] localServers =
                ConnectionFactory.startLocalServer(serverParams);
            serverConnection =
                ConnectionFactory.newServerConnection(localServers[0], "", "");
            serverConnection.createDataStore(DATASTORE_IDENTIFIER,
                                             Collections.emptyMap());
            serverConnection.setNumberOfThreads(2);

            prefixes.declareStandardPrefixes();
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

                log.info("Importing RDF Data from file {}", absoluteDataPath);
                try (InputStream inputStream = new BufferedInputStream(
                    new FileInputStream(absoluteDataPath))) {
                    dataStoreConnection.importData(UpdateType.ADDITION,
                                                   Prefixes.s_emptyPrefixes,
                                                   inputStream);
                }
                log.info("Number of tuples after import: " +
                         getTriplesCount(dataStoreConnection, "all"));

                String absoluteRulePath =
                    BenchmarkUtils.getFilePath(testDataPath, engineName,
                                               testCase, ".ttl");

                System.out.println("Importing rules from a file...");
                try (InputStream inputStream = new BufferedInputStream(
                    new FileInputStream(absoluteRulePath))) {
                    dataStoreConnection.importData(UpdateType.ADDITION,
                                                   prefixes, inputStream);
                }

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
            dataStoreConnection.close();
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
}