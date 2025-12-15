package com.hshim.kemi

import com.fasterxml.jackson.databind.ObjectMapper
import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.*
import com.hshim.kemi.schema.SchemaGenerator.processStreamResponse
import org.springframework.http.*
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

/**
 * Chat-based Gemini API generator with conversation history
 *
 * Example usage:
 * ```kotlin
 * val chat = geminiChatGenerator.createSession()
 * chat.sendMessage("Hello, who are you?")
 * chat.sendMessage("What did I just ask you?") // AI remembers context
 * ```
 */
class GeminiChatGenerator(
    val properties: GeminiProperties,
    private val defaultPrompt: String? = null,
) {
    private val restTemplate = RestTemplate().apply {
        errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean = false
        }
    }
    private val objectMapper = ObjectMapper()

    /**
     * Create a new chat session with optional custom configuration
     */
    fun createSession(
        systemPrompt: String? = null,
        apiKeys: List<String>? = null,
        models: List<String>? = null
    ): ChatSession {
        return ChatSession(
            restTemplate = restTemplate,
            objectMapper = objectMapper,
            baseUrl = properties.baseUrl,
            apiKeys = apiKeys ?: properties.getAllApiKeys(),
            models = models ?: properties.models,
            defaultPrompt = defaultPrompt,
            systemPrompt = systemPrompt,
            history = mutableListOf()
        )
    }

    /**
     * Restore a chat session from saved state
     */
    fun restoreSession(state: ChatSessionState): ChatSession {
        return ChatSession(
            restTemplate = restTemplate,
            objectMapper = objectMapper,
            baseUrl = state.baseUrl,
            apiKeys = state.apiKeys,
            models = state.models,
            defaultPrompt = state.defaultPrompt,
            systemPrompt = state.systemPrompt,
            history = state.history.toMutableList()
        )
    }

    /**
     * Chat session that maintains conversation history
     */
    class ChatSession(
        private val restTemplate: RestTemplate,
        private val objectMapper: ObjectMapper,
        private val baseUrl: String,
        private val apiKeys: List<String>,
        private val models: List<String>,
        private val defaultPrompt: String?,
        private val systemPrompt: String?,
        private val history: MutableList<ChatMessage>
    ) {
        private var fallbackCnt = 0
        private var modelIdx = 0
        private var apiKeyIdx = 0
        private val totalApiKeys = apiKeys.size

        val currentModel: String
            get() = models[modelIdx]

        val currentApiKey: String
            get() = apiKeys[apiKeyIdx]

        /**
         * Get conversation history
         */
        fun getHistory(): List<ChatMessage> = history.toList()

        /**
         * Export session state for persistence
         */
        fun exportSession(): ChatSessionState {
            return ChatSessionState(
                apiKeys = apiKeys,
                models = models,
                history = history.toList(),
                defaultPrompt = defaultPrompt,
                systemPrompt = systemPrompt,
                baseUrl = baseUrl
            )
        }

        /**
         * Clear conversation history
         */
        fun clearHistory() {
            history.clear()
            resetFallback()
        }

        /**
         * Send a message and get response
         */
        fun sendMessage(message: String): String? {
            // Add user message to history
            history.add(ChatMessage(ChatMessage.Role.USER, message))

            return try {
                val response = directSendMessage()
                fallbackCnt = 0

                // Add model response to history
                response?.answer?.let { answer ->
                    history.add(ChatMessage(ChatMessage.Role.MODEL, answer))
                    answer
                }
            } catch (e: Exception) {
                // Remove last user message if failed
                history.removeLastOrNull()
                fallback().sendMessage(message)
            }
        }

        /**
         * Send message with streaming response
         * @param message User message
         * @param handler Callback for each chunk of response
         * @return Complete response text
         */
        fun sendMessageStream(message: String, handler: StreamResponseHandler): String? {
            // Add user message to history
            history.add(ChatMessage(ChatMessage.Role.USER, message))

            return try {
                val response = directSendMessageStream(handler)
                fallbackCnt = 0

                // Add complete model response to history
                response?.also { answer ->
                    history.add(ChatMessage(ChatMessage.Role.MODEL, answer))
                }
            } catch (e: Exception) {
                // Remove last user message if failed
                history.removeLastOrNull()
                fallback().sendMessageStream(message, handler)
            }
        }

        /**
         * Send message with custom API key (does not trigger fallback)
         */
        fun sendMessage(message: String, customApiKey: String): String? {
            history.add(ChatMessage(ChatMessage.Role.USER, message))

            val url = generateContentUrl(currentModel, customApiKey)
            val requestBody = GeminiChatRequest.fromHistory(history, defaultPrompt, systemPrompt)

            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val response = restTemplate.postForObject(
                url,
                HttpEntity(requestBody, headers),
                GeminiResponse::class.java
            )

            return response?.answer?.also { answer ->
                history.add(ChatMessage(ChatMessage.Role.MODEL, answer))
            }
        }

        private fun directSendMessage(): GeminiResponse? {
            val url = generateContentUrl(currentModel, currentApiKey)
            val requestBody = GeminiChatRequest.fromHistory(history, defaultPrompt, systemPrompt)

            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            return restTemplate.postForObject(
                url,
                HttpEntity(requestBody, headers),
                GeminiResponse::class.java
            )
        }

        private fun directSendMessageStream(handler: StreamResponseHandler): String? {
            val url = generateContentUrl(currentModel, currentApiKey)
                .replace("generateContent", "streamGenerateContent") + "&alt=sse"
            val requestBody = GeminiChatRequest.fromHistory(history, defaultPrompt, systemPrompt)

            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }

            return restTemplate.execute(
                url,
                HttpMethod.POST,
                { request ->
                    request.headers.putAll(headers)
                    objectMapper.writeValue(request.body, requestBody)
                },
                { response: ClientHttpResponse ->
                    processStreamResponse(response, handler)
                }
            )
        }

        private fun generateContentUrl(model: String, apiKey: String): String =
            "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"

        private fun fallback() = this.apply {
            fallbackCnt++
            val totalCombinations = models.size * totalApiKeys

            if (fallbackCnt >= totalCombinations) {
                throw IllegalStateException(
                    "All fallback options exhausted. Tried ${models.size} models with $totalApiKeys API keys."
                )
            }

            if (apiKeyIdx == totalApiKeys - 1) {
                apiKeyIdx = 0
                if (modelIdx == models.lastIndex) modelIdx = 0
                else modelIdx++
            } else apiKeyIdx++
        }

        private fun resetFallback() {
            fallbackCnt = 0
            modelIdx = 0
            apiKeyIdx = 0
        }
    }
}
