package ie.ucd.dempsey.websocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.core.*;

import java.net.URI;

// todo remove copied javadocs

public class CommandLineWsClient extends WebSocketClient {
    private static Logger logger = LoggerFactory.getLogger(CommandLineWsClient.class);

    /**
     * Constructs a WebSocketClient instance and sets it to the connect to the
     * specified URI. The channel does not attempt to connect automatically. The connection
     * will be established once you call <var>connect</var>.
     *
     * @param serverUri the server URI to connect to
     */
    public CommandLineWsClient(URI serverUri) {
        super(serverUri);

        // todo include some info here to identify the service name to request
    }

    /**
     * Called after an opening handshake has been performed and the given websocket is ready to be written on.
     *
     * @param handshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("Connection opened to server at " + getConnection().getRemoteSocketAddress());
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

        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();
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
}
