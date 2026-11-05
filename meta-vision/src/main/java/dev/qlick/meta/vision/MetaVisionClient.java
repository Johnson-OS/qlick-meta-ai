package dev.qlick.meta.vision;

import dev.qlick.meta.core.MetaApiClient;
import java.util.List;
import java.util.Map;

/** Muse Spark image understanding using documented multimodal chat content. */
public final class MetaVisionClient {
    private final MetaApiClient api;
    private final String model;
    public MetaVisionClient(MetaApiClient api, String model) { this.api = api; this.model = model; }
    public Map<String, Object> understand(String question, String imageUrlOrDataUrl) {
        if (question == null || question.isBlank() || imageUrlOrDataUrl == null || imageUrlOrDataUrl.isBlank()) throw new IllegalArgumentException("question and image are required");
        Map<String, Object> content = Map.of("role", "user", "content", List.of(
                Map.of("type", "text", "text", question),
                Map.of("type", "image_url", "image_url", Map.of("url", imageUrlOrDataUrl))));
        return api.post("chat/completions", Map.of("model", model, "messages", List.of(content)));
    }
}
