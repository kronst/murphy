package io.murphy.core

/**
 * Represents a scenario consisting of multiple [MurphyRule]s.
 *
 * This class follows the "First Match Wins" strategy, where the first rule that matches the given [MurphyContext]
 */
class MurphyScenario(
    private val rules: List<MurphyRule>,
) {

    /**
     * Finds the **first** [MurphyRule] that matches the given [context].
     */
    fun findRule(context: MurphyContext): MurphyRule? {
        return rules.find { rule -> rule.matcher.matches(context) }
    }

    companion object {
        @JvmStatic
        fun from(vararg rules: MurphyRule): MurphyScenario = MurphyScenario(rules = rules.toList())

        @JvmStatic
        fun from(rules: List<MurphyRule>): MurphyScenario = MurphyScenario(rules = rules)
    }
}
