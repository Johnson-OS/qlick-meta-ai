package dev.qlick.meta.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sun.net.httpserver.HttpServer;
import dev.qlick.meta.core.MetaApiClient;
import dev.qlick.meta.core.MetaApiProperties;
import dev.qlick.meta.language.MetaLanguageClient;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

class CommerceControllerTest {
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void searchesThenChecksMatchedInventoryAndReturnsVerifiedProductLink() throws Exception {
        AtomicInteger request = new AtomicInteger();
        startServer(request, false);
        CommerceController controller = controller();

        ResponseEntity<?> response = controller.chat(new CommerceController.ChatRequest(
                "Is the Sage + Oat Barrier Serum available?"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CommerceController.ChatResponse body = assertInstanceOf(CommerceController.ChatResponse.class, response.getBody());
        assertEquals("Yes, it is available.", body.answer());
        assertEquals(2, body.trace().size());
        assertEquals("search_products", body.trace().getFirst().name());
        assertEquals("check_inventory", body.trace().getLast().name());
        assertEquals("/api/product/sage-oat-barrier-serum", body.verifiedProducts().getFirst().get("product_url"));
        assertEquals("$28.00", body.verifiedProducts().getFirst().get("price"));
    }

    @Test
    void refusesAvailabilityWhenModelSkipsProductSearch() throws Exception {
        AtomicInteger request = new AtomicInteger();
        startServer(request, true);
        CommerceController controller = controller();

        ResponseEntity<?> response = controller.chat(new CommerceController.ChatRequest(
                "Is the Sage + Oat Barrier Serum available?"));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, response.getStatusCode());
    }

    private CommerceController controller() {
        ObjectMapper mapper = new ObjectMapper();
        MetaApiClient api = new MetaApiClient(
                new MetaApiProperties(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"),
                        "test-key", Duration.ofSeconds(2), 0),
                mapper,
                ignored -> {});
        return new CommerceController(new MetaLanguageClient(api, "test-model"), new DemoCatalog());
    }

    private void startServer(AtomicInteger request, boolean skipSearch) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            int turn = request.getAndIncrement();
            String body;
            if (turn == 0) {
                String name = skipSearch ? "check_inventory" : "search_products";
                String arguments = skipSearch
                        ? "{\"product_id\":\"p-001\"}"
                        : "{\"query\":\"Sage + Oat Barrier Serum\"}";
                body = toolCall("call-1", name, arguments);
            } else if (turn == 1 && !skipSearch) {
                body = toolCall("call-2", "check_inventory", "{\"product_id\":\"p-001\"}");
            } else {
                body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Yes, it is available.\"}}]}";
            }
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    private static String toolCall(String id, String name, String arguments) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String encodedArgs = mapper.writeValueAsString(arguments);
            return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"id\":\""
                    + id + "\",\"type\":\"function\",\"function\":{\"name\":\"" + name
                    + "\",\"arguments\":" + encodedArgs + "}}]}}]}";
        } catch (RuntimeException e) {
            throw new IllegalStateException(e);
        }
    }
}
