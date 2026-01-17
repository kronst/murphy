package io.murphy.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MurphyRuleTest {

    @Test
    fun `builder fails without effects`() {
        assertFailsWith<IllegalStateException> {
            MurphyRule.builder()
                .matches(Matchers.always())
                .build()
        }
    }

    @Test
    fun `builder creates rule correctly`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.path("/api/*"))
            .causes(Effects.latency(1000))
            .causes(Effects.status(500))
            .build()

        assertEquals(2, rule.effects.size)

        val context = MurphyContext(
            url = "https://test.com",
            path = "/api/users",
            method = "GET",
            headers = emptyMap(),
        )

        assertTrue(rule.matcher.matches(context))
    }

    @Test
    fun `builder creates rule with list causes`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("POST"))
            .causes(
                listOf(
                    Effects.latency(500),
                    Effects.status(201),
                )
            )
            .build()

        assertEquals(2, rule.effects.size)
    }

    @Test
    fun `builder creates rule with vararg causes`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("GET"))
            .causes(
                Effects.latency(200),
                Effects.status(200),
            )
            .build()

        assertEquals(2, rule.effects.size)
    }
}
