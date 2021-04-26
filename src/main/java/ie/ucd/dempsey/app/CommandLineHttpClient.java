package ie.ucd.dempsey.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
        logger.debug("Running.");
        addData();
        sendData();
    }

    public String getData() {
        HttpRequest request = HttpRequest.newBuilder(getUri())
                .GET()
                .build();
        HttpResponse<String> response = executeHttpRequest(request);
        return isNull(response) ? "GET Response is null!" : response.body();
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
        boolean canAccessService = serviceAccessible();

        if (nonNull(serviceUri) && canAccessService) {
            sendPairsOverHttp();
            pairs.clear();
        } else {
            if (isNull(serviceUri)) logger.warn("In sendData, serviceUri is null.");
            if (!canAccessService) logger.warn("In sendData, serviceAccessible returns false.");
        }
    }

    private boolean serviceAccessible() {
        logger.debug("Checking if service is accessible.");
        HttpRequest request = HttpRequest.newBuilder(dryRunUri())
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();
        HttpResponse<String> response = executeHttpRequest(request);

        return !isNull(response) && response.statusCode() == 200;
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
        processPostResponse(response);
    }

    private void processPostResponse(HttpResponse<String> response) {
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
        } catch (IOException | InterruptedException ex) {
            logger.error("Exception while executing Http Request.", ex);
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
