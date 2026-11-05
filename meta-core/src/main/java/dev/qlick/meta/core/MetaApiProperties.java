package dev.qlick.meta.core;

import java.net.URI;
import java.time.Duration;

/** Configuration for the documented Meta Model API. */
public record MetaApiProperties(
        URI baseUri,
        String apiKey,
        Duration timeout,
        int maxRetries) {
    public MetaApiProperties {
        if (baseUri == null || baseUri.getHost() == null || baseUri.getRawUserInfo() != null
                || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null
                || !("https".equalsIgnoreCase(baseUri.getScheme())
                        || ("http".equalsIgnoreCase(baseUri.getScheme()) && isLoopback(baseUri.getHost()))))
            throw new IllegalArgumentException("Meta Model API base URI must use HTTPS (HTTP is allowed for loopback tests)");
        apiKey = apiKey == null ? "" : apiKey.trim();
        if (timeout == null || timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("timeout must be positive");
        if (maxRetries < 0 || maxRetries > 5) throw new IllegalArgumentException("maxRetries must be between 0 and 5");
    }

    public static MetaApiProperties fromEnvironment() {
        return new MetaApiProperties(
                URI.create(System.getenv().getOrDefault("META_MODEL_API_BASE_URL", "https://api.meta.ai/v1")),
                System.getenv("MODEL_API_KEY"),
                Duration.ofSeconds(60),
                3);
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }
}
