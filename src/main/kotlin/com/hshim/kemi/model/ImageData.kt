package com.hshim.kemi.model

import java.io.File
import java.util.*

/**
 * Represents image data that can be sent to Gemini API
 */
data class ImageData(
    val mimeType: String,
    val data: String // base64 encoded
) {
    companion object {
        /**
         * Create ImageData from a file
         */
        fun fromFile(file: File): ImageData {
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> throw IllegalArgumentException("Unsupported image format: ${file.extension}")
            }
            val bytes = file.readBytes()
            val base64 = Base64.getEncoder().encodeToString(bytes)
            return ImageData(mimeType, base64)
        }

        /**
         * Create ImageData from a file path
         */
        fun fromPath(path: String): ImageData {
            return fromFile(File(path))
        }

        /**
         * Create ImageData from byte array
         */
        fun fromBytes(bytes: ByteArray, mimeType: String = "image/jpeg"): ImageData {
            val base64 = Base64.getEncoder().encodeToString(bytes)
            return ImageData(mimeType, base64)
        }

        /**
         * Create ImageData from base64 string
         */
        fun fromBase64(base64: String, mimeType: String = "image/jpeg"): ImageData {
            return ImageData(mimeType, base64)
        }
    }
}
