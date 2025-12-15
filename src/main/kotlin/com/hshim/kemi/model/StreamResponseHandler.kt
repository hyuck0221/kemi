package com.hshim.kemi.model

/**
 * Handler for streaming responses from Gemini API
 */
fun interface StreamResponseHandler {
    /**
     * Called for each chunk of the streaming response
     * @param chunk The text chunk received from the API
     */
    fun onChunk(chunk: String)
}
