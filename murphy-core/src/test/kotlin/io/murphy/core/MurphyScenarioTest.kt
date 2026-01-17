package io.murphy.core

import io.murphy.testhelper.MurphyMocks.ctx
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class MurphyScenarioTest {

    @Test
    fun `findRule returns null when scenario is empty`() {
        val scenario = MurphyScenario.from()

        val rule = scenario.findRule(ctx())

        assertNull(rule)
    }

    @Test
    fun `findRule returns matching rule`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.path("/test"))
            .causes(Effects.status(200))
            .build()

        val scenario = MurphyScenario.from(rule)

        val found = scenario.findRule(ctx(path = "/test"))

        assertNotNull(found)
        assertSame(rule, found)
    }

    @Test
    fun `findRule returns first matching rule`() {
        val rule1 = MurphyRule.builder()
            .matches(Matchers.path("/test"))
            .causes(Effects.status(200))
            .build()

        val rule2 = MurphyRule.builder()
            .matches(Matchers.path("/test"))
            .causes(Effects.status(404))
            .build()

        val scenario = MurphyScenario.from(rule1, rule2)

        val found = scenario.findRule(ctx(path = "/test"))

        assertNotNull(found)
        assertSame(rule1, found)
    }

    @Test
    fun `findRule returns null when no rules match`() {
        val scenario = MurphyScenario.from(
            MurphyRule.builder()
                .matches(Matchers.path("/foo"))
                .causes(Effects.status(200))
                .build()
        )

        val found = scenario.findRule(ctx(path = "/bar"))

        assertNull(found)
    }

    @Test
    fun `findRule skips non-matching rules`() {
        val rule1 = MurphyRule.builder()
            .matches(Matchers.path("/foo"))
            .causes(Effects.status(200))
            .build()

        val rule2 = MurphyRule.builder()
            .matches(Matchers.path("/bar"))
            .causes(Effects.status(404))
            .build()

        val scenario = MurphyScenario.from(rule1, rule2)

        val found = scenario.findRule(ctx(path = "/bar"))

        assertNotNull(found)
        assertSame(rule2, found)
    }
}
