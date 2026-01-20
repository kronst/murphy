package io.murphy.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.murphy.core.Effects
import io.murphy.core.Effects.withProbability
import io.murphy.core.Matchers
import io.murphy.core.MurphyRule
import io.murphy.core.MurphyScenario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MurphyPluginTest {

    @Test
    fun `plugin blocks request and returns response`() = runTest {
        val json = """{"message": "I'm a teapot"}"""
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.json(code = 418, json))
            .build()

        var serverCallCount = 0
        val client = HttpClient(MockEngine) {
            install(MurphyPlugin) {
                scenario = MurphyScenario.from(rule)
            }
            engine {
                addHandler {
                    serverCallCount++
                    respond("Should not be called", HttpStatusCode.OK)
                }
            }
        }

        val response = client.get("https://test.com/test")

        assertEquals(HttpStatusCode.fromValue(418), response.status)
        assertEquals(ContentType.Application.Json, response.contentType())
        assertEquals(json, response.bodyAsText())
        assertEquals(0, serverCallCount)
    }

    @Test
    fun `plugin applies delay before proceeding to network`() = runTest {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.latency(delay))
            .build()

        var serverCallCount = 0
        val client = HttpClient(MockEngine) {
            install(MurphyPlugin) {
                scenario = MurphyScenario.from(rule)
            }
            engine {
                addHandler {
                    serverCallCount++
                    respond("Mock Server Response", HttpStatusCode.OK)
                }
            }
        }

        val start = currentTime
        val response = client.get("https://test.com/test")
        val elapsed = currentTime - start

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Mock Server Response", response.bodyAsText())
        assert(elapsed >= delay)
        assertEquals(1, serverCallCount)
    }

    @Test
    fun `plugin applies delay before returning response`() = runTest {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(
                Effects.latency(delay),
                Effects.status(202),
            )
            .build()

        var serverCallCount = 0
        val client = HttpClient(MockEngine) {
            install(MurphyPlugin) {
                scenario = MurphyScenario.from(rule)
            }
            engine {
                addHandler {
                    serverCallCount++
                    respond("Should not be called", HttpStatusCode.OK)
                }
            }
        }

        val start = currentTime
        val response = client.get("https://test.com/test")
        val elapsed = currentTime - start

        assertEquals(HttpStatusCode.fromValue(202), response.status)
        assert(elapsed >= delay)
        assertEquals(0, serverCallCount)
    }

    @Test
    fun `plugin proceeds to network if matcher does not match`() = runTest {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("POST"))
            .causes(Effects.status(500))
            .build()

        var serverCallConfig = 0
        val client = HttpClient(MockEngine) {
            install(MurphyPlugin) {
                scenario = MurphyScenario.from(rule)
            }
            engine {
                addHandler {
                    serverCallConfig++
                    respond("Mock Server Response", HttpStatusCode.OK)
                }
            }
        }

        val response = client.get("https://test.com/test")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Mock Server Response", response.bodyAsText())
        assertEquals(1, serverCallConfig)
    }

    @Test
    fun `plugin proceeds to network if probability is not met`() = runTest {
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.status(500).withProbability(0.0))
            .build()

        var serverCallCount = 0
        val client = HttpClient(MockEngine) {
            install(MurphyPlugin) {
                scenario = MurphyScenario.from(rule)
            }
            engine {
                addHandler {
                    serverCallCount++
                    respond("Mock Server Response", HttpStatusCode.OK)
                }
            }
        }

        val response = client.get("https://test.com/test")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Mock Server Response", response.bodyAsText())
        assertEquals(1, serverCallCount)
    }

    @Test
    fun `scenario with multiple rules - first match wins`() = runTest {
        val scenario = MurphyScenario.from(
            MurphyRule.builder()
                .matches(Matchers.path("/api/foo"))
                .causes(Effects.status(201))
                .build(),
            MurphyRule.builder()
                .matches(Matchers.path("/api/**"))
                .causes(Effects.status(500))
                .build()
        )

        var serverCallCount = 0
        val client = HttpClient(MockEngine) {
            install(MurphyPlugin) {
                this.scenario = scenario
            }
            engine {
                addHandler {
                    serverCallCount++
                    respond("Should not be called", HttpStatusCode.OK)
                }
            }
        }

        val responseFoo = client.get("https://test.com/api/foo")
        assertEquals(HttpStatusCode.fromValue(201), responseFoo.status)

        val responseBar = client.get("https://test.com/api/bar")
        assertEquals(HttpStatusCode.fromValue(500), responseBar.status)
    }
}
