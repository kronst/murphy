package io.murphy.core.internal

import java.util.regex.Pattern

internal object GlobCompiler {

    fun compile(pattern: String, isPath: Boolean): Pattern {
        val sb = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            when (val c = pattern[i]) {
                '*' -> {
                    // greedy wildcard
                    if (pattern.startsWith("**", i)) {
                        sb.append(".*")
                        i += 2
                    } else {
                        // exclude slash for path patterns
                        sb.append(if (isPath) "[^/]*" else ".*")
                        i++
                    }
                }
                '?' -> {
                    // single character wildcard
                    sb.append('.')
                    i++
                }
                else -> {
                    // escape regex special characters
                    if (".+[]{}()|^$\\".indexOf(c) != -1) sb.append('\\')
                    sb.append(c)
                    i++
                }
            }
        }
        sb.append("$")

        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE)
    }
}
