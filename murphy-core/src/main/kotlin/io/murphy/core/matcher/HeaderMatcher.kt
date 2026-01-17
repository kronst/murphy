package io.murphy.core.matcher

import io.murphy.core.Matcher
import io.murphy.core.MurphyContext
import io.murphy.core.internal.GlobCompiler

class HeaderMatcher(
    private val namePattern: String,
    private val valuePattern: String? = null,
) : Matcher {
    private val nameRegex = GlobCompiler.compile(pattern = namePattern, isPath = false)
    private val valueRegex = valuePattern?.let { GlobCompiler.compile(pattern = it, isPath = false) }

    override fun matches(context: MurphyContext): Boolean {
        return context.headers.any { (name, values) ->
            if (!nameRegex.matcher(name).matches()) {
                return@any false
            }

            if (valueRegex == null) {
                return@any true
            }

            values.any { value -> valueRegex.matcher(value).matches() }
        }
    }

    override fun toString(): String {
        return if (valuePattern != null) {
            "Header '$namePattern' matches value '$valuePattern'"
        } else {
            "Header '$namePattern' exists"
        }
    }
}
