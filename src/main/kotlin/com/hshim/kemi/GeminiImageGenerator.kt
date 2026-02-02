package com.hshim.kemi

import com.fasterxml.jackson.databind.ObjectMapper
import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.GeminiImageRequest
import com.hshim.kemi.model.GeminiImageResponse
import org.springframework.http.*
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

/**
 * Gemini Image Generator using Imagen API
 *
 * Example usage:
 * ```kotlin
 * val imageGen = geminiImageGenerator
 * val images = imageGen.generateImage("A beautiful sunset over mountains")
 * images?.forEach { println(it.base64Data) }
 * ```
 */
class GeminiImageGenerator(
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

    // Imagen models
    private val imageModels = listOf(
        "imagen-3.0-generate-001",
        "imagen-3.0-fast-generate-001"
    )

    val currentModel: String
        get() = imageModels[modelIdx]

    val currentApiKey: String
        get() = properties.getApiKey(apiKeyIdx)

    /**
     * Generate images from text prompt
     * @param prompt Text description of the image to generate
     * @param numberOfImages Number of images to generate (1-4)
     * @param aspectRatio Aspect ratio of the image (default: "1:1")
     * @param negativePrompt What to avoid in the image
     * @return List of generated images or null if failed
     */
    fun generateImage(
        prompt: String,
        numberOfImages: Int = 1,
        aspectRatio: String = "1:1",
        negativePrompt: String? = null
    ): List<GeminiImageResponse.Image>? {
        return try {
            val response = directGenerateImage(
                prompt = prompt,
                numberOfImages = numberOfImages,
                aspectRatio = aspectRatio,
                negativePrompt = negativePrompt,
                model = currentModel,
                apiKey = currentApiKey
            )
            fallbackCnt = 0
            response?.images
        } catch (e: Exception) {
            fallback().generateImage(prompt, numberOfImages, aspectRatio, negativePrompt)
        }
    }

    /**
     * Direct image generation with specific model and API key
     */
    fun directGenerateImage(
        prompt: String,
        numberOfImages: Int = 1,
        aspectRatio: String = "1:1",
        negativePrompt: String? = null,
        model: String,
        apiKey: String
    ): GeminiImageResponse? {
        val url = "${properties.baseUrl}/v1/models/$model:predict?key=$apiKey"

        val finalPrompt = if (defaultPrompt != null) {
            "$defaultPrompt\n\n$prompt"
        } else {
            prompt
        }

        val requestBody = GeminiImageRequest(
            prompt = finalPrompt,
            numberOfImages = numberOfImages,
            aspectRatio = aspectRatio,
            negativePrompt = negativePrompt
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        return restTemplate.postForObject(
            url,
            HttpEntity(requestBody, headers),
            GeminiImageResponse::class.java
        )
    }

    private fun fallback() = this.apply {
        fallbackCnt++
        val totalCombinations = imageModels.size * totalApiKeys

        if (fallbackCnt >= totalCombinations) {
            throw IllegalStateException(
                "All fallback options exhausted. Tried ${imageModels.size} models with $totalApiKeys API keys."
            )
        }

        if (apiKeyIdx == totalApiKeys - 1) {
            apiKeyIdx = 0
            if (modelIdx == imageModels.lastIndex) modelIdx = 0
            else modelIdx++
        } else apiKeyIdx++
    }
}
