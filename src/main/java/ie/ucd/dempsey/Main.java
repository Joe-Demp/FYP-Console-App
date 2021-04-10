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
import java.net.URL;
import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Main implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Option(names = {"-p", "--ping-port"}, defaultValue = "8092", paramLabel = "Ping Server Port Number")
    private int pingPortNumber;

    @Option(names = {"-o", "--orchestrator"}, defaultValue = "ws://csi420-01-vm1.ucd.ie",
            paramLabel = "Orchestrator URI", description = "Should be of the form: ws://{host}[:{port}]")
    private URI orchestratorUri;

    public static void main(String[] args) {
        // take in an Orchestrator address, return the status code
//        System.exit(new CommandLine(new Main()).execute(args));

        // todo remove this test section
        AtomicReference<URI> uriRef = new AtomicReference<>();
        ExecutorService exec = Executors.newSingleThreadExecutor();
        HttpClient httpClient = HttpClient.newBuilder().executor(exec).build();
        CommandLineHttpClient client = new CommandLineHttpClient(uriRef, httpClient);
        String getResult = null;

        assert uriRef.get() == null;
        try {
            getResult = client.getData();
        } catch (RuntimeException re) {
            logger.error("Error getting data with null reference");
            logger.debug("getResult={}", getResult);
        }

        uriRef.set(URI.create("http://localhost:8080"));
//        URI uri = uriRef.get();
//        logger.debug(uri.toString());
//        logger.debug(uri.getScheme());
//        logger.debug(uri.getHost());
//        logger.debug(String.valueOf(uri.getPort()));
//        logger.debug(uri.getPath());
//        try {
//            logger.debug(new URL(uri.getScheme(), uri.getHost(), uri.getPort(), "/").toString());
//        } catch (Exception e) {}
        getResult = client.getData();
        logger.debug("getResult={}", getResult);

        client.run();

        getResult = client.getData();
        logger.debug("getResult={}", getResult);

        exec.shutdownNow();
    }


    @Override
    public void run() {
        // take in an address
        logger.debug("command line orchestrator address: " + orchestratorUri);

        PingServer pingServer = new PingServer(new InetSocketAddress(Constants.MOBILE_PING_SERVER_PORT));
        Thread pingThread = new Thread(pingServer);
        pingThread.start();

        CommandLineWsClient wsClient = new CommandLineWsClient(orchestratorUri);
        wsClient.connect();

        // start the Http data process
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new CommandLineHttpClient(wsClient.getCloudServiceReference(), null),
                2, 2, TimeUnit.SECONDS);

        // start the application
        CommandLineApplication app = new CommandLineApplication(wsClient);
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
