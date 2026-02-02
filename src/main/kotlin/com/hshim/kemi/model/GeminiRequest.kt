package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiRequest(
    val systemInstruction: SystemInstruction,
    val contents: List<Content>,
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class SystemInstruction(
        val parts: List<Part>,
    ) {
        constructor(defaultPrompt: String?, prompt: String?): this (
            parts = listOfNotNull(defaultPrompt?.let { Part.text(it) }, prompt?.let { Part.text(it) }),
        )
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Content(
        val parts: List<Part>,
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

        companion object {
            fun text(content: String) = Part(text = content)
            fun image(imageData: ImageData) = Part(
                inlineData = InlineData(imageData.mimeType, imageData.data)
            )
        }
    }

    constructor(question: String, defaultPrompt: String?, prompt: String?) : this(
        systemInstruction = SystemInstruction(defaultPrompt, prompt),
        contents = listOf(Content(listOf(Part.text(question)))),
    )

    constructor(
        parts: List<Part>,
        defaultPrompt: String?,
        prompt: String?
    ) : this(
        systemInstruction = SystemInstruction(defaultPrompt, prompt),
        contents = listOf(Content(parts)),
    )
}