package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Request model for Gemini Image Generation API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiImageRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Content(
        val parts: List<Part>
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Part(
        val text: String? = null,
        val inlineData: InlineData? = null
    ) {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        data class InlineData(
            val mimeType: String,
            val data: String // base64 encoded
        )
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class GenerationConfig(
        val responseModalities: List<String>,
        val imageConfig: ImageConfig? = null
    )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ImageConfig(
        val aspectRatio: String = "1:1",
        val imageSize: String = "2K"
    )

    companion object {
        /**
         * Create image generation request
         * @param prompt Text description of the image
         * @param aspectRatio Image aspect ratio (1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9)
         * @param imageSize Image resolution (1K, 2K, 4K)
         * @param referenceImages Optional reference images for style/content guidance
         */
        fun create(
            prompt: String,
            aspectRatio: String = "1:1",
            imageSize: String = "2K",
            referenceImages: List<ImageData>? = null
        ): GeminiImageRequest {
            val parts = mutableListOf<Part>()
            parts.add(Part(text = prompt))

            referenceImages?.forEach { image ->
                parts.add(Part(
                    inlineData = Part.InlineData(
                        mimeType = image.mimeType,
                        data = image.data
                    )
                ))
            }

            return GeminiImageRequest(
                contents = listOf(Content(parts)),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("TEXT", "IMAGE"),
                    imageConfig = ImageConfig(aspectRatio, imageSize)
                )
            )
        }
    }
}
