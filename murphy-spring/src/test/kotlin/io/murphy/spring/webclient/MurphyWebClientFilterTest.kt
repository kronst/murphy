package io.murphy.spring.webclient

import io.murphy.core.Effects
import io.murphy.core.Effects.withProbability
import io.murphy.core.Matchers
import io.murphy.core.MurphyRule
import io.murphy.core.MurphyScenario
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import kotlin.test.assertEquals

class MurphyWebClientFilterTest {

    private lateinit var server: MockWebServer

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    private fun buildClient(scenario: MurphyScenario): WebClient {
        return WebClient.builder()
            .baseUrl(server.url("/").toString())
            .filter(MurphyWebClientFilter(scenario))
            .build()
    }

    @Test
    fun `filter blocks request and returns response`() = runTest {
        val json = """{"message": "Hello, Murphy!"}"""
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.json(code = 201, json))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val response = client.get().uri("/test").retrieve().awaitEntity<String>()

        assertEquals(201, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(json, response.body)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `filter applies delay before proceeding to network`() = runTest {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.latency(delay))
            .build()

        val client = buildClient(MurphyScenario.from(rule))

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val start = System.currentTimeMillis()
        val response = client.get().uri("/test").retrieve().awaitEntity<String>()
        val elapsed = System.currentTimeMillis() - start

        assertEquals(200, response.statusCode.value())
        assertEquals("Mock Server Response", response.body)
        assert(elapsed >= delay)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `filter applies delay before returning response`() = runTest {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(
                Effects.latency(delay),
                Effects.status(202),
            )
            .build()

        val client = buildClient(MurphyScenario.from(rule))

        val start = System.currentTimeMillis()
        val response = client.get().uri("/test").retrieve().awaitEntity<String>()
        val elapsed = System.currentTimeMillis() - start

        assertEquals(202, response.statusCode.value())
        assert(elapsed >= delay)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `filter proceeds to network if matcher does not match`() = runTest {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("POST"))
            .causes(Effects.status(500))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val response = client.get().uri("/test").retrieve().awaitEntity<String>()

        assertEquals(200, response.statusCode.value())
        assertEquals("Mock Server Response", response.body)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `filter proceeds to network if probability is not met`() = runTest {
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.status(500).withProbability(0.0))
            .build()

        val client = buildClient(MurphyScenario.from(rule))

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val response = client.get().uri("/test").retrieve().awaitEntity<String>()

        assertEquals(200, response.statusCode.value())
        assertEquals("Mock Server Response", response.body)
        assertEquals(1, server.requestCount)
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
                .causes(Effects.status(202))
                .build()
        )

        val client = buildClient(scenario)

        val responseFoo = client.get().uri("/api/foo").retrieve().awaitEntity<String>()
        assertEquals(201, responseFoo.statusCode.value())

        val responseBar = client.get().uri("/api/bar").retrieve().awaitEntity<String>()
        assertEquals(202, responseBar.statusCode.value())
    }
}
