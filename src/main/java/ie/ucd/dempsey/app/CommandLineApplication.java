package ie.ucd.dempsey.app;

import ie.ucd.dempsey.websocket.CommandLineWsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Scanner;

public class CommandLineApplication {
    private static final char PROMPT_CHAR = '\u00bb';
    private static final Logger logger = LoggerFactory.getLogger(CommandLineApplication.class);
    private static final Scanner scanner = new Scanner(System.in);
    private static final String INPUT_REQUEST;

    static {
        StringBuilder sb = new StringBuilder("Please enter a command (");
        Command[] commands = Command.values();

        // first command
        sb.append(' ').append(commands[0].getName());

        // all subsequent commands
        for (int i = 1; i < commands.length; i++) {
            sb.append(" | ").append(commands[i].getName());
        }
        sb.append(" )");

        INPUT_REQUEST = sb.toString();
    }

    private final CommandLineWsClient wsClient;
    private boolean quitRequested = false;

    public CommandLineApplication(CommandLineWsClient wsClient) {
        this.wsClient = wsClient;
    }

    private static void prompt() {
        System.out.print(" " + PROMPT_CHAR + " ");
    }

    private static void requestInput() {
        System.out.println();
        System.out.println(INPUT_REQUEST);
        prompt();
    }

    private static String getAndSanitizeInput() {
        String rawInput = scanner.nextLine();
        return rawInput.strip().toLowerCase();
    }

    public void start() {
        while (!quitRequested) {
            requestInput();
            String input = getAndSanitizeInput();
            Command command = Command.toCommand(input);

            handleCommand(command);
        }
    }

    private void handleCommand(Command command) {
        if (command == null) {
            printError();
        } else {
            switch (command) {
                case INFO:
                    printInfo();
                    break;
                case PING:
                    ping();
                    break;
                case HELP:
                    printHelp();
                    break;
                case QUIT:
                    printQuit();
                    quitRequested = true;
                    break;
                default:
                    printError();
                    break;
            }
        }
    }

    private void printInfo() {
        System.out.println("orchestrator address: " + wsClient.getRemoteSocketAddress());
        System.out.println("application instance address: " + wsClient.getCloudService());
        System.out.println("client UUID: " + wsClient.getAssignedUUID());
        System.out.println();
    }

    private void printError() {
        System.out.printf("\nAn error occurred: %s", "incorrect input");
    }

    private void printQuit() {
        System.out.println();
        System.out.println("Application is stopping");
        System.out.println();
    }

    private void printHelp() {
        System.out.println("Help message not yet implemented.");
    }

    private void ping() {
        String msg = "Sending ping at: " + Instant.now();
        System.out.println(msg);
        logger.debug(msg);
        wsClient.sendPing();
    }

    private enum Command {
        INFO("info"),
        PING("ping"),
        HELP("help"),
        QUIT("quit");

        private String name;

        Command(String name) {
            this.name = name;
        }

        public static Command toCommand(String s) {
            for (Command c : Command.values()) {
                if (c.name.equals(s)) {
                    return c;
                }
            }
            return QUIT;
        }

        public String getName() {
            return this.name;
        }
    }
}
