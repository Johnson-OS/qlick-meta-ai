package dev.qlick.meta.segmentation;

import dev.qlick.meta.core.MetaApiClient;
import java.util.List;
import java.util.Map;

/** SAM 3.1 still-image segmentation. The raw output text uses Meta's documented SAM token grammar. */
public final class MetaSegmentationClient {
    private final MetaApiClient api;
    private final String model;
    public MetaSegmentationClient(MetaApiClient api) { this(api, "sam-3.1"); }
    public MetaSegmentationClient(MetaApiClient api, String model) {
        this.api = api;
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
        this.model = model;
    }
    public Map<String, Object> segmentImage(String objectPrompt, String imageUrlOrDataUrl) {
        if (objectPrompt == null || objectPrompt.isBlank() || objectPrompt.length() > 100) throw new IllegalArgumentException("Use a short object phrase");
        if (imageUrlOrDataUrl == null || imageUrlOrDataUrl.isBlank()) throw new IllegalArgumentException("image is required");
        return api.post("responses", Map.of("model", model, "input", List.of(Map.of(
                "type", "message", "role", "user", "content", List.of(
                        Map.of("type", "input_text", "text", objectPrompt),
                        Map.of("type", "input_image", "image_url", imageUrlOrDataUrl))))));
    }
}
