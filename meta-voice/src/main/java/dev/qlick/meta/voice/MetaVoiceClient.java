package dev.qlick.meta.voice;

import dev.qlick.meta.core.MetaApiException;
import dev.qlick.meta.core.MetaApiProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** File transcription via the documented Muse Voice Transcribe multipart endpoint. */
public final class MetaVoiceClient {
    private static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024;
    private final MetaApiProperties properties;
    private final ObjectMapper mapper;
    private final String model;
    public MetaVoiceClient(MetaApiProperties properties, ObjectMapper mapper) {
        this(properties, mapper, "muse-voice-transcribe-1.0");
    }
    public MetaVoiceClient(MetaApiProperties properties, ObjectMapper mapper, String model) {
        this.properties = properties;
        this.mapper = mapper;
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
        this.model = model;
    }

    public Map<String, Object> transcribe(String filename, String audioEncoding, byte[] audio, String mode) {
        if (audio == null || audio.length == 0 || audio.length > MAX_AUDIO_BYTES) throw new IllegalArgumentException("audio must be between 1 byte and 25 MiB");
        if (audioEncoding == null || !audioEncoding.matches("WAV|MP3|M4A|FLAC|OGG|WEBM")) throw new IllegalArgumentException("unsupported audio encoding");
        if (mode == null || !mode.matches("PUSH_TO_TALK|ENDPOINTING|DIARIZATION")) throw new IllegalArgumentException("unsupported transcription mode");
        String boundary = "----MetaBoundary" + UUID.randomUUID().toString().replace("-", "");
        String settings;
        try {
            settings = mapper.writeValueAsString(Map.of("mode", mode, "model", model, "audioEncoding", audioEncoding));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Could not encode transcription settings", e);
        }
        byte[] body = multipart(boundary, filename, settings, audio);
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUri().toString().replaceAll("/+$", "") + "/asr/transcribe"))
                .timeout(Duration.ofMinutes(3)).header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        if (properties.apiKey().isBlank()) throw new MetaApiException(503, "missing_credentials", "Set MODEL_API_KEY on the server to enable transcription.");
        try {
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new MetaApiException(response.statusCode(), "transcription_error", "Meta transcription request failed.");
            return mapper.readValue(response.body(), Map.class);
        } catch (IOException e) { throw new MetaApiException(502, "transport_error", "Meta transcription request failed."); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new MetaApiException(0, "interrupted", "Transcription request was interrupted."); }
        catch (JacksonException e) { throw new MetaApiException(502, "invalid_response", "Meta transcription response was invalid."); }
    }

    private static byte[] multipart(String boundary, String filename, String settings, byte[] audio) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            part(out, boundary, "request", "application/json", settings);
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"" + sanitizeFilename(filename) + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes());
            out.write(audio); out.write(("\r\n--" + boundary + "--\r\n").getBytes()); return out.toByteArray();
        } catch (IOException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void part(ByteArrayOutputStream out, String boundary, String name, String type, String value) throws IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\nContent-Type: " + type + "\r\n\r\n" + value + "\r\n").getBytes());
    }
    private static String sanitizeFilename(String value) { return value == null ? "audio" : value.replaceAll("[^A-Za-z0-9._-]", "_"); }
}
