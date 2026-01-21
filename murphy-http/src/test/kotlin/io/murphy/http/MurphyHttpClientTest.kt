package io.murphy.http

import io.murphy.core.Effects
import io.murphy.core.Effects.withProbability
import io.murphy.core.Matchers
import io.murphy.core.MurphyRule
import io.murphy.core.MurphyScenario
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MurphyHttpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var baseClient: HttpClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        baseClient = HttpClient.newHttpClient()
    }

    @AfterEach
    fun teardown() {
        server.shutdown()
    }

    private fun buildClient(scenario: MurphyScenario): HttpClient {
        return MurphyHttpClient.decorate(baseClient, scenario)
    }

    @Test
    fun `client blocks request and returns response`() {
        val json = """{"message": "I'm a teapot"}"""
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.json(code = 418, json))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(418, response.statusCode())
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null))
        assertEquals(json, response.body())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `client applies delay before proceeding to network`() {
        val delayMs = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.latency(delayMs))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val start = System.currentTimeMillis()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val elapsed = System.currentTimeMillis() - start

        assertEquals(200, response.statusCode())
        assertEquals("Mock Server Response", response.body())
        assertTrue(elapsed >= delayMs)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `client applies delay before returning response`() {
        val delayMs = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(
                Effects.latency(delayMs),
                Effects.status(202),
            )
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        val start = System.currentTimeMillis()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val elapsed = System.currentTimeMillis() - start

        assertEquals(202, response.statusCode())
        assertTrue(elapsed >= delayMs)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `client proceeds to network if matcher does not match`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("POST"))
            .causes(Effects.status(500))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assertEquals("Mock Server Response", response.body())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `client proceeds to network if probability is not met`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.status(500).withProbability(0.0))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assertEquals("Mock Server Response", response.body())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `scenario with multiple rules - first match wins`() {
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

        val client = buildClient(scenario)

        val request1 = HttpRequest.newBuilder().uri(server.url("/api/foo").toUri()).GET().build()
        val response1 = client.send(request1, HttpResponse.BodyHandlers.ofString())
        assertEquals(201, response1.statusCode())

        val request2 = HttpRequest.newBuilder().uri(server.url("/api/bar").toUri()).GET().build()
        val response2 = client.send(request2, HttpResponse.BodyHandlers.ofString())
        assertEquals(500, response2.statusCode())
    }

    @Test
    fun `client blocks async request and returns response`() {
        val json = """{"message": "I'm a teapot"}"""
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.json(code = 418, json))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join()

        assertEquals(418, response.statusCode())
        assertEquals("application/json", response.headers().firstValue("Content-Type").orElse(null))
        assertEquals(json, response.body())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `client applies delay to async request`() {
        val delayMs = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(
                Effects.latency(delayMs),
                Effects.status(202)
            )
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = HttpRequest.newBuilder()
            .uri(server.url("/test").toUri())
            .GET()
            .build()

        val start = System.currentTimeMillis()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join()
        val elapsed = System.currentTimeMillis() - start

        assertEquals(202, response.statusCode())
        assertTrue(elapsed >= delayMs, "Elapsed time $elapsed should be >= $delayMs")
        assertEquals(0, server.requestCount)
    }
}
