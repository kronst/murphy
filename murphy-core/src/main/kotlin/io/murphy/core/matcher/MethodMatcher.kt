package io.murphy.core.matcher

import io.murphy.core.Matcher
import io.murphy.core.MurphyContext

class MethodMatcher(private val method: String) : Matcher {

    override fun matches(context: MurphyContext): Boolean {
        return context.method.equals(method, ignoreCase = true)
    }

    override fun toString() = "Method is '$method'"
}
