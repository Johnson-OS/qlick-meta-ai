package dev.qlick.meta.autoconfigure;

import dev.qlick.meta.core.MetaApiClient;
import dev.qlick.meta.core.MetaApiProperties;
import dev.qlick.meta.core.MetaUsageReporter;
import dev.qlick.meta.image.MetaImageClient;
import dev.qlick.meta.language.MetaLanguageClient;
import dev.qlick.meta.segmentation.MetaSegmentationClient;
import dev.qlick.meta.vision.MetaVisionClient;
import dev.qlick.meta.voice.MetaVoiceClient;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(MetaModelProperties.class)
public class MetaModelAutoConfiguration {
    @Bean @ConditionalOnMissingBean MetaUsageReporter metaUsageReporter() { return ignored -> {}; }
    @Bean @ConditionalOnMissingBean MetaApiProperties metaApiProperties(MetaModelProperties properties) {
        return new MetaApiProperties(URI.create(properties.getBaseUrl()), properties.getApiKey(),
                Duration.ofSeconds(properties.getTimeoutSeconds()), properties.getMaxRetries());
    }
    @Bean @ConditionalOnMissingBean MetaApiClient metaApiClient(MetaApiProperties properties, ObjectMapper mapper, MetaUsageReporter reporter) {
        return new MetaApiClient(properties, mapper, reporter);
    }
    @Bean @ConditionalOnMissingBean MetaLanguageClient metaLanguageClient(MetaApiClient api, MetaModelProperties properties) {
        return new MetaLanguageClient(api, properties.getLanguageModel());
    }
    @Bean @ConditionalOnMissingBean MetaImageClient metaImageClient(MetaApiClient api, MetaModelProperties properties) {
        return new MetaImageClient(api, properties.getImageModel());
    }
    @Bean @ConditionalOnMissingBean MetaVoiceClient metaVoiceClient(MetaApiProperties properties, ObjectMapper mapper, MetaModelProperties settings) {
        return new MetaVoiceClient(properties, mapper, settings.getVoiceModel());
    }
    @Bean @ConditionalOnMissingBean MetaVisionClient metaVisionClient(MetaApiClient api, MetaModelProperties properties) {
        return new MetaVisionClient(api, properties.getVisionModel());
    }
    @Bean @ConditionalOnMissingBean MetaSegmentationClient metaSegmentationClient(MetaApiClient api, MetaModelProperties properties) {
        return new MetaSegmentationClient(api, properties.getSegmentationModel());
    }
}
