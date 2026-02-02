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
     * Gemini API Key (single key - deprecated, use apiKeys instead)
     */
    val apiKey: String? = null,

    /**
     * List of Gemini API Keys for fallback support (required: at least 1 key)
     */
    val apiKeys: List<String> = emptyList(),

    /**
     * List of Gemini model names to use
     * @default [gemini-pro]
     */
    val models: List<String> = listOf("gemini-2.5-pro"),

    /**
     * List of Gemini image generation model names
     * @default [gemini-2.5-flash-image]
     */
    val imageModels: List<String> = listOf("gemini-2.5-flash-image", "gemini-3-pro-image-preview")
) {
    /**
     * API Key provider (can be set programmatically for callback support)
     */
    @Transient
    var apiKeyProvider: (() -> String)? = null

    /**
     * Get all configured API Keys
     */
    fun getAllApiKeys(): List<String> {
        val keys = apiKeyProvider?.let { listOf(it.invoke()) }
            ?: apiKeys.takeIf { it.isNotEmpty() }
            ?: apiKey?.let { listOf(it) }
            ?: emptyList()

        if (keys.isEmpty()) {
            throw IllegalStateException(
                "At least one API key must be configured."
            )
        }

        return keys
    }

    /**
     * Get API Key by index
     */
    fun getApiKey(index: Int = 0): String {
        val keys = getAllApiKeys()
        return keys.getOrNull(index) ?: keys.first()
    }

    /**
     * Generate final API endpoint URL with custom API key
     */
    fun generateContentUrl(model: String, apiKey: String): String =
        "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"
}