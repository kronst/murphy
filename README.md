# 🕵️‍♂️ Murphy

**Murphy** is a lightweight chaos engineering library for the JVM (Kotlin & Java) designed to simulate network failures, latency, and erratic API behavior in your HTTP clients.

Named after *Murphy's Law*, this library helps you ensure that if anything *can* go wrong with your downstream services, your application is prepared to handle it.

## ✨ Features

* **Zero-dependency Core**: The heart of Murphy is pure Kotlin with no external dependencies.
* **First Match Wins**: Define a scenario with multiple rules; Murphy applies the first matching one.
* **Probability-based Chaos**: Inject failures only in a percentage of requests (e.g., "fail only 5% of the time").
* **Kotlin & Java Friendly**: Idiomatic DSL for Kotlin and clean Builders for Java.
* **Extensible**: Modular architecture to support any HTTP client (OkHttp, Ktor, Spring, etc.).

## 🚀 Getting Started

### 1. Define your Scenario

A **Scenario** is a collection of rules that describe what should go wrong and when.

#### Kotlin
```kotlin
val scenario = MurphyScenario.from(
    // Rule: Make all /api/v1/users requests slow
    MurphyRule.builder()
        .matches(Matchers.path("/api/v1/users/**"))
        .causes(Effects.latency(500, TimeUnit.MILLISECONDS))
        .build(),

    // Rule: 10% of POST requests to payments should fail
    MurphyRule.builder()
        .matches(Matchers.all(Matchers.method("POST"), Matchers.path("/payments")))
        .causes(Effects.status(503).withProbability(0.1))
        .build(),
)
```

#### Java
```java
MurphyScenario scenario = MurphyScenario.from(
    MurphyRule.builder()
        .matches(Matchers.path("/api/**"))
        .causes(Effects.status(500))
        .build()
);
```

### 2. Plug it into your Client

#### OkHttp
```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(MurphyOkHttpInterceptor(scenario))
    .build()
```

#### Spring RestClient
> [!NOTE]
> Spring `RestTemplate` is not supported because it is [deprecated](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-resttemplate) in favor of `RestClient`.
```kotlin
val client = RestClient.builder()
    .requestInterceptor(MurphyRestClientInterceptor(scenario))
    .build()
```

#### Spring WebClient
```kotlin
val client = WebClient.builder()
    .filter(MurphyWebClientFilter(scenario))
    .build()
```

#### Ktor Client
```kotlin
val client = HttpClient(CIO) {
    install(MurphyPlugin) { this.scenario = scenario }
}
```

## 🛠 Available Effects
- `latency(ms)`: Adds a fixed delay.
- `jitter(min, max)`: Adds a random delay within a specified range.
- `status(code)`: Returns an empty response with the specified HTTP status code.
- `json(code, body)`: Returns a JSON response with a custom status code.
- `crash()`: Throws an `IOException` to simulate a sudden network drop.
- `withProbability(0.0..1.0)`: Decorates any effect to trigger it randomly.

## 🧩 Architecture & Extensibility

If you want to write a custom interceptor or extend Murphy's logic, here are the core components:

* **`MurphyContext`**: A simple data class representing an outgoing request (URL, path, method, and headers).
* **`MurphyResponse`**: A value object representing the desired chaos response (status code, headers, and body).
* **`Matcher`**: A functional interface `(MurphyContext) -> Boolean`. Use it to define when a rule should be triggered.
* **`Effect`**: The heart of failure injection. It takes a `MurphyContext` and can either:
    * Return `null`: Perform a side effect (like delay) and continue to the next effect.
    * Return `MurphyResponse`: Intercept the call and return this response immediately.
    * Throw an `Exception`: Simulate a low-level network crash.
* **`MurphyRule`**: Combines a `Matcher` with a list of `Effect`s.
* **`MurphyScenario`**: A collection of rules that uses the **First Match Wins** strategy to pick the appropriate rule for a request.



### Custom Effects Example

You can create your own effects by implementing the `Effect` interface:

```kotlin
val loggerEffect = Effect { context ->
    println("Applying chaos to: ${context.url}")
    null // Continue to next effects
}
```

## 🗺 Roadmap
- [x] OkHttp Interceptor
- [x] Spring RestClient Interceptor
- [x] Spring WebClient Filter
- [x] Ktor Client Plugin
