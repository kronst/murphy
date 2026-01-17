package io.murphy.core

import io.murphy.core.matcher.AllMatcher
import io.murphy.core.matcher.AnyMatcher
import io.murphy.core.matcher.HeaderMatcher
import io.murphy.core.matcher.MethodMatcher
import io.murphy.core.matcher.NoneMatcher
import io.murphy.core.matcher.PathMatcher

object Matchers {

    @JvmStatic
    fun path(pattern: String): Matcher = PathMatcher(pattern)

    @JvmStatic
    fun method(method: String): Matcher = MethodMatcher(method)

    @JvmStatic
    @JvmOverloads
    fun header(namePattern: String, valuePattern: String? = null): Matcher {
        return HeaderMatcher(namePattern = namePattern, valuePattern = valuePattern)
    }

    @JvmStatic
    fun not(matcher: Matcher): Matcher = !matcher

    @JvmStatic
    fun all(vararg matchers: Matcher): Matcher = AllMatcher(matchers.toList())

    @JvmStatic
    fun any(vararg matchers: Matcher): Matcher = AnyMatcher(matchers.toList())

    @JvmStatic
    fun none(vararg matchers: Matcher): Matcher = NoneMatcher(matchers.toList())

    @JvmStatic
    fun always(): Matcher = Matcher { true }

    @JvmStatic
    fun never(): Matcher = Matcher { false }
}
