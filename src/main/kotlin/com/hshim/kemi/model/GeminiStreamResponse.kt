package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeminiStreamResponse(
    val candidates: List<Candidate>? = null,
    val usageMetadata: UsageMetadata? = null,
    val modelVersion: String? = null,
    val responseId: String? = null
) {
    val text: String?
        get() = candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Candidate(
        val content: Content? = null,
        val finishReason: String? = null,
        val index: Int = 0
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Content(
        val parts: List<Part>? = null,
        val role: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Part(
        val text: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class UsageMetadata(
        val promptTokenCount: Int? = null,
        val candidatesTokenCount: Int? = null,
        val totalTokenCount: Int? = null,
        val promptTokensDetails: List<TokenDetail>? = null,
        val thoughtsTokenCount: Int? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TokenDetail(
        val modality: String? = null,
        val tokenCount: Int? = null
    )
}
