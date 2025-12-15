package com.hshim.kemi.config

import com.hshim.kemi.GeminiChatGenerator
import com.hshim.kemi.GeminiGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for Gemini API generators
 *
 * This configuration automatically creates GeminiGenerator and GeminiChatGenerator beans
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

    /**
     * Creates a GeminiChatGenerator bean for conversational AI interactions
     */
    @Bean
    @ConditionalOnMissingBean
    fun geminiChatGenerator(properties: GeminiProperties): GeminiChatGenerator {
        return GeminiChatGenerator(properties)
    }
}
