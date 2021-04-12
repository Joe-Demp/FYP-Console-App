package ie.ucd.dempsey;

import ie.ucd.dempsey.app.CommandLineApplication;
import ie.ucd.dempsey.app.CommandLineHttpClient;
import ie.ucd.dempsey.websocket.CommandLineWsClient;
import ie.ucd.dempsey.websocket.PingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import service.core.Constants;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-p", "--ping-port"}, defaultValue = "8092", paramLabel = "Ping Server Port Number")
    private int pingPortNumber;

    @Option(names = {"-o", "--orchestrator"}, defaultValue = "ws://csi420-01-vm1.ucd.ie",
            paramLabel = "Orchestrator URI", description = "Should be of the form: ws://{host}[:{port}]")
    private URI orchestratorUri;
    private PingServer pingServer;
    private Thread pingThread;
    private CommandLineWsClient wsClient;
    private ExecutorService httpClientExecutor = Executors.newSingleThreadExecutor();
    private ScheduledExecutorService clHttpClientExecutor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        // take in an Orchestrator address, return the status code
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public void run() {
        // take in an address
        logger.debug("command line orchestrator address: " + orchestratorUri);
        startPingServer();
        startWebSocketClient();
        startHttpClient();
        startApplication();
        shutdownEverything();
        logger.debug("Finished.");
    }

    private void startWebSocketClient() {
        wsClient = new CommandLineWsClient(orchestratorUri);
        wsClient.connect();
    }

    private void startPingServer() {
        pingServer = new PingServer(new InetSocketAddress(Constants.MOBILE_PING_SERVER_PORT));
        pingThread = new Thread(pingServer);
        pingThread.start();
    }

    // schedules the CommandLineHttpClient task, that sends data to cloud services
    private void startHttpClient() {
        HttpClient httpClient = HttpClient.newBuilder().executor(httpClientExecutor).build();
        clHttpClientExecutor.scheduleAtFixedRate(
                new CommandLineHttpClient(wsClient.getCloudServiceReference(), httpClient),
                2, 2, TimeUnit.SECONDS
        );
    }

    private void startApplication() {
        CommandLineApplication app = new CommandLineApplication(wsClient);
        app.start();
    }

    /*
     * stops the executor that the CommandLineHttpClient's HttpClient uses. Causes a delay and error with mvn exec if
     * this is not shutdown manually.
     * Then stops the scheduled CommandLineHttpClient task.
     * Then shuts down the PingServer.
     */
    private void shutdownEverything() {
        httpClientExecutor.shutdown();
        clHttpClientExecutor.shutdown();

        try {
            final int timeout = 3 * 1000;
            pingServer.stop(timeout);
            pingThread.join(timeout);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
