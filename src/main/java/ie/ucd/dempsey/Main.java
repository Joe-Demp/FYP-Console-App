package ie.ucd.dempsey;

import ie.ucd.dempsey.app.CommandLineApplication;
import ie.ucd.dempsey.websocket.CommandLineWsClient;
import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.net.URI;

public class Main implements Runnable {
    private static final String WS_CLIENT_THREAD = "WebSocket_Client_Thread";
    private static final String APPLICATION_THREAD = "Application_Thread";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-o", "--orch"}, defaultValue = "ws://csi420-01-vm1.ucd.ie",
            paramLabel = "Orchestrator URI", description = "Should be of the form: ws://{host}[:{port}]")
    private URI orchestratorUri;

    public static void main(String[] args) {
        // take in an Orchestrator address, return the status code
        System.exit(new CommandLine(new Main()).execute(args));
    }


    @Override
    public void run() {
        // take in an address
        logger.debug("command line orchestrator address: " + orchestratorUri);

        // open a WebSocket Client with that address and
        //  emulate the actions of the Mobile App (ask for a host etc.)
        WebSocketClient client = new CommandLineWsClient(orchestratorUri);
        new Thread(client, WS_CLIENT_THREAD).start();

        // start the application
        Thread cliThread = new Thread(new CommandLineApplication(client), APPLICATION_THREAD);
        cliThread.start();
        try {
            cliThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("Finished.");
    }
}