# Kemi - Gemini API for Spring Boot

A lightweight Spring Boot library for integrating Google's Gemini API with automatic model fallback and type-safe configuration.

[한국어 문서](README_KR.md)

## Features

- ✅ Spring Boot 3.1+ support
- ✅ Auto Configuration
- ✅ Type-safe Configuration Properties
- ✅ IDE auto-completion support
- ✅ Automatic model fallback
- ✅ Flexible API key configuration (direct value or callback)
- ✅ Structured response mapping to data classes
- ✅ Image generation with Gemini 2.5 Flash Image model
- ✅ Vision support (image input in questions and chat)

## Requirements

- **JVM**: 8+
- **Kotlin**: 1.9+
- **Spring Boot**: 2.7+
- **Spring Framework**: 5.3+

## Installation

### Add JitPack Repository

#### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.hyuck0221:kemi:0.1.0")
}
```

#### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.hyuck0221:kemi:0.1.0'
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.hyuck0221</groupId>
        <artifactId>kemi</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

## Configuration

### application.yml

```yaml
kemi:
  gemini:
    api-keys:  # Required: At least one API key must be provided
      - your-first-api-key
      - your-second-api-key
      - your-third-api-key
    base-url: https://generativelanguage.googleapis.com  # Optional
    models:
      - gemini-2.5-flash
      - gemini-2.5-flash-lite
      - gemini-2.0-flash
      - gemini-2.0-flash-lite
    image-models:  # Optional: Image generation models
      - gemini-2.5-flash-image
      - gemini-3-pro-image-preview
```

### API Key Configuration Options

#### Option 1: Multiple API Keys (Recommended)

```yaml
kemi:
  gemini:
    api-keys:  # Required
      - your-first-api-key
      - your-second-api-key
```

#### Option 2: Callback Function (Programmatic)

```kotlin
import com.hshim.kemi.config.GeminiProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GeminiConfig(
    private val geminiProperties: GeminiProperties
) {
    @Bean
    fun configureApiKeyProvider() {
        // Example: Load API key from external service or secret manager
        geminiProperties.apiKeyProvider = {
            // Your custom logic to retrieve API key
            System.getenv("GEMINI_API_KEY") ?: "fallback-key"
        }
    }
}
```

## Usage

### Basic Usage

```kotlin
import com.hshim.kemi.GeminiGenerator
import org.springframework.stereotype.Service

@Service
class MyService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateResponse() {
        val answer = geminiGenerator.ask("Hello, how are you?")
        println(answer)
    }
}
```

### Streaming Response (Real-time)

Receive responses in real-time as they're generated:

```kotlin
@Service
class StreamingService(
    private val geminiGenerator: GeminiGenerator
) {
    fun streamResponse() {
        val fullResponse = geminiGenerator.askStream(
            question = "Write a long story about a robot",
            handler = { chunk ->
                // Called for each chunk as it arrives
                print(chunk)  // Print immediately as it streams
            }
        )

        println("\n\nComplete response: $fullResponse")
    }
}
```

### Custom Prompt

```kotlin
@Service
class PromptService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateWithPrompt() {
        val result = geminiGenerator.ask(
            question = "Explain quantum computing",
            prompt = "You are a helpful physics professor."
        )
        println(result)
    }
}
```

### Chat-based Conversation (with History)

Use `GeminiChatGenerator` to maintain conversation context across multiple messages:

```kotlin
@Service
class ChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun havingConversation() {
        // Create a new chat session
        val chat = geminiChatGenerator.createSession()

        // First message
        val response1 = chat.sendMessage("Hello, my name is John")
        println(response1) // "Hello John! How can I help you today?"

        // Second message - AI remembers the context
        val response2 = chat.sendMessage("What's my name?")
        println(response2) // "Your name is John"

        // Get conversation history
        val history = chat.getHistory()
        println("Total messages: ${history.size}")

        // Clear history to start fresh
        chat.clearHistory()
    }
}
```

#### Chat with System Prompt

```kotlin
@Service
class TutorChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun createTutorSession() {
        // Create chat session with system prompt
        val chat = geminiChatGenerator.createSession(
            systemPrompt = "You are a helpful math tutor. Explain concepts step by step."
        )

        chat.sendMessage("What is calculus?")
        chat.sendMessage("Can you give me an example?")
        chat.sendMessage("How does it relate to what you just said?")

        // AI maintains context throughout the conversation
    }
}
```

#### Chat with Streaming

```kotlin
@Service
class StreamingChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun streamingConversation() {
        val chat = geminiChatGenerator.createSession()

        // First message with streaming
        chat.sendMessageStream("Tell me a story") { chunk ->
            print(chunk)  // Prints each word as it arrives
        }

        println("\n---")

        // Follow-up message (AI remembers the story)
        chat.sendMessageStream("What was the main character's name?") { chunk ->
            print(chunk)
        }
    }
}
```

#### Managing Multiple Chat Sessions

```kotlin
@Service
class MultiChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    private val userSessions = mutableMapOf<String, GeminiChatGenerator.ChatSession>()

    fun getUserChat(userId: String): GeminiChatGenerator.ChatSession {
        return userSessions.getOrPut(userId) {
            geminiChatGenerator.createSession()
        }
    }

    fun sendMessageForUser(userId: String, message: String): String? {
        val chat = getUserChat(userId)
        return chat.sendMessage(message)
    }

    fun clearUserHistory(userId: String) {
        userSessions[userId]?.clearHistory()
    }
}
```

#### Custom API Keys and Models per Session

```kotlin
@Service
class CustomSessionService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun createCustomSession() {
        // Create session with specific API keys and models
        val chat = geminiChatGenerator.createSession(
            systemPrompt = "You are a helpful assistant",
            apiKeys = listOf("key1", "key2", "key3"),
            models = listOf("gemini-2.5-flash", "gemini-2.0-flash")
        )

        chat.sendMessage("Hello!")
        // Uses only the specified API keys and models
    }
}
```

#### Session Persistence (Export/Restore)

Save and restore chat sessions for long-term conversation storage:

```kotlin
@Service
class SessionPersistenceService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun saveAndRestoreSession() {
        // Create and use a session
        val chat = geminiChatGenerator.createSession()
        chat.sendMessage("Hello, my name is John")
        chat.sendMessage("I like programming")

        // Export session state for storage
        val sessionState = chat.exportSession()

        // Save to database, file, cache, etc.
        saveToDatabase(sessionState)

        // Later... restore the session
        val restoredChat = geminiChatGenerator.restoreSession(sessionState)

        // Continue the conversation with full context
        val response = restoredChat.sendMessage("What do I like?")
        println(response) // "You like programming"
    }

    private fun saveToDatabase(state: ChatSessionState) {
        // Implementation: save state.apiKeys, state.models, state.history, etc.
    }
}
```

**ChatSessionState** includes:
- `apiKeys`: List of API keys
- `models`: List of model names
- `history`: Conversation history
- `defaultPrompt`: Default system prompt
- `systemPrompt`: Session-specific system prompt
- `baseUrl`: API base URL

### Image Generation

Generate images from text descriptions using Gemini 2.5 Flash Image model:

```kotlin
@Service
class ImageService(
    private val geminiImageGenerator: GeminiImageGenerator
) {
    fun generateImage() {
        // Generate an image
        val images = geminiImageGenerator.generateImage(
            prompt = "A beautiful sunset over mountains"
        )

        images.forEach { image ->
            // image.base64Data - Base64 encoded image data
            // image.mimeType - Image format (e.g., "image/png")
            saveImage(image.base64Data, image.mimeType)
        }
    }

    fun generateWithOptions() {
        // Generate with custom aspect ratio and resolution
        val images = geminiImageGenerator.generateImage(
            prompt = "A futuristic city at night",
            aspectRatio = "16:9",
            imageSize = "4K"
        )

        images.forEach { image ->
            println("Generated: ${image.mimeType}")
        }
    }

    fun generateWithReferenceImages() {
        // Generate image with reference images for style guidance
        val referenceImage = ImageData.fromPath("style-reference.jpg")

        val images = geminiImageGenerator.generateImage(
            prompt = "A mountain landscape in this artistic style",
            aspectRatio = "3:2",
            imageSize = "2K",
            referenceImages = listOf(referenceImage)
        )

        images.forEach { image ->
            println("Generated styled image: ${image.mimeType}")
        }
    }

    private fun saveImage(base64Data: String, mimeType: String) {
        // Implementation to save image
        val bytes = Base64.getDecoder().decode(base64Data)
        // Save bytes to file...
    }
}
```

**Supported aspect ratios**: `1:1`, `2:3`, `3:2`, `3:4`, `4:3`, `4:5`, `5:4`, `9:16`, `16:9`, `21:9`

**Supported image sizes**: `1K`, `2K`, `4K`

### Vision (Image Input)

Send images along with your questions for image analysis and understanding:

#### Basic Image Analysis

```kotlin
import com.hshim.kemi.model.ImageData

@Service
class VisionService(
    private val geminiGenerator: GeminiGenerator
) {
    fun analyzeImage() {
        // Load image from file
        val image = ImageData.fromPath("/path/to/photo.jpg")

        val answer = geminiGenerator.askWithImages(
            question = "What's in this image?",
            images = listOf(image)
        )
        println(answer)
    }

    fun compareImages() {
        // Analyze multiple images at once
        val image1 = ImageData.fromFile(File("photo1.jpg"))
        val image2 = ImageData.fromFile(File("photo2.png"))

        val answer = geminiGenerator.askWithImages(
            question = "What are the differences between these two images?",
            images = listOf(image1, image2)
        )
        println(answer)
    }
}
```

#### Vision in Chat Conversations

```kotlin
@Service
class VisionChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun analyzeImagesInConversation() {
        val chat = geminiChatGenerator.createSession()

        // First message with image
        val image1 = ImageData.fromPath("diagram.png")
        chat.sendMessageWithImages(
            message = "What does this diagram show?",
            images = listOf(image1)
        )

        // Follow-up question (AI remembers the image context)
        chat.sendMessage("Can you explain the third component?")

        // Another image in the same conversation
        val image2 = ImageData.fromPath("chart.png")
        chat.sendMessageWithImages(
            message = "How does this chart relate to the previous diagram?",
            images = listOf(image2)
        )
    }

    fun streamVisionResponse() {
        val chat = geminiChatGenerator.createSession()
        val image = ImageData.fromFile(File("complex-image.jpg"))

        // Stream response for image analysis
        chat.sendMessageStreamWithImages(
            message = "Describe this image in detail",
            images = listOf(image)
        ) { chunk ->
            print(chunk)  // Prints analysis as it's generated
        }
    }
}
```

#### ImageData Creation Options

```kotlin
// From file path
val image1 = ImageData.fromPath("/path/to/image.jpg")

// From File object
val image2 = ImageData.fromFile(File("photo.png"))

// From byte array
val bytes = getImageBytes()
val image3 = ImageData.fromBytes(bytes, "image/jpeg")

// From base64 string
val base64String = getBase64Image()
val image4 = ImageData.fromBase64(base64String, "image/png")
```

**Supported image formats**: JPEG, PNG, GIF, WebP

### Structured Response (Data Class Mapping)

#### Basic Usage

```kotlin
data class Product(
    val name: String,
    val price: Int,
    val description: String
)

@Service
class ProductService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateProduct() {
        val product = geminiGenerator.askWithClass<Product>(
            "Create a product description for a smartphone"
        )
        println(product?.name)
        println(product?.price)
        println(product?.description)
    }
}
```

#### Advanced Usage with Annotations

Use `@GeminiPrompt` and `@GeminiField` annotations to provide additional context to the AI model:

```kotlin
import com.hshim.kemi.annotation.GeminiPrompt
import com.hshim.kemi.annotation.GeminiField

@GeminiPrompt("Generate accurate and realistic product information for e-commerce")
data class Product(
    @GeminiField("Product name, should be clear and concise")
    val name: String,

    @GeminiField("Price in USD, must be a positive integer")
    val price: Int,

    @GeminiField("Detailed product description, 2-3 sentences")
    val description: String,

    @GeminiField("Product category (e.g., Electronics, Clothing, Food)")
    val category: String
)

@Service
class AdvancedProductService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateDetailedProduct() {
        val product = geminiGenerator.askWithClass<Product>(
            "Create a premium smartphone product"
        )
        // AI will use the annotations to generate more accurate results
        println(product)
    }
}
```

### Using Specific Model

```kotlin
@Service
class ModelService(
    private val geminiGenerator: GeminiGenerator
) {
    fun useSpecificModel() {
        val response = geminiGenerator.directAsk(
            question = "What is artificial intelligence?",
            model = "gemini-2.5-flash"
        )
        println(response?.answer)
    }
}
```

### Automatic Model and API Key Fallback

When a request fails, Kemi automatically retries with the next configured API key, then the next model:

```kotlin
@Service
class FallbackExample(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateWithFallback() {
        try {
            // Tries all API keys for gemini-2.5-flash, then moves to next model
            // Example: key1+model1 → key2+model1 → key3+model1 → key1+model2 ...
            val answer = geminiGenerator.ask("Complex question")
            println(answer)
        } catch (e: IllegalStateException) {
            // Thrown when all combinations (models × API keys) are exhausted
            println("All fallback options exhausted")
        }
    }
}
```

## Configuration Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `kemi.gemini.api-keys` | List<String> | ✅ | - | List of Gemini API Keys for fallback (at least 1 required) |
| `kemi.gemini.base-url` | String | ❌ | `https://generativelanguage.googleapis.com` | API base URL |
| `kemi.gemini.models` | List<String> | ❌ | `["gemini-2.5-pro"]` | List of model names for fallback |
| `kemi.gemini.image-models` | List<String> | ❌ | `["gemini-2.5-flash-image", "gemini-3-pro-image-preview"]` | List of image generation model names for fallback |

## API Reference

### GeminiGenerator

#### Methods

- `ask(question: String, prompt: String? = null): String?`
  - Basic question/answer with optional system prompt
  - Returns: Generated text response

- `askWithImages(question: String, images: List<ImageData>, prompt: String? = null): String?`
  - Question/answer with image input for vision capabilities
  - Returns: Generated text response analyzing the images

- `askStream(question: String, prompt: String? = null, handler: StreamResponseHandler): String?`
  - Real-time streaming response with callback handler
  - Returns: Complete response text

- `askWithClass<T>(question: String, prompt: String? = null): T?`
  - Structured response mapped to data class
  - Returns: Parsed instance of type T

- `directAsk(question: String, model: String, prompt: String? = null, customApiKey: String? = null): GeminiResponse?`
  - Direct API call with specific model and optional custom API key
  - Returns: Full GeminiResponse object

- `directAskWithImages(question: String, images: List<ImageData>, model: String, prompt: String? = null, apiKey: String): GeminiResponse?`
  - Direct API call with images using specific model and API key
  - Returns: Full GeminiResponse object

- `currentModel: String`
  - Property that returns currently active model name

- `currentApiKey: String`
  - Property that returns currently active API key

### GeminiChatGenerator

#### Methods

- `createSession(systemPrompt: String? = null, apiKeys: List<String>? = null, models: List<String>? = null): ChatSession`
  - Create a new chat session with optional configuration
  - Returns: ChatSession instance

- `restoreSession(state: ChatSessionState): ChatSession`
  - Restore a chat session from saved state
  - Returns: ChatSession instance

#### ChatSession Methods

- `sendMessage(message: String): String?`
  - Send a text message and get response
  - Returns: AI response text

- `sendMessageWithImages(message: String, images: List<ImageData>): String?`
  - Send a message with images and get response
  - Returns: AI response text

- `sendMessageStream(message: String, handler: StreamResponseHandler): String?`
  - Send a message with streaming response
  - Returns: Complete response text

- `sendMessageStreamWithImages(message: String, images: List<ImageData>, handler: StreamResponseHandler): String?`
  - Send a message with images and streaming response
  - Returns: Complete response text

- `getHistory(): List<ChatMessage>`
  - Get conversation history
  - Returns: List of chat messages

- `clearHistory()`
  - Clear conversation history

- `exportSession(): ChatSessionState`
  - Export session state for persistence
  - Returns: ChatSessionState

### GeminiImageGenerator

#### Methods

- `generateImage(prompt: String, aspectRatio: String = "1:1", imageSize: String = "2K", referenceImages: List<ImageData>? = null): List<GeminiImageResponse.Image>`
  - Generate images from text description
  - Parameters:
    - `prompt`: Text description of the image
    - `aspectRatio`: Image aspect ratio (1:1, 2:3, 3:2, 3:4, 4:3, 4:5, 5:4, 9:16, 16:9, 21:9)
    - `imageSize`: Image resolution (1K, 2K, 4K)
    - `referenceImages`: Optional reference images for style/content guidance
  - Returns: List of generated images with base64 data

- `directGenerateImage(prompt: String, aspectRatio: String = "1:1", imageSize: String = "2K", referenceImages: List<ImageData>? = null, model: String, apiKey: String): GeminiImageResponse?`
  - Direct image generation with specific model and API key
  - Returns: Full GeminiImageResponse object

- `currentModel: String`
  - Property that returns currently active image generation model name

- `currentApiKey: String`
  - Property that returns currently active API key

### GeminiProperties

#### Methods

- `getAllApiKeys(): List<String>`
  - Returns all configured API keys
  - Throws `IllegalStateException` if not configured

- `getApiKey(index: Int = 0): String`
  - Returns API key at specified index
  - Falls back to first key if index is out of bounds

- `generateContentUrl(model: String, apiKeyIndex: Int = 0): String`
  - Generates complete API endpoint URL with API key at index

- `generateContentUrl(model: String, customApiKey: String): String`
  - Generates complete API endpoint URL with custom API key

#### Properties

- `apiKeyProvider: (() -> String)?`
  - Callback function for dynamic API key retrieval

## Advanced Examples

### Dynamic API Key from AWS Secrets Manager

```kotlin
import com.hshim.kemi.config.GeminiProperties
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Configuration
class SecretManagerConfig(
    private val geminiProperties: GeminiProperties,
    private val secretsManagerClient: SecretsManagerClient
) {
    @Bean
    fun configureSecretManager() {
        geminiProperties.apiKeyProvider = {
            val response = secretsManagerClient.getSecretValue {
                it.secretId("gemini-api-key")
            }
            response.secretString()
        }
    }
}
```

### Environment-Based Configuration

```kotlin
@Configuration
class EnvironmentConfig(
    private val geminiProperties: GeminiProperties
) {
    @Bean
    fun configureEnvironment() {
        geminiProperties.apiKeyProvider = {
            when (System.getenv("SPRING_PROFILES_ACTIVE")) {
                "prod" -> System.getenv("PROD_GEMINI_KEY")
                "dev" -> System.getenv("DEV_GEMINI_KEY")
                else -> "test-key"
            }
        }
    }
}
```

## License

This project is licensed under the terms of the MIT license.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions, please use the [GitHub Issues](https://github.com/hyuck0221/kemi/issues) page.