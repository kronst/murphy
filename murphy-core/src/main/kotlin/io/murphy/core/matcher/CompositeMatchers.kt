package io.murphy.core.matcher

import io.murphy.core.Matcher
import io.murphy.core.MurphyContext

/**
 * Composite matcher that matches if all contained matchers match.
 * Returns true if all matchers return true. Returns true for an empty list.
 */
internal class AllMatcher(private val matchers: List<Matcher>) : Matcher {
    override fun matches(context: MurphyContext) = matchers.all { it.matches(context) }
    override fun toString() = "ALL(${matchers.joinToString(", ")})"
}

/**
 * Composite matcher that matches if any contained matcher matches.
 * Returns true if at least one matcher returns true. Returns false for an empty list.
 */
internal class AnyMatcher(private val matchers: List<Matcher>) : Matcher {
    override fun matches(context: MurphyContext) = matchers.any { it.matches(context) }
    override fun toString() = "ANY(${matchers.joinToString(", ")})"
}

/**
 * Composite matcher that matches if none of the contained matchers match.
 * Returns true if all matchers return false. Returns true for an empty list.
 */
internal class NoneMatcher(private val matchers: List<Matcher>) : Matcher {
    override fun matches(context: MurphyContext) = matchers.none { it.matches(context) }
    override fun toString() = "NONE(${matchers.joinToString(", ")})"
}

/**
 * Composite matcher that negates the result of the contained matcher.
 */
internal class NotMatcher(private val matcher: Matcher) : Matcher {
    override fun matches(context: MurphyContext) = !matcher.matches(context)
    override fun toString() = "NOT($matcher)"
}
