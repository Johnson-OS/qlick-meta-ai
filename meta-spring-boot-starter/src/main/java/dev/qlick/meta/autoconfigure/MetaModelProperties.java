package dev.qlick.meta.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meta.model-api")
public class MetaModelProperties {
    private String baseUrl = "https://api.meta.ai/v1";
    private String apiKey = "";
    private String languageModel = "muse-spark-1.3";
    private String imageModel = "muse-image-1.0";
    private String voiceModel = "muse-voice-transcribe-1.0";
    private String visionModel = "muse-spark-1.3";
    private String segmentationModel = "sam-3.1";
    private int timeoutSeconds = 60;
    private int maxRetries = 3;
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getLanguageModel() { return languageModel; }
    public void setLanguageModel(String languageModel) { this.languageModel = languageModel; }
    public String getImageModel() { return imageModel; }
    public void setImageModel(String imageModel) { this.imageModel = imageModel; }
    public String getVoiceModel() { return voiceModel; }
    public void setVoiceModel(String voiceModel) { this.voiceModel = voiceModel; }
    public String getVisionModel() { return visionModel; }
    public void setVisionModel(String visionModel) { this.visionModel = visionModel; }
    public String getSegmentationModel() { return segmentationModel; }
    public void setSegmentationModel(String segmentationModel) { this.segmentationModel = segmentationModel; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
}
