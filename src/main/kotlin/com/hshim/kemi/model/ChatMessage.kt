package com.hshim.kemi.model

/**
 * Represents a single message in a chat conversation
 */
data class ChatMessage(
    val role: Role,
    val parts: List<GeminiChatRequest.Part>
) {
    enum class Role {
        USER,
        MODEL
    }

    val content: String
        get() = parts.mapNotNull { it.text }.joinToString(" ")

    companion object {
        /**
         * Create a text-only message
         */
        fun text(role: Role, content: String): ChatMessage {
            return ChatMessage(role, listOf(GeminiChatRequest.Part.text(content)))
        }

        /**
         * Create a message with text and images
         */
        fun withImages(role: Role, text: String, images: List<ImageData>): ChatMessage {
            val parts = mutableListOf<GeminiChatRequest.Part>()
            parts.add(GeminiChatRequest.Part.text(text))
            parts.addAll(images.map { GeminiChatRequest.Part.image(it) })
            return ChatMessage(role, parts)
        }

        /**
         * Create a message with custom parts
         */
        fun withParts(role: Role, parts: List<GeminiChatRequest.Part>): ChatMessage {
            return ChatMessage(role, parts)
        }
    }
}
