package dev.qlick.meta.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import dev.qlick.meta.core.MetaApiClient;
import dev.qlick.meta.core.MetaApiException;
import dev.qlick.meta.core.MetaApiProperties;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MetaLanguageClientTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void executesApprovedToolAndReturnsFinalAnswer() throws Exception {
        AtomicInteger request = new AtomicInteger();
        AtomicReference<String> toolTurn = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            int current = request.getAndIncrement();
            if (current == 1) toolTurn.set(new String(requestBody));
            String response = current == 0
                    ? "{\"model\":\"test-model\",\"usage\":{\"total_tokens\":5},\"choices\":[{\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"lookup\",\"arguments\":\"{\\\"query\\\":\\\"serum\\\"}\"}}]},\"finish_reason\":\"tool_calls\"}]}"
                    : "{\"model\":\"test-model\",\"usage\":{\"total_tokens\":7},\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Verified: available.\"},\"finish_reason\":\"stop\"}]}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
        var api = new MetaApiClient(new MetaApiProperties(URI.create(base), "test-key", Duration.ofSeconds(2), 0), new ObjectMapper(), ignored -> {});
        var client = new MetaLanguageClient(api, "test-model");
        var tool = new MetaLanguageClient.Tool("lookup", "look up a product", Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")), "required", List.of("query"), "additionalProperties", false));
        var result = client.runTools("system", "is serum available", List.of(tool),
                (name, args) -> Map.of("items", List.of(Map.of("id", "p-1"))), true);
        assertEquals("Verified: available.", result.answer());
        assertEquals("lookup", result.trace().getFirst().name());
        assertEquals(2, request.get());
        Map<?, ?> sent = new ObjectMapper().readValue(toolTurn.get(), Map.class);
        List<?> messages = (List<?>) sent.get("messages");
        Map<?, ?> toolMessage = (Map<?, ?>) messages.getLast();
        assertEquals("{\"items\":[{\"id\":\"p-1\"}]}", toolMessage.get("content"));
    }

    @Test void failsWhenRequiredToolWasSkipped() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] response = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Sure\"}}]}".getBytes();
            exchange.sendResponseHeaders(200, response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        var api = new MetaApiClient(new MetaApiProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"), "test", Duration.ofSeconds(2), 0), new ObjectMapper(), ignored -> {});
        var client = new MetaLanguageClient(api, "test-model");
        var tool = new MetaLanguageClient.Tool("lookup", "look up a product", Map.of("type", "object"));
        assertThrows(MetaApiException.class, () -> client.runTools("system", "is it in stock", List.of(tool), (n, a) -> Map.of(), true));
    }

    @Test void emitsTextDeltasFromMetaServerSentEvents() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] events = ("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\" world\"}}]}\n\n"
                    + "data: [DONE]\n\n").getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(events);
            exchange.close();
        });
        server.start();
        var api = new MetaApiClient(
                new MetaApiProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
                        "test", Duration.ofSeconds(2), 0),
                new ObjectMapper(),
                ignored -> {});
        var client = new MetaLanguageClient(api, "test-model");
        List<String> tokens = new ArrayList<>();

        client.stream("Say hello", tokens::add);

        assertEquals(List.of("Hello", " world"), tokens);
    }
}
