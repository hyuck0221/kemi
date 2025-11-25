package com.hshim.kemi.model

data class GeminiResponse(
    val candidates: List<Candidates>,
) {
    data class Candidates(
        val content: Content,
    ) {
        data class Content(
            val parts: List<Part>,
        )
    }

    data class Part(
        val text: String,
    )

    val answer: String?
        get() = candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
}