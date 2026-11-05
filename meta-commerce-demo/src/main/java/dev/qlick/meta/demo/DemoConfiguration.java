package dev.qlick.meta.demo;

import dev.qlick.meta.core.MetaApiClient;
import dev.qlick.meta.core.MetaApiProperties;
import dev.qlick.meta.core.MetaUsageReporter;
import dev.qlick.meta.language.MetaLanguageClient;
import java.net.URI;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

@Configuration
class DemoConfiguration {
    @Bean MetaApiClient metaApiClient(ObjectMapper mapper, MetaUsageReporter usageReporter) {
        String key = System.getenv().getOrDefault("MODEL_API_KEY", "");
        URI base = URI.create(System.getenv().getOrDefault("META_MODEL_API_BASE_URL", "https://api.meta.ai/v1"));
        return new MetaApiClient(new MetaApiProperties(base, key, Duration.ofSeconds(60), 3), mapper, usageReporter);
    }
    @Bean MetaLanguageClient metaLanguageClient(MetaApiClient api) {
        return new MetaLanguageClient(api, System.getenv().getOrDefault("META_LANGUAGE_MODEL", "muse-spark-1.3"));
    }
    @Bean MetaUsageReporter usageReporter() {
        var log = LoggerFactory.getLogger("dev.qlick.meta.usage");
        return usage -> log.info("Meta request usage model={} tokens={}", usage.model(), usage.usage().get("total_tokens"));
    }
}
