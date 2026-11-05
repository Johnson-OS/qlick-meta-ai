package dev.qlick.meta.image;

import dev.qlick.meta.core.MetaApiClient;
import java.util.Map;

/** Documented one-shot Muse Image generation and edit operations. */
public final class MetaImageClient {
    private final MetaApiClient api;
    private final String model;
    public MetaImageClient(MetaApiClient api, String model) { this.api = api; this.model = model; }
    public Map<String, Object> generate(String prompt) {
        if (prompt == null || prompt.isBlank() || prompt.length() > 4000) throw new IllegalArgumentException("prompt must be 1..4000 characters");
        return api.post("images/generations", Map.of("model", model, "prompt", prompt, "n", 1));
    }
    public Map<String, Object> edit(String prompt, String imageUrlOrDataUrl) {
        if (prompt == null || prompt.isBlank() || imageUrlOrDataUrl == null || imageUrlOrDataUrl.isBlank()) throw new IllegalArgumentException("prompt and image are required");
        return api.post("images/edits", Map.of("model", model, "prompt", prompt, "images", java.util.List.of(Map.of("image_url", imageUrlOrDataUrl)), "n", 1));
    }
}
