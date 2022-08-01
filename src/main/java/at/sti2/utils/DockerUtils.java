package at.sti2.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Utils for handling docker containers.
 *
 * @author kevin.angele@sti2.at
 */
@Slf4j
public class DockerUtils {

    /**
     * Start container for a given engineName. The engine must be specified in
     * the `docker-compose.yml` file.
     *
     * @param engineName to be started
     * @throws IOException
     */
    public static void startContainerForEngine(String engineName)
        throws IOException {
        String dockerComposeFilePath = new File(".").getCanonicalPath();
        String dockerComposeCommand = "docker-compose -f " +
                                      dockerComposeFilePath +
                                      "/docker-compose.yml" +
                                      " up -d " +
                                      engineName.toLowerCase();
        log.info("Executing: {}", dockerComposeCommand);
        Process p = Runtime.getRuntime().exec(dockerComposeCommand);
        printProcessInputStream(p.getInputStream());
    }

    /**
     * Stop all docker containers.
     *
     * @throws IOException
     */
    public static void stopContainers()
        throws IOException {
        String dockerComposeFilePath = new File(".").getCanonicalPath();
        String dockerComposeCommand = "docker-compose -f " +
                                      dockerComposeFilePath +
                                      "/docker-compose.yml" +
                                      " down -v";
        log.info("Executing: {}", dockerComposeCommand);
        Process p = Runtime.getRuntime().exec(dockerComposeCommand);
        printProcessInputStream(p.getInputStream());
    }

    private static void printProcessInputStream(InputStream inputStream)
        throws IOException {
        String line;
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader rdr = new BufferedReader(isr);
        while ((line = rdr.readLine()) != null) {
            log.info(line);
        }
    }

}