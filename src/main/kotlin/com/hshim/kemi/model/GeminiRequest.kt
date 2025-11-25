package com.hshim.kemi.model

data class GeminiRequest(
    val systemInstruction: SystemInstruction,
    val contents: List<Content>,
) {
    data class SystemInstruction(
        val parts: List<Part>,
    ) {
        constructor(defaultPrompt: String?, prompt: String?): this (
            parts = listOfNotNull(defaultPrompt?.let { Part(it) }, prompt?.let { Part(it) }),
        )
    }

    data class Content(
        val parts: List<Part>,
    )

    data class Part(
        val text: String
    )

    constructor(question: String, defaultPrompt: String?, prompt: String?) : this(
        systemInstruction = SystemInstruction(defaultPrompt, prompt),
        contents = listOf(Content(listOf(Part(question)))),
    )
}