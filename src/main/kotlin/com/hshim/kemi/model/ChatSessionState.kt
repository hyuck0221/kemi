package com.hshim.kemi.model

/**
 * Serializable state of a chat session
 * Can be saved and restored to persist conversations
 */
data class ChatSessionState(
    val apiKeys: List<String>,
    val models: List<String>,
    val history: List<ChatMessage>,
    val defaultPrompt: String?,
    val systemPrompt: String?,
    val baseUrl: String
)
