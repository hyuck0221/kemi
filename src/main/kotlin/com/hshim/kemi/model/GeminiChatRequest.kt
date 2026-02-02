package com.hshim.kemi.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Request model for Gemini Chat API with conversation history
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GeminiChatRequest(
    val systemInstruction: SystemInstruction?,
    val contents: List<Content>,
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class SystemInstruction(
        val parts: List<Part>,
    ) {
        constructor(defaultPrompt: String?, prompt: String?) : this(
            parts = listOfNotNull(defaultPrompt?.let { Part.text(it) }, prompt?.let { Part.text(it) }),
        )
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Content(
        val role: String,
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

    companion object {
        fun fromHistory(
            history: List<ChatMessage>,
            defaultPrompt: String?,
            prompt: String?
        ): GeminiChatRequest {
            val systemInstruction = if (defaultPrompt != null || prompt != null) {
                SystemInstruction(defaultPrompt, prompt)
            } else null

            val contents = history.map { message ->
                Content(
                    role = when (message.role) {
                        ChatMessage.Role.USER -> "user"
                        ChatMessage.Role.MODEL -> "model"
                    },
                    parts = message.parts
                )
            }

            return GeminiChatRequest(systemInstruction, contents)
        }
    }
}
