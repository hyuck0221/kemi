package com.hshim.kemi.config

import com.hshim.kemi.GeminiGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for GeminiGenerator
 *
 * This configuration automatically creates a GeminiGenerator bean
 * when kemi.gemini.enabled is true (default)
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties::class)
@ConditionalOnProperty(
    prefix = "kemi.gemini",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class GeminiAutoConfiguration {

    /**
     * Creates a GeminiGenerator bean using properties from application.yml
     */
    @Bean
    @ConditionalOnMissingBean
    fun geminiGenerator(properties: GeminiProperties): GeminiGenerator {
        return GeminiGenerator(properties)
    }
}
