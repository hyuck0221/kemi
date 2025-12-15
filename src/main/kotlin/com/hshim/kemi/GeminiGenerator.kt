package com.hshim.kemi

import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.GeminiRequest
import com.hshim.kemi.model.GeminiResponse
import com.hshim.kemi.schema.SchemaGenerator
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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