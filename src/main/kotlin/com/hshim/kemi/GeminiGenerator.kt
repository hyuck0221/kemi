package com.hshim.kemi

import com.fasterxml.jackson.databind.ObjectMapper
import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.GeminiRequest
import com.hshim.kemi.model.GeminiResponse
import com.hshim.kemi.model.ImageData
import com.hshim.kemi.model.StreamResponseHandler
import com.hshim.kemi.schema.SchemaGenerator
import com.hshim.kemi.schema.SchemaGenerator.processStreamResponse
import org.springframework.http.*
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import util.ClassUtil.jsonToClass

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

    /**
     * Ask a question with images
     * @param question The question text
     * @param images List of images to include
     * @param prompt Optional system prompt
     * @return Response text or null if failed
     */
    fun askWithImages(
        question: String,
        images: List<ImageData>,
        prompt: String? = null
    ): String? {
        return try {
            val response = directAskWithImages(question, images, currentModel, prompt)
            fallbackCnt = 0
            response?.answer
        } catch (e: Exception) {
            fallback().askWithImages(question, images, prompt)
        }
    }

    /**
     * Direct request with images using specific model and API key
     */
    fun directAskWithImages(
        question: String,
        images: List<ImageData>,
        model: String,
        prompt: String? = null,
        apiKey: String = currentApiKey
    ): GeminiResponse? {
        val url = properties.generateContentUrl(model, apiKey)
        val parts = mutableListOf<GeminiRequest.Part>()
        parts.add(GeminiRequest.Part.text(question))
        parts.addAll(images.map { GeminiRequest.Part.image(it) })

        val requestBody = GeminiRequest(parts, defaultPrompt, prompt)

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        return restTemplate.postForObject(
            url,
            HttpEntity(requestBody, headers),
            GeminiResponse::class.java
        )
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
        val url = properties.generateContentUrl(model, apiKey)
            .replace("generateContent", "streamGenerateContent") + "&alt=sse"
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