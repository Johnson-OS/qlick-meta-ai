package dev.qlick.meta.demo;

import dev.qlick.meta.core.MetaApiException;
import dev.qlick.meta.language.MetaLanguageClient;
import dev.qlick.meta.language.MetaLanguageClient.Tool;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class CommerceController {
    private static final Map<String, Object> SEARCH_SCHEMA = Map.of(
            "type", "object", "properties", Map.of("query", Map.of("type", "string", "description", "Product name or search terms")),
            "required", List.of("query"), "additionalProperties", false);
    private static final Map<String, Object> INVENTORY_SCHEMA = Map.of(
            "type", "object", "properties", Map.of("product_id", Map.of("type", "string")),
            "required", List.of("product_id"), "additionalProperties", false);
    private final MetaLanguageClient language;
    private final DemoCatalog catalog;

    CommerceController(MetaLanguageClient language, DemoCatalog catalog) { this.language = language; this.catalog = catalog; }

    @PostMapping("/chat")
    ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank() || request.message().length() > 1000)
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a message up to 1000 characters."));
        boolean availability = request.message().toLowerCase().matches(".*\\b(available|availability|in stock|stock|have)\\b.*");
        List<Tool> tools = List.of(
                new Tool("search_products", "Search the fictional merchant's approved catalog. Use before making product claims.", SEARCH_SCHEMA),
                new Tool("check_inventory", "Read the current authoritative stock count for a catalog product id.", INVENTORY_SCHEMA));
        try {
            Set<String> searchMatches = new HashSet<>();
            var result = language.runTools("You are the helpful shopping assistant for fictional merchant Juniper & Moss Skincare. For product availability questions, call search_products first, then check_inventory for a matching result. Never guess availability, prices, or product links. Use only tool results. The app will show the verified product card separately.",
                    request.message(), tools, (name, args) -> execute(name, args, searchMatches), availability);
            List<Map<String, Object>> verified = result.trace().stream()
                    .filter(call -> call.name().equals("check_inventory"))
                    .map(call -> call.result())
                    .filter(item -> Boolean.TRUE.equals(item.get("ok")))
                    .filter(item -> item.get("product_id") instanceof String id && searchMatches.contains(id))
                    .toList();
            boolean searchedMatch = result.trace().stream()
                    .filter(call -> call.name().equals("search_products"))
                    .anyMatch(call -> call.result().get("products") instanceof List<?> matches && !matches.isEmpty());
            if (availability && (!searchedMatch || verified.isEmpty()))
                throw new MetaApiException(422, "catalog_verification_incomplete", "The assistant could not verify a matching product and inventory record.");
            return ResponseEntity.ok(new ChatResponse(result.answer(), result.trace(), verified));
        } catch (MetaApiException e) {
            int status = e.status() >= 400 && e.status() < 600 ? e.status() : 502;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage(), "code", e.errorType()));
        }
    }

    @GetMapping("/products") List<Map<String, Object>> products() {
        return catalog.search("").stream().map(this::publicCard).toList();
    }

    @GetMapping("/product/{slug}") ResponseEntity<?> product(@PathVariable String slug) {
        return catalog.bySlug(slug).<ResponseEntity<?>>map(p -> ResponseEntity.ok(publicCard(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> execute(String name, Map<String, Object> args, Set<String> searchMatches) {
        if (name.equals("search_products")) {
            Object query = args.get("query");
            if (!(query instanceof String q) || q.isBlank() || q.length() > 120) return Map.of("ok", false, "error", "Invalid search query");
            List<DemoProduct> products = catalog.search(q);
            products.forEach(product -> searchMatches.add(product.id()));
            List<Map<String, Object>> matches = products.stream().map(p -> Map.<String, Object>of(
                    "product_id", p.id(), "name", p.name(), "sku", p.sku())).toList();
            return Map.of("ok", true, "products", matches);
        }
        if (name.equals("check_inventory")) {
            Object rawId = args.get("product_id");
            if (!(rawId instanceof String id)) return Map.of("ok", false, "error", "Invalid product id");
            if (!searchMatches.contains(id)) return Map.of("ok", false, "error", "Search for this product before checking inventory");
            return catalog.byId(id).map(p -> Map.<String, Object>of(
                    "ok", true, "product_id", p.id(), "name", p.name(), "in_stock", p.stock() > 0,
                    "stock_count", p.stock(), "price", p.price(), "product_url", p.url()))
                    .orElse(Map.of("ok", false, "error", "Product not found"));
        }
        return Map.of("ok", false, "error", "Tool not approved");
    }

    private Map<String, Object> publicCard(DemoProduct p) {
        return Map.of("id", p.id(), "sku", p.sku(), "name", p.name(), "description", p.description(),
                "inStock", p.stock() > 0, "stockCount", p.stock(), "price", p.price(), "url", p.url());
    }
    record ChatRequest(String message) {}
    record ChatResponse(String answer, List<MetaLanguageClient.ToolExecution> trace, List<Map<String, Object>> verifiedProducts) {}
}
