package io.murphy.core

import io.murphy.core.matcher.AllMatcher
import io.murphy.core.matcher.AnyMatcher
import io.murphy.core.matcher.NotMatcher

fun interface Matcher {
    fun matches(context: MurphyContext): Boolean

    infix fun and(other: Matcher): Matcher = AllMatcher(listOf(this, other))
    infix fun or(other: Matcher): Matcher = AnyMatcher(listOf(this, other))

    operator fun not(): Matcher = NotMatcher(this)
}
