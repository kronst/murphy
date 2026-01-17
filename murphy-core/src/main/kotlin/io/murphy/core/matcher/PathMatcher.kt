package io.murphy.core.matcher

import io.murphy.core.Matcher
import io.murphy.core.MurphyContext
import io.murphy.core.internal.GlobCompiler

class PathMatcher(pattern: String) : Matcher {
    private val regex = GlobCompiler.compile(pattern = pattern, isPath = true)

    override fun matches(context: MurphyContext): Boolean {
        return regex.matcher(context.path).matches()
    }

    override fun toString() = "Path matches '${regex.pattern()}'"
}
