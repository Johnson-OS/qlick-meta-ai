package dev.qlick.meta.language;

import dev.qlick.meta.core.MetaApiClient;
import dev.qlick.meta.core.MetaApiException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/** Muse Spark chat, schema constrained output, and bounded function tool execution. */
public final class MetaLanguageClient {
    private final MetaApiClient api;
    private final String model;
    private final int maxToolRounds;

    public MetaLanguageClient(MetaApiClient api, String model) { this(api, model, 4); }

    public MetaLanguageClient(MetaApiClient api, String model, int maxToolRounds) {
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
        if (maxToolRounds < 1 || maxToolRounds > 8) throw new IllegalArgumentException("maxToolRounds must be 1..8");
        this.api = api;
        this.model = model;
        this.maxToolRounds = maxToolRounds;
    }

    public Map<String, Object> structured(String instruction, Map<String, Object> jsonSchema) {
        Map<String, Object> format = Map.of("type", "json_schema", "json_schema", Map.of(
                "name", "meta_structured_response", "strict", true, "schema", jsonSchema));
        return api.post("chat/completions", Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", instruction)),
                "response_format", format));
    }

    /** Streams text deltas from Meta's documented Chat Completions SSE response. */
    public void stream(String instruction, Consumer<String> onToken) {
        Objects.requireNonNull(onToken, "onToken");
        api.stream("chat/completions", Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", instruction)),
                "stream", true),
                event -> {
                    Map<String, Object> choice = firstMap(event.get("choices"));
                    Map<String, Object> delta = asMap(choice.get("delta"));
                    Object content = delta.get("content");
                    if (content instanceof String token && !token.isEmpty()) onToken.accept(token);
                });
    }

    public AgentResult runTools(String system, String prompt, List<Tool> tools,
            BiFunction<String, Map<String, Object>, Map<String, Object>> executor,
            boolean requireToolCall) {
        Objects.requireNonNull(executor, "executor");
        if (tools == null || tools.isEmpty() || tools.size() > 32) {
            throw new IllegalArgumentException("tools must contain between 1 and 32 entries");
        }
        Set<String> names = new HashSet<>();
        if (tools.stream().anyMatch(tool -> !names.add(tool.name()))) {
            throw new IllegalArgumentException("tool names must be unique");
        }
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        messages.add(Map.of("role", "user", "content", prompt));
        List<ToolExecution> trace = new ArrayList<>();
        List<Map<String, Object>> wireTools = tools.stream().map(Tool::asWire).toList();
        for (int round = 0; round < maxToolRounds; round++) {
            Map<String, Object> response = api.post("chat/completions", Map.of(
                    "model", model, "messages", messages, "tools", wireTools,
                    "tool_choice", "auto", "parallel_tool_calls", false));
            Map<String, Object> choice = firstMap(response.get("choices"));
            Map<String, Object> message = asMap(choice.get("message"));
            List<?> calls = message.get("tool_calls") instanceof List<?> list ? list : List.of();
            if (calls.isEmpty()) {
                if (requireToolCall && trace.isEmpty()) throw new MetaApiException(422, "tool_not_called", "The assistant did not verify the request with the catalog tool.");
                Object content = message.get("content");
                return new AgentResult(content instanceof String s ? s : "", List.copyOf(trace), response.get("usage"));
            }
            if (trace.size() + calls.size() > 12) throw new MetaApiException(422, "tool_limit", "The assistant exceeded the allowed tool call limit.");
            // Preserve the exact assistant tool-call turn for the provider's follow-up protocol.
            messages.add(message);
            for (Object rawCall : calls) {
                Map<String, Object> call = asMap(rawCall);
                String id = string(call.get("id"));
                Map<String, Object> function = asMap(call.get("function"));
                String name = string(function.get("name"));
                Map<String, Object> arguments = parseObject(string(function.get("arguments")));
                Tool selected = tools.stream().filter(tool -> tool.name().equals(name)).findFirst()
                        .orElseThrow(() -> new MetaApiException(422, "unknown_tool", "The assistant requested an unapproved tool."));
                Map<String, Object> result;
                try { result = executor.apply(name, arguments); }
                catch (RuntimeException failure) { result = Map.of("ok", false, "error", "Tool execution failed safely."); }
                trace.add(new ToolExecution(name, arguments, result));
                messages.add(Map.of("role", "tool", "tool_call_id", id, "content", api.encodeJson(result)));
            }
        }
        throw new MetaApiException(422, "tool_round_limit", "The assistant did not complete within the tool round limit.");
    }

    private static Map<String, Object> parseObject(String json) {
        // Tool args are parsed by Jackson upstream in MetaApiClient responses; keep malformed calls fail closed.
        try {
            tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (RuntimeException e) { throw new MetaApiException(422, "invalid_tool_arguments", "The assistant returned invalid tool arguments."); }
    }
    @SuppressWarnings("unchecked") private static Map<String, Object> asMap(Object value) { return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of(); }
    private static Map<String, Object> firstMap(Object value) { return value instanceof List<?> list && !list.isEmpty() ? asMap(list.getFirst()) : Map.of(); }
    private static String string(Object value) { return value instanceof String s ? s : ""; }

    public record Tool(String name, String description, Map<String, Object> parameters) {
        public Tool {
            if (name == null || !name.matches("[a-zA-Z0-9_-]{1,64}")) {
                throw new IllegalArgumentException("tool name must use 1..64 letters, digits, underscores or hyphens");
            }
            if (description == null || description.isBlank() || description.length() > 512) {
                throw new IllegalArgumentException("tool description must contain 1..512 characters");
            }
            Objects.requireNonNull(parameters, "parameters");
            if (!"object".equals(parameters.get("type"))) {
                throw new IllegalArgumentException("tool parameters must be a JSON object schema");
            }
        }
        Map<String, Object> asWire() { return Map.of("type", "function", "function", Map.of("name", name, "description", description, "parameters", parameters, "strict", true)); }
    }
    public record ToolExecution(String name, Map<String, Object> arguments, Map<String, Object> result) {}
    public record AgentResult(String answer, List<ToolExecution> trace, Object usage) {}
}
