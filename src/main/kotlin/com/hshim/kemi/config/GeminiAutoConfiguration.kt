package com.hshim.kemi.config

import com.hshim.kemi.GeminiChatGenerator
import com.hshim.kemi.GeminiGenerator
import com.hshim.kemi.GeminiImageGenerator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration for Gemini API generators
 *
 * This configuration automatically creates GeminiGenerator, GeminiChatGenerator,
 * and GeminiImageGenerator beans when kemi.gemini.enabled is true (default)
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties::class)
@ConditionalOnProperty(
    prefix = "kemi.gemini",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
open class GeminiAutoConfiguration {

    /**
     * Creates a GeminiGenerator bean using properties from application.yml
     */
    @Bean
    @ConditionalOnMissingBean
    open fun geminiGenerator(properties: GeminiProperties): GeminiGenerator {
        return GeminiGenerator(properties)
    }

    /**
     * Creates a GeminiChatGenerator bean for conversational AI interactions
     */
    @Bean
    @ConditionalOnMissingBean
    open fun geminiChatGenerator(properties: GeminiProperties): GeminiChatGenerator {
        return GeminiChatGenerator(properties)
    }

    /**
     * Creates a GeminiImageGenerator bean for image generation using Imagen API
     */
    @Bean
    @ConditionalOnMissingBean
    open fun geminiImageGenerator(properties: GeminiProperties): GeminiImageGenerator {
        return GeminiImageGenerator(properties)
    }
}
