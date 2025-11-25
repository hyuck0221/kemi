package com.hshim.kemi.schema

import com.hshim.kemi.annotation.GeminiField
import com.hshim.kemi.annotation.GeminiPrompt
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Generates JSON schema description from Kotlin data classes
 * with annotation support for AI model guidance
 */
object SchemaGenerator {

    /**
     * Generate enhanced prompt with schema information
     *
     * @param T Target type for deserialization
     * @param userPrompt User's original prompt
     * @return Enhanced prompt with schema and field descriptions
     */
    inline fun <reified T : Any> generatePrompt(userPrompt: String): String {
        return generatePrompt(T::class, userPrompt)
    }

    /**
     * Generate enhanced prompt with schema information
     *
     * @param kClass Target class for schema generation
     * @param userPrompt User's original prompt
     * @return Enhanced prompt with schema and field descriptions
     */
    fun <T : Any> generatePrompt(kClass: KClass<T>, userPrompt: String): String {
        val classPrompt = kClass.findAnnotation<GeminiPrompt>()?.value ?: ""
        val schema = generateJsonSchema(kClass)

        return buildString {
            appendLine(userPrompt)
            if (classPrompt.isNotEmpty()) {
                appendLine()
                appendLine(classPrompt)
            }
            appendLine()
            appendLine("Respond in this exact JSON format:")
            appendLine(schema)
        }.trim()
    }

    /**
     * Generate JSON schema with field descriptions
     *
     * @param kClass Target class for schema generation
     * @return JSON schema string with field descriptions
     */
    fun <T : Any> generateJsonSchema(kClass: KClass<T>): String {
        val properties = kClass.memberProperties

        return buildString {
            appendLine("{")
            properties.forEachIndexed { index, prop ->
                val fieldDesc = prop.findAnnotation<GeminiField>()?.description
                val typeName = getTypeName(prop.returnType.toString())

                append("  \"${prop.name}\": ")
                if (fieldDesc != null) {
                    appendLine("\"$typeName\", // $fieldDesc")
                } else {
                    appendLine("\"$typeName\"")
                }

                if (index < properties.size - 1) {
                    // Remove last character if it's a newline, then add comma
                }
            }
            append("}")
        }
    }

    /**
     * Extract simple type name from fully qualified type string
     */
    private fun getTypeName(fullType: String): String {
        // kotlin.String -> String
        // kotlin.Int -> Int
        // kotlin.collections.List<kotlin.String> -> List<String>
        return fullType
            .replace("kotlin.", "")
            .replace("java.lang.", "")
            .substringAfter(".")
            .replace("?", "")
    }
}
