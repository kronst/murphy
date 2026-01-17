package io.murphy.core

import io.murphy.testhelper.MurphyMocks.ctx
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
}
