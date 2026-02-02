# Kemi - Spring Boot용 Gemini API

자동 모델 폴백과 타입 안전 설정을 갖춘 Google Gemini API Spring Boot 통합 라이브러리입니다.

[English Documentation](README.md)

## 특징

- ✅ Spring Boot 3.1+ 지원
- ✅ 자동 설정 (Auto Configuration)
- ✅ Type-safe Configuration Properties
- ✅ IDE 자동완성 지원
- ✅ AI 모델 자동 fallback 지원
- ✅ 유연한 API 키 설정 (직접 입력 또는 콜백)
- ✅ 데이터 클래스로의 구조화된 응답 매핑
- ✅ Imagen API를 이용한 이미지 생성
- ✅ Vision 지원 (질문 및 채팅에 이미지 입력)

## 지원 버전

- **JVM**: 8+
- **Kotlin**: 1.9+
- **Spring Boot**: 2.7+
- **Spring Framework**: 5.3+

## 설치

### JitPack 저장소 추가

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

## 설정

### application.yml

```yaml
kemi:
  gemini:
    api-keys:  # 필수: 최소 1개 이상의 API 키를 입력해야 합니다
      - your-first-api-key
      - your-second-api-key
      - your-third-api-key
    base-url: https://generativelanguage.googleapis.com  # 선택
    models:
      - gemini-2.5-flash
      - gemini-2.5-flash-lite
      - gemini-2.0-flash
      - gemini-2.0-flash-lite
```

### API 키 설정 옵션

#### 옵션 1: 여러 API 키 (권장)

```yaml
kemi:
  gemini:
    api-keys:  # 필수
      - your-first-api-key
      - your-second-api-key
```

#### 옵션 2: 콜백 함수 (프로그래밍 방식)

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
        // 예: 외부 서비스나 시크릿 관리자에서 API 키 로드
        geminiProperties.apiKeyProvider = {
            // API 키를 가져오는 커스텀 로직
            System.getenv("GEMINI_API_KEY") ?: "fallback-key"
        }
    }
}
```

## 사용 예시

### 기본 사용법

```kotlin
import com.hshim.kemi.GeminiGenerator
import org.springframework.stereotype.Service

@Service
class MyService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateResponse() {
        val answer = geminiGenerator.ask("안녕하세요, 어떻게 지내세요?")
        println(answer)
    }
}
```

### 스트리밍 응답 (실시간)

생성되는 대로 실시간으로 응답 받기:

```kotlin
@Service
class StreamingService(
    private val geminiGenerator: GeminiGenerator
) {
    fun streamResponse() {
        val fullResponse = geminiGenerator.askStream(
            question = "로봇에 대한 긴 이야기를 써주세요",
            handler = { chunk ->
                // 각 청크가 도착할 때마다 호출됨
                print(chunk)  // 스트리밍되는 즉시 출력
            }
        )

        println("\n\n완전한 응답: $fullResponse")
    }
}
```

### 커스텀 프롬프트

```kotlin
@Service
class PromptService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateWithPrompt() {
        val result = geminiGenerator.ask(
            question = "양자 컴퓨팅을 설명해주세요",
            prompt = "당신은 친절한 물리학 교수입니다."
        )
        println(result)
    }
}
```

### 채팅 기반 대화 (히스토리 유지)

`GeminiChatGenerator`를 사용하여 여러 메시지에 걸쳐 대화 컨텍스트를 유지할 수 있습니다:

```kotlin
@Service
class ChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun havingConversation() {
        // 새 채팅 세션 생성
        val chat = geminiChatGenerator.createSession()

        // 첫 번째 메시지
        val response1 = chat.sendMessage("안녕하세요, 제 이름은 John입니다")
        println(response1) // "안녕하세요 John님! 무엇을 도와드릴까요?"

        // 두 번째 메시지 - AI가 컨텍스트를 기억합니다
        val response2 = chat.sendMessage("제 이름이 뭐죠?")
        println(response2) // "당신의 이름은 John입니다"

        // 대화 기록 가져오기
        val history = chat.getHistory()
        println("전체 메시지 수: ${history.size}")

        // 기록 지우고 새로 시작
        chat.clearHistory()
    }
}
```

#### 시스템 프롬프트와 함께 채팅

```kotlin
@Service
class TutorChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun createTutorSession() {
        // 시스템 프롬프트와 함께 채팅 세션 생성
        val chat = geminiChatGenerator.createSession(
            systemPrompt = "당신은 친절한 수학 튜터입니다. 개념을 단계별로 설명해주세요."
        )

        chat.sendMessage("미적분학이 무엇인가요?")
        chat.sendMessage("예시를 들어주실 수 있나요?")
        chat.sendMessage("방금 말씀하신 것과 어떻게 연관되나요?")

        // AI가 대화 전체에 걸쳐 컨텍스트를 유지합니다
    }
}
```

#### 스트리밍 채팅

```kotlin
@Service
class StreamingChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun streamingConversation() {
        val chat = geminiChatGenerator.createSession()

        // 스트리밍으로 첫 메시지 전송
        chat.sendMessageStream("이야기를 들려주세요") { chunk ->
            print(chunk)  // 각 단어가 도착하는 즉시 출력
        }

        println("\n---")

        // 후속 메시지 (AI가 이야기를 기억함)
        chat.sendMessageStream("주인공의 이름이 뭐였죠?") { chunk ->
            print(chunk)
        }
    }
}
```

#### 여러 채팅 세션 관리

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

#### 세션별 커스텀 API 키 및 모델 설정

```kotlin
@Service
class CustomSessionService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun createCustomSession() {
        // 특정 API 키와 모델로 세션 생성
        val chat = geminiChatGenerator.createSession(
            systemPrompt = "당신은 친절한 어시스턴트입니다",
            apiKeys = listOf("key1", "key2", "key3"),
            models = listOf("gemini-2.5-flash", "gemini-2.0-flash")
        )

        chat.sendMessage("안녕하세요!")
        // 지정된 API 키와 모델만 사용합니다
    }
}
```

#### 세션 영속성 (내보내기/복원)

장기 대화 저장을 위해 채팅 세션을 저장하고 복원할 수 있습니다:

```kotlin
@Service
class SessionPersistenceService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun saveAndRestoreSession() {
        // 세션 생성 및 사용
        val chat = geminiChatGenerator.createSession()
        chat.sendMessage("안녕하세요, 제 이름은 John입니다")
        chat.sendMessage("저는 프로그래밍을 좋아합니다")

        // 저장을 위해 세션 상태 내보내기
        val sessionState = chat.exportSession()

        // 데이터베이스, 파일, 캐시 등에 저장
        saveToDatabase(sessionState)

        // 나중에... 세션 복원
        val restoredChat = geminiChatGenerator.restoreSession(sessionState)

        // 전체 컨텍스트로 대화 계속하기
        val response = restoredChat.sendMessage("제가 뭘 좋아하죠?")
        println(response) // "당신은 프로그래밍을 좋아합니다"
    }

    private fun saveToDatabase(state: ChatSessionState) {
        // 구현: state.apiKeys, state.models, state.history 등을 저장
    }
}
```

**ChatSessionState**에 포함된 정보:
- `apiKeys`: API 키 목록
- `models`: 모델명 목록
- `history`: 대화 기록
- `defaultPrompt`: 기본 시스템 프롬프트
- `systemPrompt`: 세션별 시스템 프롬프트
- `baseUrl`: API 기본 URL

### 이미지 생성

Google의 Imagen API를 사용하여 텍스트 설명으로부터 이미지를 생성할 수 있습니다:

```kotlin
@Service
class ImageService(
    private val geminiImageGenerator: GeminiImageGenerator
) {
    fun generateImage() {
        // 단일 이미지 생성
        val images = geminiImageGenerator.generateImage(
            prompt = "산 위로 지는 아름다운 석양"
        )

        images?.forEach { image ->
            // image.base64Data - Base64로 인코딩된 이미지 데이터
            // image.mimeType - 이미지 형식 (예: "image/png")
            saveImage(image.base64Data, image.mimeType)
        }
    }

    fun generateMultipleImages() {
        // 한 번에 여러 이미지 생성 (1-4개)
        val images = geminiImageGenerator.generateImage(
            prompt = "미래의 도시",
            numberOfImages = 3,
            aspectRatio = "16:9",
            negativePrompt = "흐릿한, 저품질"
        )

        images?.forEachIndexed { index, image ->
            println("이미지 ${index + 1}: ${image.mimeType}")
        }
    }

    private fun saveImage(base64Data: String, mimeType: String) {
        // 이미지 저장 구현
        val bytes = Base64.getDecoder().decode(base64Data)
        // 바이트를 파일로 저장...
    }
}
```

**지원되는 종횡비**: `1:1`, `16:9`, `9:16`, `4:3`, `3:4`

### Vision (이미지 입력)

이미지 분석 및 이해를 위해 질문과 함께 이미지를 전송할 수 있습니다:

#### 기본 이미지 분석

```kotlin
import com.hshim.kemi.model.ImageData

@Service
class VisionService(
    private val geminiGenerator: GeminiGenerator
) {
    fun analyzeImage() {
        // 파일에서 이미지 로드
        val image = ImageData.fromPath("/path/to/photo.jpg")

        val answer = geminiGenerator.askWithImages(
            question = "이 이미지에 무엇이 있나요?",
            images = listOf(image)
        )
        println(answer)
    }

    fun compareImages() {
        // 여러 이미지를 한 번에 분석
        val image1 = ImageData.fromFile(File("photo1.jpg"))
        val image2 = ImageData.fromFile(File("photo2.png"))

        val answer = geminiGenerator.askWithImages(
            question = "이 두 이미지의 차이점은 무엇인가요?",
            images = listOf(image1, image2)
        )
        println(answer)
    }
}
```

#### 채팅 대화에서 Vision 사용

```kotlin
@Service
class VisionChatService(
    private val geminiChatGenerator: GeminiChatGenerator
) {
    fun analyzeImagesInConversation() {
        val chat = geminiChatGenerator.createSession()

        // 이미지와 함께 첫 메시지
        val image1 = ImageData.fromPath("diagram.png")
        chat.sendMessageWithImages(
            message = "이 다이어그램은 무엇을 보여주나요?",
            images = listOf(image1)
        )

        // 후속 질문 (AI가 이미지 컨텍스트를 기억함)
        chat.sendMessage("세 번째 구성 요소를 설명해 주실 수 있나요?")

        // 같은 대화에서 다른 이미지
        val image2 = ImageData.fromPath("chart.png")
        chat.sendMessageWithImages(
            message = "이 차트는 이전 다이어그램과 어떤 관련이 있나요?",
            images = listOf(image2)
        )
    }

    fun streamVisionResponse() {
        val chat = geminiChatGenerator.createSession()
        val image = ImageData.fromFile(File("complex-image.jpg"))

        // 이미지 분석을 스트리밍 응답으로 받기
        chat.sendMessageStreamWithImages(
            message = "이 이미지를 자세히 설명해주세요",
            images = listOf(image)
        ) { chunk ->
            print(chunk)  // 분석이 생성되는 대로 출력
        }
    }
}
```

#### ImageData 생성 옵션

```kotlin
// 파일 경로에서
val image1 = ImageData.fromPath("/path/to/image.jpg")

// File 객체에서
val image2 = ImageData.fromFile(File("photo.png"))

// 바이트 배열에서
val bytes = getImageBytes()
val image3 = ImageData.fromBytes(bytes, "image/jpeg")

// base64 문자열에서
val base64String = getBase64Image()
val image4 = ImageData.fromBase64(base64String, "image/png")
```

**지원되는 이미지 형식**: JPEG, PNG, GIF, WebP

### 구조화된 응답 (데이터 클래스 매핑)

#### 기본 사용법

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
            "스마트폰에 대한 제품 설명을 작성해주세요"
        )
        println(product?.name)
        println(product?.price)
        println(product?.description)
    }
}
```

#### 어노테이션을 사용한 고급 사용법

`@GeminiPrompt`와 `@GeminiField` 어노테이션을 사용하여 AI 모델에 추가 컨텍스트를 제공할 수 있습니다:

```kotlin
import com.hshim.kemi.annotation.GeminiPrompt
import com.hshim.kemi.annotation.GeminiField

@GeminiPrompt("전자상거래를 위한 정확하고 현실적인 제품 정보를 생성하세요")
data class Product(
    @GeminiField("제품명, 명확하고 간결해야 함")
    val name: String,

    @GeminiField("USD 가격, 양의 정수여야 함")
    val price: Int,

    @GeminiField("상세한 제품 설명, 2-3문장")
    val description: String,

    @GeminiField("제품 카테고리 (예: 전자제품, 의류, 식품)")
    val category: String
)

@Service
class AdvancedProductService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateDetailedProduct() {
        val product = geminiGenerator.askWithClass<Product>(
            "프리미엄 스마트폰 제품을 생성해주세요"
        )
        // AI가 어노테이션을 사용하여 더 정확한 결과를 생성합니다
        println(product)
    }
}
```

### 특정 모델 직접 사용

```kotlin
@Service
class ModelService(
    private val geminiGenerator: GeminiGenerator
) {
    fun useSpecificModel() {
        val response = geminiGenerator.directAsk(
            question = "인공지능이 무엇인가요?",
            model = "gemini-2.5-flash"
        )
        println(response?.answer)
    }
}
```

### 자동 모델 및 API 키 폴백

요청이 실패하면 Kemi는 자동으로 다음 설정된 API 키로 재시도하고, 이후 다음 모델로 이동합니다:

```kotlin
@Service
class FallbackExample(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateWithFallback() {
        try {
            // gemini-2.5-flash의 모든 API 키를 시도한 후 다음 모델로 이동
            // 예: key1+model1 → key2+model1 → key3+model1 → key1+model2 ...
            val answer = geminiGenerator.ask("복잡한 질문")
            println(answer)
        } catch (e: IllegalStateException) {
            // 모든 조합 (모델 × API 키)이 소진되면 발생
            println("모든 폴백 옵션이 소진되었습니다")
        }
    }
}
```

## 설정 속성

| 속성 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| `kemi.gemini.api-keys` | List<String> | ✅ | - | 폴백을 위한 Gemini API 키 목록 (최소 1개 필수) |
| `kemi.gemini.base-url` | String | ❌ | `https://generativelanguage.googleapis.com` | API 기본 URL |
| `kemi.gemini.models` | List<String> | ❌ | `["gemini-2.5-pro"]` | 폴백을 위한 모델명 목록 |

## API 레퍼런스

### GeminiGenerator

#### 메서드

- `ask(question: String, prompt: String? = null): String?`
  - 선택적 시스템 프롬프트를 사용한 기본 질의응답
  - 반환: 생성된 텍스트 응답

- `askWithImages(question: String, images: List<ImageData>, prompt: String? = null): String?`
  - Vision 기능을 위한 이미지 입력과 함께 질의응답
  - 반환: 이미지를 분석한 텍스트 응답

- `askStream(question: String, prompt: String? = null, handler: StreamResponseHandler): String?`
  - 콜백 핸들러를 사용한 실시간 스트리밍 응답
  - 반환: 완전한 응답 텍스트

- `askWithClass<T>(question: String, prompt: String? = null): T?`
  - 데이터 클래스로 매핑된 구조화된 응답
  - 반환: T 타입의 파싱된 인스턴스

- `directAsk(question: String, model: String, prompt: String? = null, customApiKey: String? = null): GeminiResponse?`
  - 특정 모델과 선택적 커스텀 API 키를 사용한 직접 API 호출
  - 반환: 전체 GeminiResponse 객체

- `directAskWithImages(question: String, images: List<ImageData>, model: String, prompt: String? = null, apiKey: String): GeminiResponse?`
  - 특정 모델과 API 키를 사용한 이미지 포함 직접 API 호출
  - 반환: 전체 GeminiResponse 객체

- `currentModel: String`
  - 현재 활성화된 모델명을 반환하는 프로퍼티

- `currentApiKey: String`
  - 현재 활성화된 API 키를 반환하는 프로퍼티

### GeminiChatGenerator

#### 메서드

- `createSession(systemPrompt: String? = null, apiKeys: List<String>? = null, models: List<String>? = null): ChatSession`
  - 선택적 설정으로 새 채팅 세션 생성
  - 반환: ChatSession 인스턴스

- `restoreSession(state: ChatSessionState): ChatSession`
  - 저장된 상태에서 채팅 세션 복원
  - 반환: ChatSession 인스턴스

#### ChatSession 메서드

- `sendMessage(message: String): String?`
  - 텍스트 메시지를 보내고 응답 받기
  - 반환: AI 응답 텍스트

- `sendMessageWithImages(message: String, images: List<ImageData>): String?`
  - 이미지와 함께 메시지를 보내고 응답 받기
  - 반환: AI 응답 텍스트

- `sendMessageStream(message: String, handler: StreamResponseHandler): String?`
  - 스트리밍 응답으로 메시지 보내기
  - 반환: 완전한 응답 텍스트

- `sendMessageStreamWithImages(message: String, images: List<ImageData>, handler: StreamResponseHandler): String?`
  - 이미지와 함께 스트리밍 응답으로 메시지 보내기
  - 반환: 완전한 응답 텍스트

- `getHistory(): List<ChatMessage>`
  - 대화 기록 가져오기
  - 반환: 채팅 메시지 목록

- `clearHistory()`
  - 대화 기록 지우기

- `exportSession(): ChatSessionState`
  - 영속성을 위한 세션 상태 내보내기
  - 반환: ChatSessionState

### GeminiImageGenerator

#### 메서드

- `generateImage(prompt: String, numberOfImages: Int = 1, aspectRatio: String = "1:1", negativePrompt: String? = null): List<GeminiImageResponse.Image>?`
  - 텍스트 설명으로부터 이미지 생성
  - 파라미터:
    - `prompt`: 이미지의 텍스트 설명
    - `numberOfImages`: 생성할 이미지 개수 (1-4)
    - `aspectRatio`: 이미지 종횡비 (1:1, 16:9, 9:16, 4:3, 3:4)
    - `negativePrompt`: 이미지에서 피해야 할 것
  - 반환: base64 데이터를 포함한 생성된 이미지 목록

- `directGenerateImage(prompt: String, numberOfImages: Int = 1, aspectRatio: String = "1:1", negativePrompt: String? = null, model: String, apiKey: String): GeminiImageResponse?`
  - 특정 모델과 API 키를 사용한 직접 이미지 생성
  - 반환: 전체 GeminiImageResponse 객체

- `currentModel: String`
  - 현재 활성화된 Imagen 모델명을 반환하는 프로퍼티

- `currentApiKey: String`
  - 현재 활성화된 API 키를 반환하는 프로퍼티

### GeminiProperties

#### 메서드

- `getAllApiKeys(): List<String>`
  - 설정된 모든 API 키 반환
  - 설정되지 않은 경우 `IllegalStateException` 발생

- `getApiKey(index: Int = 0): String`
  - 지정된 인덱스의 API 키 반환
  - 인덱스가 범위를 벗어나면 첫 번째 키로 폴백

- `generateContentUrl(model: String, apiKeyIndex: Int = 0): String`
  - 인덱스의 API 키로 완전한 API 엔드포인트 URL 생성

- `generateContentUrl(model: String, customApiKey: String): String`
  - 커스텀 API 키로 완전한 API 엔드포인트 URL 생성

#### 프로퍼티

- `apiKeyProvider: (() -> String)?`
  - 동적 API 키 검색을 위한 콜백 함수

## 고급 예제

### AWS Secrets Manager에서 동적 API 키 가져오기

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

### 환경 기반 설정

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

## 라이선스

이 프로젝트는 MIT 라이선스 조항에 따라 라이선스가 부여됩니다.

## 기여

기여를 환영합니다! Pull Request를 자유롭게 제출해 주세요.

## 지원

이슈 및 질문은 [GitHub Issues](https://github.com/hyuck0221/kemi/issues) 페이지를 이용해 주세요.
