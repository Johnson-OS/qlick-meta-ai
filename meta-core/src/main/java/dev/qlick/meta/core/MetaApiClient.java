package dev.qlick.meta.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Small transport for Meta's documented OpenAI-compatible HTTP API. Never logs request bodies or credentials. */
public final class MetaApiClient {
    private static final Logger log = LoggerFactory.getLogger(MetaApiClient.class);
    private final MetaApiProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final MetaUsageReporter usageReporter;

    public MetaApiClient(MetaApiProperties properties, ObjectMapper mapper, MetaUsageReporter usageReporter) {
        this.properties = properties;
        this.mapper = mapper;
        this.usageReporter = usageReporter == null ? ignored -> {} : usageReporter;
        this.http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    public Map<String, Object> post(String path, Map<String, Object> body) {
        if (properties.apiKey().isBlank()) throw new MetaApiException(503, "missing_credentials", "Set MODEL_API_KEY on the server to enable live Meta requests.");
        final String json;
        try { json = mapper.writeValueAsString(body); }
        catch (JacksonException e) { throw new IllegalArgumentException("Could not encode Meta request", e); }
        HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        for (int attempt = 0; ; attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    Map<String, Object> decoded = decode(response.body());
                    reportUsage(decoded);
                    return decoded;
                }
                MetaApiException failure = apiFailure(response.statusCode(), response.body());
                if (!failure.retryable() || attempt >= properties.maxRetries()) throw failure;
                pause(attempt);
            } catch (IOException e) {
                if (attempt >= properties.maxRetries()) {
                    log.warn("Meta API transport failed after {} retries", attempt);
                    throw new MetaApiException(0, "transport_error", "Meta API request failed after retries");
                }
                pause(attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MetaApiException(0, "interrupted", "Meta API request was interrupted");
            }
        }
    }

    /** Reads a server-sent event response incrementally and passes each JSON data event to the caller. */
    public void stream(String path, Map<String, Object> body, Consumer<Map<String, Object>> onEvent) {
        if (properties.apiKey().isBlank()) throw new MetaApiException(503, "missing_credentials", "Set MODEL_API_KEY on the server to enable live Meta requests.");
        HttpRequest request = HttpRequest.newBuilder(resolve(path))
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(encodeJson(body)))
                .build();
        for (int attempt = 0; ; attempt++) {
            try {
                HttpResponse<Stream<String>> response = http.send(request, HttpResponse.BodyHandlers.ofLines());
                try (Stream<String> lines = response.body()) {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        MetaApiException failure = apiFailure(response.statusCode(), String.join("\n", lines.toList()));
                        if (!failure.retryable() || attempt >= properties.maxRetries()) throw failure;
                        pause(attempt);
                        continue;
                    }
                    var iterator = lines.iterator();
                    while (iterator.hasNext()) {
                        String line = iterator.next();
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if (data.equals("[DONE]")) return;
                        Map<String, Object> event = decode(data);
                        reportUsage(event);
                        onEvent.accept(event);
                    }
                    return;
                }
            } catch (IOException e) {
                if (attempt >= properties.maxRetries()) {
                    log.warn("Meta API stream failed after {} retries", attempt);
                    throw new MetaApiException(0, "transport_error", "Meta API stream failed after retries");
                }
                pause(attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MetaApiException(0, "interrupted", "Meta API stream was interrupted");
            }
        }
    }

    /** Serializes tool results without flattening nested maps or lists into strings. */
    public String encodeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not encode JSON value", e);
        }
    }

    private void reportUsage(Map<String, Object> event) {
        Object usage = event.get("usage");
        if (usage instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked") Map<String, Object> safe = (Map<String, Object>) map;
            Object model = event.get("model");
            try {
                usageReporter.report(new MetaUsage(model instanceof String s ? s : "unknown", safe));
            } catch (RuntimeException failure) {
                log.warn("Meta usage reporter failed", failure);
            }
        }
    }

    private URI resolve(String path) {
        String base = properties.baseUri().toString().replaceAll("/+$", "");
        return URI.create(base + "/" + path.replaceAll("^/+", ""));
    }

    private Map<String, Object> decode(String body) {
        try { return mapper.readValue(body, Map.class); }
        catch (JacksonException e) { throw new MetaApiException(502, "invalid_response", "Meta API returned invalid JSON"); }
    }

    private MetaApiException apiFailure(int status, String body) {
        String type = "api_error";
        String message = "Meta API returned HTTP " + status;
        try {
            Map<String, Object> root = decode(body);
            if (root.get("error") instanceof Map<?, ?> error) {
                if (error.get("type") instanceof String s) type = s;
                // Provider messages are passed through to callers, but never logged.
                if (error.get("message") instanceof String s && !s.isBlank()) message = s;
            }
        } catch (RuntimeException ignored) { /* Keep the safe status-only message. */ }
        return new MetaApiException(status, type, message);
    }

    private static void pause(int attempt) {
        long cap = Math.min(8_000L, 400L * (1L << attempt));
        try { Thread.sleep(ThreadLocalRandom.current().nextLong(Math.max(1, cap / 2), cap + 1)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new MetaApiException(0, "interrupted", "Meta API retry interrupted"); }
    }
}
