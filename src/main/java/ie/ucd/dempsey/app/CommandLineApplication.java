package ie.ucd.dempsey.app;

import org.java_websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineApplication implements Runnable {
    private static final char PROMPT_CHAR = '\u00bb';
    private static final Logger logger = LoggerFactory.getLogger(CommandLineApplication.class);

    private final WebSocketClient wsClient;

    public CommandLineApplication(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    private static void prompt() {
        System.out.print(" " + PROMPT_CHAR + " ");
    }

    private static void write(String s) {

    }

    @Override
    public void run() {
        prompt();
        prompt();
        prompt();

        /*
        event loop

        info - write current application information
        ping?
        application requests - basically use
         */
    }
}
