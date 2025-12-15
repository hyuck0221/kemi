package com.hshim.kemi

import com.fasterxml.jackson.databind.ObjectMapper
import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.GeminiRequest
import com.hshim.kemi.model.GeminiResponse
import com.hshim.kemi.model.StreamResponseHandler
import com.hshim.kemi.schema.SchemaGenerator
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import util.ClassUtil.jsonToClass
import java.io.BufferedReader

class GeminiGenerator(
    val properties: GeminiProperties,
    private val defaultPrompt: String? = null,
) {
    private val restTemplate = RestTemplate().apply {
        errorHandler = object : DefaultResponseErrorHandler() {
            override fun hasError(statusCode: HttpStatus): Boolean = false
        }
    }
    private val objectMapper = ObjectMapper()
    private var fallbackCnt = 0
    private var modelIdx = 0
    private var apiKeyIdx = 0
    private val totalApiKeys = properties.getAllApiKeys().size

    val currentModel: String
        get() = properties.models[modelIdx]

    val currentApiKey: String
        get() = properties.getApiKey(apiKeyIdx)

    fun directAsk(
        question: String,
        model: String,
        prompt: String? = null,
        apiKey: String = currentApiKey
    ): GeminiResponse? {
        val url = properties.generateContentUrl(model, apiKey)
        val requestBody = GeminiRequest(question, defaultPrompt, prompt)

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        return restTemplate.postForObject(
            url,
            HttpEntity(requestBody, headers),
            GeminiResponse::class.java
        )
    }

    fun ask(question: String, prompt: String? = null): String? {
        return try {
            val response = directAsk(question, currentModel, prompt)
            fallbackCnt = 0
            response?.answer
        } catch (e: Exception) {
            fallback().ask(question, prompt)
        }
    }

    inline fun <reified T : Any> askWithClass(question: String, prompt: String? = null): T? {
        val enhancedPrompt = SchemaGenerator.generatePrompt<T>(question)
        return ask(enhancedPrompt, prompt)
            ?.replaceFirst("```json", "")
            ?.replace("```", "")
            ?.trim()
            ?.jsonToClass()
    }

    /**
     * Stream response in real-time with callback handler
     * @param question The question to ask
     * @param prompt Optional system prompt
     * @param handler Callback function that receives each chunk of text
     * @return Complete concatenated response text
     */
    fun askStream(
        question: String,
        prompt: String? = null,
        handler: StreamResponseHandler
    ): String? {
        return try {
            val response = directAskStream(question, currentModel, prompt, handler)
            fallbackCnt = 0
            response
        } catch (e: Exception) {
            fallback().askStream(question, prompt, handler)
        }
    }

    /**
     * Direct streaming request with specific model and API key
     */
    fun directAskStream(
        question: String,
        model: String,
        prompt: String? = null,
        handler: StreamResponseHandler,
        apiKey: String = currentApiKey
    ): String? {
        val url = properties.generateContentUrl(model, apiKey).replace("generateContent", "streamGenerateContent")
        val requestBody = GeminiRequest(question, defaultPrompt, prompt)

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

    private fun processStreamResponse(response: ClientHttpResponse, handler: StreamResponseHandler): String {
        val fullText = StringBuilder()

        response.body.bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.startsWith("data: ")) {
                    val jsonData = line.substring(6).trim()
                    if (jsonData.isNotEmpty() && jsonData != "[DONE]") {
                        try {
                            val chunk = objectMapper.readValue(jsonData, GeminiResponse::class.java)
                            chunk.answer?.let { text ->
                                handler.onChunk(text)
                                fullText.append(text)
                            }
                        } catch (e: Exception) {
                            // Skip invalid JSON chunks
                        }
                    }
                }
            }
        }

        return fullText.toString()
    }

    fun fallback() = this.apply {
        fallbackCnt++
        val totalCombinations = properties.models.size * totalApiKeys

        if (fallbackCnt >= totalCombinations) {
            throw IllegalStateException(
                "All fallback options exhausted. Tried ${properties.models.size} models with $totalApiKeys API keys."
            )
        }

        if (apiKeyIdx == totalApiKeys - 1) {
            apiKeyIdx = 0
            if (modelIdx == properties.models.lastIndex) modelIdx = 0
            else modelIdx++
        } else apiKeyIdx++
    }
}