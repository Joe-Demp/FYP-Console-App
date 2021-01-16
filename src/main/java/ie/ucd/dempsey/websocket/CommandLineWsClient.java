package ie.ucd.dempsey.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

// todo remove copied javadocs

public class CommandLineWsClient extends WebSocketClient {
    private static final String HEARTBEAT_RESPONSE_SERVICE_NAME = "MobileUser";
    private static Logger logger = LoggerFactory.getLogger(CommandLineWsClient.class);

    private UUID assignedUUID;
    private String desiredService;
    private AtomicReference<URI> desiredServiceUri = new AtomicReference<>();

    /**
     * Constructs a WebSocketClient instance and sets it to the connect to the
     * specified URI. The channel does not attempt to connect automatically. The connection
     * will be established once you call <var>connect</var>.
     *
     * @param serverUri the server URI to connect to
     */
    public CommandLineWsClient(URI serverUri, String desiredService) {
        super(serverUri);
        this.desiredService = desiredService;
        // todo include some info here to identify the service name to request
    }

    /**
     * Called after an opening handshake has been performed and the given websocket is ready to be written on.
     *
     * @param handshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.debug("Connection opened to server at " + getConnection().getRemoteSocketAddress());

        // todo schedule an action to check the application address -> ScheduledExecutorService from Executors
    }

    /**
     * Callback for string messages received from the remote host
     *
     * @param message The UTF-8 decoded message that was received.
     **/
    @Override
    public void onMessage(String message) {
        logger.debug("Received: " + message);
        RuntimeTypeAdapterFactory<Message> adapter = RuntimeTypeAdapterFactory
                .of(Message.class, "type")
                .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                .registerSubtype(HostRequest.class, Message.MessageTypes.HOST_REQUEST)
                .registerSubtype(HostResponse.class, Message.MessageTypes.HOST_RESPONSE)
                .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST);
        // todo handle Heartbeat Requests here

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();
        Message messageObj = gson.fromJson(message, Message.class);

        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO_REQUEST:
                handleNodeInfoRequest((NodeInfoRequest) messageObj);
                sendNodeInfoResponse();
                requestApplicationHost();
                break;
            case Message.MessageTypes.HOST_RESPONSE:
                handleHostResponse((HostResponse) messageObj);
                // Save the host

                //                HostResponse hostResponse = (HostResponse) messageObj;
//                hostResponse.getServiceHostAddress();
//                try {
//                    nodeSocketController=new NodeSocketController(new URI("wss://"+hostResponse.getServiceHostAddress()));
//                    nodeSocketController.run();
//                } catch (URISyntaxException e) {
//                    e.printStackTrace();
//                }
                break;
        }
    }

    /**
     * Called after the websocket connection has been closed.
     *
     * @param code   The codes can be looked up at: CloseFrame
     * @param reason Additional information string
     * @param remote Returns whether or not the closing of the connection was initiated by the remote host.
     **/
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.debug("Closing the WebSocketClient: ");
        logger.debug("code: " + code);
        logger.debug("reason: " + reason);
        logger.debug("remote: " + remote);
    }

    /**
     * Called when errors occurs. If an error causes the websocket connection to fail {@link #onClose(int, String, boolean)} will be called additionally.<br>
     * This method will be called primarily because of IO or protocol errors.<br>
     * If the given exception is an RuntimeException that probably means that you encountered a bug.<br>
     *
     * @param ex The exception causing this error
     **/
    @Override
    public void onError(Exception ex) {
        logger.error(ex.getMessage());
    }

    public void sendNodeInfoResponse() {
        NodeInfo nodeInfo = new NodeInfo(assignedUUID, null, HEARTBEAT_RESPONSE_SERVICE_NAME);
        String jsonStr = new Gson().toJson(nodeInfo);
        send(jsonStr);
    }

    /**
     * Gets the most recent service {@code URI} allocated to this client by the Orchestrator.
     * <p>
     * There is no guarantee that the URI will be non-null, nor that the URI points to a running service.
     * </p>
     *
     * @return the service's {@code URI} if one has been allocated thusfar, otherwise null.
     */
    public URI getDesiredServiceUri() {
        return desiredServiceUri.get();
    }

    private void setDesiredServiceUri(URI uri) {
        desiredServiceUri.set(uri);
    }

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        logger.debug("Pong received at " + Instant.now());
    }

    public void requestApplicationHost() {
        HostRequest serviceRequest = new HostRequest(assignedUUID, desiredService);
        String jsonStr = new Gson().toJson(serviceRequest);
        send(jsonStr);
    }

    private void handleNodeInfoRequest(NodeInfoRequest request) {
        assignedUUID = request.getAssignedUUID();
    }

    public void handleHostResponse(HostResponse response) {
        setDesiredServiceUri(response.getServiceHostAddress());
    }

    @Override
    public void send(String s) {
        logger.debug(String.valueOf(s));
        super.send(s);
    }
}
