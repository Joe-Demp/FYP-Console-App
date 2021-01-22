package ie.ucd.dempsey;

import ie.ucd.dempsey.app.CommandLineApplication;
import ie.ucd.dempsey.websocket.CommandLineWsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.net.URI;

public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-o", "--orchestrator"}, defaultValue = "ws://csi420-01-vm1.ucd.ie",
            paramLabel = "Orchestrator URI", description = "Should be of the form: ws://{host}[:{port}]")
    private URI orchestratorUri;

    @Option(names = {"-s", "--service"}, defaultValue = "docker.tar", paramLabel = "Service Name",
            description = "The name of the service you wish to contact via this client")
    private String serviceName;

    public static void main(String[] args) {
        // take in an Orchestrator address, return the status code
        System.exit(new CommandLine(new Main()).execute(args));
    }


    @Override
    public void run() {
        // take in an address
        logger.debug("command line orchestrator address: " + orchestratorUri);

        CommandLineWsClient client = new CommandLineWsClient(orchestratorUri, serviceName);
        client.connect();

        // start the application
        CommandLineApplication app = new CommandLineApplication(client);
        app.start();

        logger.debug("Finished.");
    }
}
