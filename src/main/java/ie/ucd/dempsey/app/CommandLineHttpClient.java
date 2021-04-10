package ie.ucd.dempsey.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Objects.*;

public class CommandLineHttpClient implements Runnable {
    private static final int DATA_PER_CYCLE = 10;
    private static final Logger logger = LoggerFactory.getLogger(CommandLineHttpClient.class);

    private final AtomicReference<URI> cloudService;
    private final Deque<Pair<Instant, Double>> pairs = new LinkedList<>();
    private final Random random = new Random();
    private HttpClient httpClient;

    public CommandLineHttpClient(AtomicReference<URI> cloudService, HttpClient httpClient) {
        this.cloudService = cloudService;
        this.httpClient = httpClient;
    }

    private static void waitASecond() {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            logger.error("Interrupted during a 1 second sleep.", e);
        }
    }

    @Override
    public void run() {
        addData();
        sendData();
    }

    public String getData() {
        HttpRequest request = HttpRequest.newBuilder(getUri())
                .GET()
                .build();
        HttpResponse<String> response = executeHttpRequest(request);
        return responseBodyAsString(response);
    }

    private String responseBodyAsString(HttpResponse<String> response) {
        requireNonNull(response, "GET Response is null!");
        return response.body();
    }

    private void addData() {
        for (int i = 0; i < DATA_PER_CYCLE; i++) {
            Pair<Instant, Double> pair = new Pair<>(Instant.now(), random.nextDouble());
            pairs.add(pair);
        }
        waitASecond();
    }

    private void sendData() {
        URI serviceUri = cloudService.get();
        if (nonNull(serviceUri) && serviceAccessible()) {
            sendPairsOverHttp();
            pairs.clear();
        }
    }

    private boolean serviceAccessible() {
        HttpRequest request = HttpRequest.newBuilder(dryRunUri())
                .GET()
                .build();
        HttpResponse<String> response = executeHttpRequest(request);
        return requireNonNull(response, "HEAD response was null!").statusCode() == 200;
    }

    private URI dryRunUri() {
        URI service = cloudService.get();
        URI resolved = service.resolve("/dryrun");
        logger.debug("DR URIs {} {}", service, resolved);
        return resolved;
    }

    private void sendPairsOverHttp() {
        String data = pairsToString();
        HttpRequest request = makePostRequest(data);
        HttpResponse<String> response = executeHttpRequest(request);
        processResponse(response);
    }

    private void processResponse(HttpResponse<String> response) {
        if (isNull(response)) {
            logger.warn("HttpResponse is null!");
        } else {
            logger.info("Http: {}", response);
        }
    }

    private HttpRequest makePostRequest(String data) {
        return HttpRequest.newBuilder(postUri())
                .POST(ofString(data))
                .build();
    }

    private HttpResponse<String> executeHttpRequest(HttpRequest request) {
        try {
            return httpClient.send(request, ofString());
        } catch (IOException | InterruptedException ioe) {
            logger.error("Exception while executing Http Request.", ioe);
        }
        return null;
    }

    private URI postUri() {
        URI service = cloudService.get();
        URI post = service.resolve("/submit");

        logger.debug("URIs {} {}", service, post);
        return post;
    }

    private URI getUri() {
        URI service = cloudService.get();
        URI resolved = service.resolve("/");
        logger.debug("GET URIs {} {}", service, resolved);
        return resolved;
    }

    private String pairsToString() {
        return pairs.stream()
                .map(p -> new StringBuilder().append(p.getFirst()).append(' ').append(p.getSecond()).append('\n'))
                .reduce(new StringBuilder(), StringBuilder::append)
                .toString();
    }
}
