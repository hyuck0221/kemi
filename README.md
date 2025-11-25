# Kemi - Gemini API for Spring Boot

Spring Boot 애플리케이션을 위한 Gemini API 라이브러리입니다.

## 특징

- ✅ Spring Boot 3.1+ 지원
- ✅ 자동 설정 (Auto Configuration)
- ✅ Type-safe Configuration Properties
- ✅ IDE 자동완성 지원
- ✅ AI 모델 자동 fallback 지원
- ✅ 유연한 설정 옵션

## 지원 버전

- **Java**: 17+
- **Kotlin**: 1.9+
- **Spring Boot**: 3.1+
- **Spring Framework**: 6.0+

## 설치

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.github.hyuck0221:kemi:0.0.1")
}
```

## 설정

### application.yml

```yaml
kemi:
  gemini:
    api-key: ${GEMINI_API_KEY}  # 필수
    base-url: https://generativelanguage.googleapis.com  # 선택
    model:
      - gemini-2.5-flash
      - gemini-2.5-flash-lite
      - gemini-2.0-flash
      - gemini-2.0-flash-lite
```

### 환경 변수

```bash
export GEMINI_API_KEY=your-api-key-here
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
    fun generate() {
        // 간단한 질문
        val answer = geminiGenerator.ask("Hello, how are you?")
        println(answer)
    }
}
```

### 커스텀 프롬프트 사용

```kotlin
@Service
class PromptService(
    private val geminiGenerator: GeminiGenerator
) {
    fun generateWithPrompt() {
        val result = geminiGenerator
            .prompt("You are a helpful assistant.")
            .ask("Explain quantum computing")
        println(result)
    }
}
```

### 구조화된 응답 (Data Class)

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
            question = "What is AI?",
            model = "gemini-2.5-flash"
        )
        println(response?.answer)
    }
}
```

## 설정 속성

| 속성 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| `kemi.gemini.api-key` | String | ✅ | - | Gemini API Key |
| `kemi.gemini.base-url` | String | ❌ | `https://generativelanguage.googleapis.com` | API 기본 URL |
| `kemi.gemini.model` | List<String> | ❌ | `["gemini-2.5-flash"]` | 사용할 모델명 목록 |


This project is licensed under the terms of the MIT license.