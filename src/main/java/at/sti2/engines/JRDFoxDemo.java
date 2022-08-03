// RDFox v1.0 Copyright 2013 Oxford University Innovation Limited and subsequent improvements Copyright 2017-2019 by Oxford Semantic Technologies Limited.

package at.sti2.engines;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import tech.oxfordsemantic.jrdfox.Prefixes;
import tech.oxfordsemantic.jrdfox.client.ConnectionFactory;
import tech.oxfordsemantic.jrdfox.client.Cursor;
import tech.oxfordsemantic.jrdfox.client.DataStoreConnection;
import tech.oxfordsemantic.jrdfox.client.ResourceValue;
import tech.oxfordsemantic.jrdfox.client.ServerConnection;
import tech.oxfordsemantic.jrdfox.client.TransactionType;
import tech.oxfordsemantic.jrdfox.client.UpdateType;
import tech.oxfordsemantic.jrdfox.exceptions.JRDFoxException;

public class JRDFoxDemo {

    public static void main(String[] args) throws Exception {
        // This example shows how to instantiate JRDFox in Java, load an RDF file in Turtle, evaluate
        // a query, perform reasoning (i.e., extend the set of triples with all implicit ones), and
        // then evaluate the query again (and get more results as a result of reasoning).

        // Unlike most RDF stores, JRDFox currently maintains a separation between RDF data and the
        // ontology. That is, the data (i.e., the ABox) and the ontology (i.e., the TBox) are currently
        // best maintained in separate files. This is quite different to many (most?) existing RDF
        // stores, where both the ontology and the data are kept in a single file. Thus, in this\
        // example we will use two kinds of axioms for reasoning:
        //
        // - axioms from an OWL ontology stored in file called univ-bench.owl, and
        //
        // - manually created rules written in a format proprietary to RDFox and stored in a file
        //   called additional-rules.txt.
        //
        // The data will be loaded from a file called lubm1.ttl. All files are located in the
        // source directory and will be loaded through Java class loaders -- please refer to Java
        // documentation for more details.

        // We now connect to the local server. The local server can be started explicitly using
        // ConnectionFactory.startLocalServer(), but it will be automatically started when the first
        // connection to it has been made. Our goal is obtain a connection to a data store, but before
        // this can be done, the data store must be created. Hence, we will connect to the server
        // first as server connections allow manipulating data stores. All connections should be closed
        // once they are not to be used any more in order to prevent resource leaks. Server and data
        // store connections implement the AutoCloseable interface so they can be used in
        // try-with-resources statements. By using "rdfox:local" as the server name, we indicate that
        // we wish to access the local server. At present, the role name and password are ignored.
        Map<String, String> serverParams = new HashMap<>();
        serverParams.put("license-file", "/home/user/rdfox.lic");
        String[] localServers = ConnectionFactory.startLocalServer(
            serverParams);
        try (
            ServerConnection serverConnection = ConnectionFactory.newServerConnection(
                localServers[0], "", "")) {

            // We create a data store using the default value for the "type" parameter, which is "par-complex-nn".
            serverConnection.createDataStore("example", Collections.emptyMap());

            // We next specify how many threads the server should use during import of data and reasoning.
            System.out.println("Setting the number of threads...");
            serverConnection.setNumberOfThreads(2);

            // We connect to the data store.
            try (
                DataStoreConnection dataStoreConnection = serverConnection.newDataStoreConnection(
                    "example")) {

                // We next import the RDF data into the store. At present, only Turtle/N-triples files are supported.
                // At the moment, please convert RDF/XML files into Turtle format to load into JRDFox.
                System.out.println("Importing RDF data...");
                try (InputStream inputStream = new BufferedInputStream(
                    JRDFoxDemo.class.getResourceAsStream(
                        "/Users/Kevin/Desktop/RDFox-macOS-x86_64-5.6/examples/data/lubm1.ttl"))) {
                    dataStoreConnection.importData(UpdateType.ADDITION,
                                                   Prefixes.s_emptyPrefixes,
                                                   inputStream);
                }

                // RDFox manages data in several fact domains.
                //
                // - The 'all' domain contains all facts -- that is, both the explicitly given and the derived facts.
                //
                // - The 'derived' domain contains the facts that were derived by reasoning, but were not explicitly given in the input.
                //
                // - The 'explicit' domain contains the facts that were explicitly given in the input.
                //
                // The domain must be specified in various places where queries are evaluated. If a query domain is not
                // specified, the 'all' domain is used.
                System.out.println("Number of tuples after import: " +
                                   getTriplesCount(dataStoreConnection, "all"));

                // SPARQL queries can be evaluated in several ways. One option is to have the query result be written to
                // an output stream in one of the supported formats.
                dataStoreConnection.evaluateStatement(null,
                                                      Prefixes.s_emptyPrefixes,
                                                      "SELECT DISTINCT ?Y WHERE { ?X ?Y ?Z }",
                                                      Collections.emptyMap(),
                                                      System.out,
                                                      "application/sparql-results+json");

                // We now add the ontology and the custom rules to the data. Many functions from the JRDFox API accept an
                // instance of the Prefixes class. Functions that process Turtle or SPARQL inputs use this instance in two ways.
                //
                // - The passed prefixes are treated as if they had been included at the top of the input. This allows
                //   applications to centralise prefix management.
                //
                // - If the input contains additional prefixes, these are placed into the passed instance. This can be
                //   used to discover the prefixes present in the input.
                //
                // In the rest of this demo, one instance is used in all such calls. Hence, all prefixes from all inputs
                // will accumulate in this instance. 
                Prefixes prefixes = new Prefixes();
                prefixes.declareStandardPrefixes();

                // In this example, the rules are kept in a file separate from the ontology. JRDFox supports
                // SWRL rules, so it is possible to store the rules into the OWL ontology.

                System.out.println("Adding the ontology to the store...");
                try (InputStream inputStream = new BufferedInputStream(
                    JRDFoxDemo.class.getResourceAsStream(
                        "/Users/Kevin/Desktop/RDFox-macOS-x86_64-5.6/examples/data/univ-bench.owl"))) {
                    dataStoreConnection.importData(UpdateType.ADDITION,
                                                   prefixes, inputStream);
                }

                System.out.println("Importing rules from a file...");
                try (InputStream inputStream = new BufferedInputStream(
                    JRDFoxDemo.class.getResourceAsStream(
                        "/Users/Kevin/Desktop/RDFox-macOS-x86_64-5.6/examples/data/additional-rules.txt"))) {
                    dataStoreConnection.importData(UpdateType.ADDITION,
                                                   prefixes, inputStream);
                }
                System.out.println("Number of tuples after materialization: " +
                                   getTriplesCount(dataStoreConnection, "all"));

                // We now evaluate the same query as before, but we do so using a cursor, which provides us with
                // programmatic access to individual query results.
                try (Cursor cursor = dataStoreConnection.createCursor(null,
                                                                      prefixes,
                                                                      "SELECT DISTINCT ?Y WHERE { ?X ?Y ?Z }",
                                                                      Collections.emptyMap())) {
                    int numberOfRows = 0;
                    System.out.println();
                    System.out.println(
                        "=======================================================================================");
                    int arity = cursor.getArity();
                    // We iterate trough the result tuples.
                    for (long multiplicity = cursor.open(); multiplicity != 0;
                         multiplicity = cursor.advance()) {
                        // We iterate trough the terms of each tuple.
                        for (int termIndex = 0; termIndex < arity;
                             ++termIndex) {
                            if (termIndex != 0) {
                                System.out.print("  ");
                            }
                            // For each term, we get a Resource object that contains the lexical form and the data type of the term.
                            // One can also access terms as Resource objects from the tech.oxfordsemantic.jrdfox.logic package using
                            // the method Cursor.getResource(int termIndex). Using objects from the tech.oxfordsemantic.jrdfox.logic
                            // package has the benefit of ensuring that at any point each term is represented by at most one Java
                            // object. This benefit, however, comes at a price, since, unlike in the case of Resource objects, the
                            // creation of Resource objects involves a hash table lookup, which in some cases can lead to a significant
                            // overhead.
                            ResourceValue resource =
                                cursor.getResourceValue(termIndex);
                            System.out.print(resource.toString(prefixes));
                        }
                        System.out.print(" * ");
                        System.out.print(multiplicity);
                        System.out.println();
                        ++numberOfRows;
                    }
                    System.out.println(
                        "---------------------------------------------------------------------------------------");
                    System.out.println(
                        "  The number of rows returned: " + numberOfRows);
                    System.out.println(
                        "=======================================================================================");
                    System.out.println();
                }

                // RDFox supports incremental reasoning. One can import facts into the store incrementally by
                // calling DataStoreConnection.importDataFiles() with additional argument UpdateType.ADDITION.
                System.out.println(
                    "Importing triples for incremental reasoning...");
                try (InputStream inputStream = new BufferedInputStream(
                    JRDFoxDemo.class.getResourceAsStream(
                        "/Users/Kevin/Desktop/RDFox-macOS-x86_64-5.6/examples/data/lubm1-new.ttl"))) {
                    dataStoreConnection.importData(UpdateType.ADDITION,
                                                   prefixes, inputStream);
                }
                // Adding the rules/facts changes the number of triples. Note that the store is updated incrementally.
                System.out.println("Number of tuples after addition: " +
                                   getTriplesCount(dataStoreConnection, "all"));
                // One can export the facts from the current store into a file as follows
                File finalFactsFile =
                    File.createTempFile("final-facts", ".ttl");
                System.out.println(
                    "Exporting facts to file '" + finalFactsFile + "'...");
                try (OutputStream outputStream = new BufferedOutputStream(
                    new FileOutputStream(finalFactsFile))) {
                    Map<String, String> parameters =
                        new HashMap<String, String>();
                    parameters.put("fact-domain", "all");
                    dataStoreConnection.exportData(prefixes, outputStream,
                                                   "application/n-triples",
                                                   parameters);
                }
            }
        }
        System.out.println("This is the end of the example!");
    }

    protected static Map<String, String> getParameters(
        String... keyValuePairs) {
        Map<String, String> parameters = new HashMap<String, String>();
        for (int index = 0; index < keyValuePairs.length; index += 2) {
            parameters.put(keyValuePairs[index], keyValuePairs[index + 1]);
        }
        return parameters;
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