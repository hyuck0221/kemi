package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response model for Gemini Image Generation API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiImageResponse(
    val candidates: List<Candidate>?
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Candidate(
        val content: Content?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Content(
        val parts: List<Part>?
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Part(
        val text: String? = null,
        val inlineData: InlineData? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class InlineData(
        @JsonProperty("mimeType")
        val mimeType: String?,
        @JsonProperty("data")
        val data: String? // base64 encoded
    )

    /**
     * Extract all generated images from the response
     */
    val images: List<Image>
        get() = candidates
            ?.flatMap { it.content?.parts ?: emptyList() }
            ?.mapNotNull { part ->
                part.inlineData?.let { inlineData ->
                    if (inlineData.mimeType != null && inlineData.data != null) {
                        Image(inlineData.data, inlineData.mimeType)
                    } else null
                }
            } ?: emptyList()

    /**
     * Extract text description from the response
     */
    val description: String?
        get() = candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull { it.text != null }
            ?.text

    data class Image(
        val base64Data: String,
        val mimeType: String
    )
}
