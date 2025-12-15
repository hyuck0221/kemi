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
    implementation("com.github.hyuck0221:kemi:0.0.7")
}
```

#### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.hyuck0221:kemi:0.0.7'
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
        <version>0.0.7</version>
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

- `askWithClass<T>(question: String, prompt: String? = null): T?`
  - 데이터 클래스로 매핑된 구조화된 응답
  - 반환: T 타입의 파싱된 인스턴스

- `directAsk(question: String, model: String, prompt: String? = null, customApiKey: String? = null): GeminiResponse?`
  - 특정 모델과 선택적 커스텀 API 키를 사용한 직접 API 호출
  - 반환: 전체 GeminiResponse 객체

- `currentModel: String`
  - 현재 활성화된 모델명을 반환하는 프로퍼티

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
