package io.murphy.okhttp

import io.murphy.core.Effects
import io.murphy.core.Effects.withProbability
import io.murphy.core.Matchers
import io.murphy.core.MurphyRule
import io.murphy.core.MurphyScenario
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MurphyOkHttpInterceptorTest {

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

    private fun buildClient(scenario: MurphyScenario): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(MurphyOkHttpInterceptor(scenario))
            .build()
    }

    @Test
    fun `interceptor blocks request and returns response`() {
        val json = """{"message": "I'm a teapot"}"""
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.json(code = 418, json))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = Request.Builder().url(server.url("/test")).build()

        client.newCall(request).execute().use { response ->
            assertEquals(418, response.code)
            assertEquals("application/json".toMediaType(), response.body?.contentType())
            assertEquals(json, response.body?.string())
        }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `interceptor applies delay before proceeding to network`() {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.latency(delay))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = Request.Builder().url(server.url("/test")).build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        val start = System.currentTimeMillis()
        client.newCall(request).execute().use { response ->
            val elapsed = System.currentTimeMillis() - start
            assertEquals(200, response.code)
            assertEquals("Mock Server Response", response.body?.string())
            assert(elapsed >= delay)
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `interceptor applies delay before returning response`() {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(
                Effects.latency(delay),
                Effects.status(202),
            )
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = Request.Builder().url(server.url("/test")).build()

        val start = System.currentTimeMillis()
        client.newCall(request).execute().use { response ->
            val elapsed = System.currentTimeMillis() - start
            assertEquals(202, response.code)
            assert(elapsed >= delay)
        }

        assertEquals(0, server.requestCount)
    }

    @Test
    fun `interceptor proceeds to network if matcher does not match`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("POST"))
            .causes(Effects.status(500))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = Request.Builder().url(server.url("/test")).build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("Mock Server Response", response.body?.string())
        }

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `interceptor proceeds to network if probability is not met`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.status(500).withProbability(0.0))
            .build()

        val client = buildClient(MurphyScenario.from(rule))
        val request = Request.Builder().url(server.url("/test")).build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("Mock Server Response"))

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("Mock Server Response", response.body?.string())
        }

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

        client.newCall(Request.Builder().url(server.url("/api/foo")).build()).execute().use { response ->
            assertEquals(201, response.code)
        }

        client.newCall(Request.Builder().url(server.url("/api/bar")).build()).execute().use { response ->
            assertEquals(500, response.code)
        }
    }
}
