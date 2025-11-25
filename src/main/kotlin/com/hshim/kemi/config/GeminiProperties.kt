package com.hshim.kemi.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kemi.gemini")
data class GeminiProperties(
    /**
     * Gemini API base URL
     * @default https://generativelanguage.googleapis.com
     */
    val baseUrl: String = "https://generativelanguage.googleapis.com",

    /**
     * Gemini API Key
     */
    val apiKey: String,

    /**
     * List of Gemini model names to use
     * @default [gemini-pro]
     */
    val models: List<String> = listOf("gemini-2.5-pro")
) {
    /**
     * Generate final API endpoint URL for content generation
     */
    fun generateContentUrl(model: String): String =
        "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"
}