package com.hshim.kemi

import com.fasterxml.jackson.databind.ObjectMapper
import com.hshim.kemi.config.GeminiProperties
import com.hshim.kemi.model.GeminiImageRequest
import com.hshim.kemi.model.GeminiImageResponse
import com.hshim.kemi.model.ImageData
import org.springframework.http.*
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate

/**
 * Gemini Image Generator using Gemini 2.5 Flash Image model
 *
 * Example usage:
 * ```kotlin
 * val imageGen = geminiImageGenerator
 * val images = imageGen.generateImage("A beautiful sunset over mountains")
 * images.forEach { println(it.base64Data) }
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

    val currentModel: String
        get() = properties.imageModels[modelIdx]

    val currentApiKey: String
        get() = properties.getApiKey(apiKeyIdx)

    /**
     * Generate image from text prompt
     * @param prompt Text description of the image to generate
     * @param aspectRatio Aspect ratio of the image (default: "1:1")
     *                    Options: 1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9
     * @param imageSize Image resolution (default: "2K")
     *                  Options: 1K, 2K, 4K
     * @param referenceImages Optional reference images for style/content guidance
     * @return List of generated images or empty list if failed
     */
    fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        imageSize: String = "2K",
        referenceImages: List<ImageData>? = null
    ): List<GeminiImageResponse.Image> {
        return try {
            val response = directGenerateImage(
                prompt = prompt,
                aspectRatio = aspectRatio,
                imageSize = imageSize,
                referenceImages = referenceImages,
                model = currentModel,
                apiKey = currentApiKey
            )
            fallbackCnt = 0
            response?.images ?: emptyList()
        } catch (e: Exception) {
            fallback().generateImage(prompt, aspectRatio, imageSize, referenceImages)
        }
    }

    /**
     * Direct image generation with specific model and API key
     */
    fun directGenerateImage(
        prompt: String,
        aspectRatio: String = "1:1",
        imageSize: String = "2K",
        referenceImages: List<ImageData>? = null,
        model: String,
        apiKey: String
    ): GeminiImageResponse? {
        val url = "${properties.baseUrl}/v1beta/models/$model:generateContent"

        val finalPrompt = if (defaultPrompt != null) {
            "$defaultPrompt\n\n$prompt"
        } else {
            prompt
        }

        val requestBody = GeminiImageRequest.create(
            prompt = finalPrompt,
            aspectRatio = aspectRatio,
            imageSize = imageSize,
            referenceImages = referenceImages
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-goog-api-key", apiKey)
        }

        return restTemplate.postForObject(
            url,
            HttpEntity(requestBody, headers),
            GeminiImageResponse::class.java
        )
    }

    private fun fallback() = this.apply {
        fallbackCnt++
        val totalCombinations = properties.imageModels.size * totalApiKeys

        if (fallbackCnt >= totalCombinations) {
            throw IllegalStateException(
                "All fallback options exhausted. Tried ${properties.imageModels.size} models with $totalApiKeys API keys."
            )
        }

        if (apiKeyIdx == totalApiKeys - 1) {
            apiKeyIdx = 0
            if (modelIdx == properties.imageModels.lastIndex) modelIdx = 0
            else modelIdx++
        } else apiKeyIdx++
    }
}
