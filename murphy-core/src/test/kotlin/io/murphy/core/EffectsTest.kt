package io.murphy.core

import io.murphy.core.Effects.withProbability
import io.murphy.core.effect.ProbabilisticDelayEffect
import io.murphy.core.effect.ProbabilisticEffect
import io.murphy.testhelper.MurphyMocks.ctx
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectsTest {

    @Test
    fun `status effect returns response with code`() {
        val effect = Effects.status(418)

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(418, response.code)
        assertEquals(0, response.body.size)
    }

    @Test
    fun `json effect sets body and content type correctly`() {
        val json = """{"foo":"bar"}"""
        val effect = Effects.json(code = 201, body = json)

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(201, response.code)
        assertEquals(json, response.body.decodeToString())
        assertTrue(response.headers.containsKey("Content-Type"))
        assertEquals(listOf("application/json"), response.headers["Content-Type"])
    }

    @Test
    fun `json effect with default status works correctly`() {
        val json = """{"foo":"bar"}"""
        val effect = Effects.json(body = json)

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(200, response.code)
        assertEquals(json, response.body.decodeToString())
        assertTrue(response.headers.containsKey("Content-Type"))
        assertEquals(listOf("application/json"), response.headers["Content-Type"])
    }

    @Test
    fun `latency effect with millis returns null response`() {
        val effect = Effects.latency(10)

        val response = effect.apply(ctx())

        assertNull(response)
    }

    @Test
    fun `latency effect with unit returns null response`() {
        val effect = Effects.latency(1000, TimeUnit.NANOSECONDS)

        val response = effect.apply(ctx())

        assertNull(response)
    }

    @Test
    fun `jitter effect with millis returns null response`() {
        val effect = Effects.jitter(10, 20)

        val response = effect.apply(ctx())

        assertNull(response)
    }

    @Test
    fun `jitter effect with unit returns null response`() {
        val effect = Effects.jitter(1000, 2000, TimeUnit.NANOSECONDS)

        val response = effect.apply(ctx())

        assertNull(response)
    }

    @Test
    fun `crash effect throws exception with specified message`() {
        val effect = Effects.crash("Test crash")

        val exception = assertFailsWith<IOException> {
            effect.apply(ctx())
        }

        assertEquals("Test crash", exception.message)
    }

    @Test
    fun `crash effect throws exception with default message`() {
        val effect = Effects.crash()

        val exception = assertFailsWith<IOException> {
            effect.apply(ctx())
        }

        assertEquals("Murphy induced crash", exception.message)
    }

    @Test
    fun `crash effect throws specified exception`() {
        val effect = Effects.crash(RuntimeException("Custom exception"))

        val exception = assertFailsWith<RuntimeException> {
            effect.apply(ctx())
        }

        assertEquals("Custom exception", exception.message)
    }

    @Test
    fun `response effect returns response`() {
        val effect = Effects.response(
            code = 429,
            body = "Too Many Requests".toByteArray(),
            headers = mapOf(
                "Retry-After" to listOf("30"),
                "X-Custom-Header" to listOf("Custom1", "Custom2"),
            ),
        )

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(429, response.code)
        assertEquals("Too Many Requests", response.body.decodeToString())
        assertTrue(response.headers.containsKey("Retry-After"))
        assertEquals(listOf("30"), response.headers["Retry-After"])
        assertTrue(response.headers.containsKey("X-Custom-Header"))
        assertEquals(listOf("Custom1", "Custom2"), response.headers["X-Custom-Header"])
    }

    @Test
    fun `response effect returns empty headers by default`() {
        val effect = Effects.response(code = 200, body = "OK".toByteArray())

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(200, response.code)
        assertEquals("OK", response.body.decodeToString())
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `response effect returns empty body by default`() {
        val effect = Effects.response(code = 204)

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(204, response.code)
        assertEquals(0, response.body.size)
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `response effect returns success response by default`() {
        val effect = Effects.response()

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(200, response.code)
        assertEquals(0, response.body.size)
        assertTrue(response.headers.isEmpty())
    }

    @Test
    fun `default probability is 1_0`() {
        assertEquals(1.0, Effects.status(200).probability())
    }

    @Test
    fun `withProbability 1_0 always applies effect`() {
        val effect = Effects.status(200).withProbability(1.0)

        val response = effect.apply(ctx())

        assertNotNull(response)
        assertEquals(200, response.code)
        assertEquals(1.0, effect.probability())
    }

    @Test
    fun `withProbability 0_0 never applies effect`() {
        val effect = Effects.status(200).withProbability(0.0)

        val response = effect.apply(ctx())

        assertNull(response)
        assertEquals(0.0, effect.probability())
    }

    @Test
    fun `withProbability coerces values to 0_0-1_0 range`() {
        assertEquals(0.0, Effects.status(200).withProbability(-0.1).probability())
        assertEquals(1.0, Effects.status(200).withProbability(1.1).probability())
    }

    @Test
    fun `withProbability returns probabilistic effects`() {
        assertIs<ProbabilisticEffect>(Effects.status(200).withProbability(0.75))
        assertIs<ProbabilisticEffect>(Effects.json(body = "{}").withProbability(0.25))
        assertIs<ProbabilisticEffect>(Effects.crash("Error").withProbability(0.9))
        assertIs<ProbabilisticEffect>(Effects.response().withProbability(0.1))
    }

    @Test
    fun `withProbability returns probabilistic delayed effects for latency`() {
        val effect = Effects.latency(100).withProbability(1.0)

        assertIs<ProbabilisticDelayEffect>(effect)
        assertEquals(1.0, effect.probability())
        assertEquals(100, effect.duration)
    }

    @Test
    fun `withProbability returns probabilistic delayed effects for jitter`() {
        val effect = Effects.jitter(10, 20).withProbability(0.5)

        assertIs<ProbabilisticDelayEffect>(effect)
        assertEquals(0.5, effect.probability())
    }
}
