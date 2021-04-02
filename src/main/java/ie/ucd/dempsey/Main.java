package ie.ucd.dempsey;

import ie.ucd.dempsey.app.CommandLineApplication;
import ie.ucd.dempsey.websocket.CommandLineWsClient;
import ie.ucd.dempsey.websocket.PingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import service.core.Constants;

import java.net.InetSocketAddress;
import java.net.URI;

public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-p", "--ping-port"}, defaultValue = "8092", paramLabel = "Ping Server Port Number")
    private int pingPortNumber;

    @Option(names = {"-o", "--orchestrator"}, defaultValue = "ws://csi420-01-vm1.ucd.ie",
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

        PingServer pingServer = new PingServer(new InetSocketAddress(Constants.MOBILE_PING_SERVER_PORT));
        Thread pingThread = new Thread(pingServer);
        pingThread.start();

        CommandLineWsClient client = new CommandLineWsClient(orchestratorUri);
        client.connect();

        // start the application
        CommandLineApplication app = new CommandLineApplication(client);
        app.start();

        try {
            final int timeout = 3 * 1000;
            pingServer.stop(timeout);
            pingThread.join(timeout);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        logger.debug("Finished.");
    }
}
