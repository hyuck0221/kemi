package com.hshim.kemi.model

/**
 * Represents a single message in a chat conversation
 */
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role {
        USER,
        MODEL
    }
}
