package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response model for Gemini Imagen API
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiImageResponse(
    val predictions: List<Prediction>?
) {
    val images: List<Image>?
        get() = predictions?.mapNotNull { it.image }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Prediction(
        @JsonProperty("bytesBase64Encoded")
        val bytesBase64Encoded: String?,
        @JsonProperty("mimeType")
        val mimeType: String?
    ) {
        val image: Image?
            get() = if (bytesBase64Encoded != null && mimeType != null) {
                Image(bytesBase64Encoded, mimeType)
            } else null
    }

    data class Image(
        val base64Data: String,
        val mimeType: String
    )
}
