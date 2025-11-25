package com.hshim.kemi.annotation

/**
 * Annotation to define additional prompt context for AI model
 *
 * @property value Additional instructions or context for the AI model
 *
 * Example:
 * ```kotlin
 * @Prompt("Create a detailed user profile with accurate information")
 * data class User(...)
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GeminiPrompt(val value: String)
