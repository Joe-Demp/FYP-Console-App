package ie.ucd.dempsey.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class PingServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(PingServer.class);

    public PingServer(InetSocketAddress address) {
        super(address);
        logger.debug("Starting ping server on {}", address);
    }

    @Override
    public void onWebsocketPing(WebSocket connection, Framedata framedata) {
        logger.debug("ping from {}", connection.getRemoteSocketAddress());
        super.onWebsocketPing(connection, framedata);
    }

    // no overrides necessary
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
    }

    @Override
    public void onStart() {
    }
}
