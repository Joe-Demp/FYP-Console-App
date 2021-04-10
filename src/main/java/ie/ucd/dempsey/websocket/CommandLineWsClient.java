package ie.ucd.dempsey.websocket;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;
import service.util.Gsons;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.nonNull;

public class CommandLineWsClient extends WebSocketClient {
    public static final long HOST_REQUEST_PERIOD = 5L;
    private static final InetSocketAddress PING_SERVER = new InetSocketAddress(Constants.MOBILE_PING_SERVER_PORT);
    private static Logger logger = LoggerFactory.getLogger(CommandLineWsClient.class);

    private UUID assignedUUID;
    private AtomicReference<URI> cloudService = new AtomicReference<>();
    private ScheduledExecutorService hostRequestScheduler = Executors.newSingleThreadScheduledExecutor();
    private Gson gson;

    /**
     * Constructs a WebSocketClient instance and sets it to the connect to the
     * specified URI. The channel does not attempt to connect automatically. The connection
     * will be established once you call <var>connect</var>.
     *
     * @param serverUri the server URI to connect to
     */
    public CommandLineWsClient(URI serverUri) {
        super(serverUri);
        gson = Gsons.mobileClientGson();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.debug("Connection opened to server at " + getConnection().getRemoteSocketAddress());
        hostRequestScheduler.scheduleAtFixedRate(
                this::requestApplicationHost, HOST_REQUEST_PERIOD, HOST_REQUEST_PERIOD, TimeUnit.SECONDS
        );
    }

    @Override
    public void onMessage(String message) {
        logger.debug("Received: " + message);
        Message messageObj = gson.fromJson(message, Message.class);

        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.SERVER_HEARTBEAT_REQUEST:
                handleServerHeartbeatRequest();
                break;
            case Message.MessageTypes.NODE_INFO_REQUEST:
                handleNodeInfoRequest((NodeInfoRequest) messageObj);
                sendMobileClientInfo();
                requestApplicationHost();
                break;
            case Message.MessageTypes.HOST_RESPONSE:
                handleHostResponse((HostResponse) messageObj);
                break;
        }
    }

    private void handleServerHeartbeatRequest() {
        if (nonNull(assignedUUID)) {
            sendMobileClientInfo();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        hostRequestScheduler.shutdown();
        logger.debug("Closing the WebSocketClient: ");
        logger.debug("hostRequestScheduler Shutdown? {} Terminated? {}",
                hostRequestScheduler.isShutdown(), hostRequestScheduler.isTerminated());
        logger.debug("code: " + code);
        logger.debug("reason: " + reason);
        logger.debug("remote: " + remote);
    }

    @Override
    public void onError(Exception ex) {
        logger.error(ex.getMessage());
    }

    public void sendMobileClientInfo() {
        MobileClientInfo info = new MobileClientInfo(assignedUUID, PING_SERVER);
        sendAsJson(info);
    }

    /**
     * Gets the most recent service {@code URI} allocated to this client by the Orchestrator.
     * <p>
     * There is no guarantee that the URI will be non-null, nor that the URI points to a running service.
     * </p>
     *
     * @return the service's {@code URI} if one has been allocated thusfar, otherwise null.
     */
    public URI getCloudService() {
        return cloudService.get();
    }

    public AtomicReference<URI> getCloudServiceReference() { return cloudService; }

    public UUID getAssignedUUID() {
        return this.assignedUUID;
    }

    private void setCloudService(URI uri) {
        cloudService.set(uri);
    }

    private void requestApplicationHost() {
        HostRequest serviceRequest = new HostRequest(assignedUUID);
        sendAsJson(serviceRequest);
    }

    private void handleNodeInfoRequest(NodeInfoRequest request) {
        assignedUUID = request.getAssignedUUID();
    }

    public void handleHostResponse(HostResponse response) {
        URI currentService = getCloudService();
        URI newService = response.getServiceHostAddress();

        if (nonNull(currentService) && !currentService.equals(newService)) {
            logger.debug("New Service Host Address! {} -> {}", currentService, newService);
        }
        setCloudService(newService);
    }

    public void sendAsJson(Message message) {
        send(gson.toJson(message));
    }

    @Override
    public void send(String s) {
        logger.debug(String.valueOf(s));
        super.send(s);
    }
}
