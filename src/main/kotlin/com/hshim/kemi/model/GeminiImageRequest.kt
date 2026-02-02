package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Request model for Gemini Imagen API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiImageRequest(
    val instances: List<Instance>,
    val parameters: Parameters
) {
    data class Instance(
        val prompt: String
    )

    data class Parameters(
        val sampleCount: Int = 1,
        val aspectRatio: String = "1:1",
        val negativePrompt: String? = null,
        val safetySetting: String = "block_some",
        val personGeneration: String = "allow_adult"
    )

    constructor(
        prompt: String,
        numberOfImages: Int = 1,
        aspectRatio: String = "1:1",
        negativePrompt: String? = null
    ) : this(
        instances = listOf(Instance(prompt)),
        parameters = Parameters(
            sampleCount = numberOfImages.coerceIn(1, 4),
            aspectRatio = aspectRatio,
            negativePrompt = negativePrompt
        )
    )
}
