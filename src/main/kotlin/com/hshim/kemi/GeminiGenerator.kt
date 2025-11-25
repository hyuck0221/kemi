package com.hshim.kemi

import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.GeminiRequest
import com.hshim.kemi.model.GeminiResponse
import com.hshim.kemi.schema.SchemaGenerator
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
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
            override fun hasError(statusCode: HttpStatusCode): Boolean = false
        }
    }
    private var fallbackCnt = 0
    private var modelIdx = 0
    val currentModel: String
        get() = properties.models[modelIdx]

    fun directAsk(question: String, model: String, prompt: String? = null): GeminiResponse? {

        val url = properties.generateContentUrl(model)
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
            val response = directAsk(question, currentModel)
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
        if (fallbackCnt >= properties.models.size) {
            throw IllegalStateException("There is no model to fallback. All ${properties.models.size} models exhausted.")
        }
        if (modelIdx == properties.models.lastIndex) modelIdx = 0 else modelIdx++
    }
}