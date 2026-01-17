package io.murphy.core.matcher

import io.murphy.core.Matcher
import io.murphy.core.MurphyContext
import io.murphy.core.internal.GlobCompiler

class HeaderMatcher(
    private val keyPattern: String,
    private val valuePattern: String? = null,
) : Matcher {
    private val keyRegex = GlobCompiler.compile(pattern = keyPattern, isPath = false)
    private val valueRegex = valuePattern?.let { GlobCompiler.compile(pattern = it, isPath = false) }

    override fun matches(context: MurphyContext): Boolean {
        return context.headers.any { (key, values) ->
            if (!keyRegex.matcher(key).matches()) {
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
            "Header '$keyPattern' matches value '$valuePattern'"
        } else {
            "Header '$keyPattern' exists"
        }
    }
}
