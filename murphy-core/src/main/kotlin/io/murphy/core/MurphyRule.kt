package io.murphy.core

class MurphyRule private constructor(
    val matcher: Matcher,
    val effects: List<Effect>,
) {

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var matcher = Matchers.always()
        private val effects = mutableListOf<Effect>()

        fun matches(matcher: Matcher) = apply {
            this.matcher = matcher
        }

        fun causes(effect: Effect) = apply {
            this.effects += effect
        }

        fun causes(effects: List<Effect>) = apply {
            this.effects += effects
        }

        fun causes(vararg effects: Effect) = apply {
            this.effects += effects
        }

        fun build(): MurphyRule {
            if (effects.isEmpty()) {
                throw IllegalStateException("Rule must have at least one effect")
            }

            return MurphyRule(matcher = matcher, effects = effects.toList())
        }
    }

    override fun toString(): String {
        return "Rule(matcher=$matcher, effects=$effects)"
    }
}
