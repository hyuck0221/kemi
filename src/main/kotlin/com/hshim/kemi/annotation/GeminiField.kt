package com.hshim.kemi.annotation

/**
 * Annotation to describe a field's purpose and constraints for AI model
 *
 * @property description Human-readable description of the field
 *
 * Example:
 * ```kotlin
 * data class User(
 *     @Field("Full name of the user")
 *     val name: String,
 *
 *     @Field("Age in years, must be positive")
 *     val age: Int
 * )
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class GeminiField(val description: String)
