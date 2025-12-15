package com.hshim.kemi.model

/**
 * Request model for Gemini Chat API with conversation history
 */
data class GeminiChatRequest(
    val systemInstruction: SystemInstruction?,
    val contents: List<Content>,
) {
    data class SystemInstruction(
        val parts: List<Part>,
    ) {
        constructor(defaultPrompt: String?, prompt: String?) : this(
            parts = listOfNotNull(defaultPrompt?.let { Part(it) }, prompt?.let { Part(it) }),
        )
    }

    data class Content(
        val role: String,
        val parts: List<Part>,
    )

    data class Part(
        val text: String
    )

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
                    parts = listOf(Part(message.content))
                )
            }

            return GeminiChatRequest(systemInstruction, contents)
        }
    }
}
