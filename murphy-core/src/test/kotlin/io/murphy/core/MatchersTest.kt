package io.murphy.core

import io.murphy.testhelper.MurphyMocks.ctx
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchersTest {

    @ParameterizedTest(name = "Path ''{0}'' matches ''{1}'' should be {2}")
    @CsvSource(
        // Exact match
        "/api/users,      /api/users,       true",
        "/api/users,      /api/user,        false",
        "/api/users,      /api/users/,      false",

        // Single wildcard
        "/api/*/users,    /api/v1/users,    true",
        "/api/*/users,    /api/v1/v2/users, false",
        "/api/v1/*,       /api/v1/users,    true",
        "/api/v1/*,       /api/v1/,         true",

        // Greedy wildcard
        "/api/**,         /api/users,       true",
        "/api/**,         /api/v1/users,    true",
        "/static/**/img,  /static/a/b/img,  true",
        "**/metrics,      /api/v1/metrics,  true",

        // Single character wildcard
        "/v?/api,         /v1/api,          true",
        "/v?/api,         /v12/api,         false",
        "file?.txt,       fileA.txt,        true",

        // Mixed wildcards
        "/api/v*/**,      /api/v1/data/1,   true",
        "/*/users/id/**,  /corp/users/id/1, true",
    )
    fun `check path matching scenarios`(pattern: String, path: String, shouldMatch: Boolean) {
        val matcher = Matchers.path(pattern)
        val context = ctx(path = path)

        if (shouldMatch) {
            assertTrue(matcher.matches(context))
        } else {
            assertFalse(matcher.matches(context))
        }
    }

    @ParameterizedTest
    @CsvSource(
        nullValues = ["N/A"],
        value = [
            "Content-Type, application/json, Content-Type,       application/json,   true",
            "Content-Type, application/json, content-type,       application/json,   true",
            "Content-Type, application/json, Content-Type,       text/html,          false",
            "Content-Type, application/json, X-Other,            application/json,   false",
            "X-Murphy-*,   true,             X-Murphy-Enabled,   true,               true",
            "X-Murphy-*,   true,             x-murphy-debug,     true,               true",
            "X-Murphy-*,   true,             X-Other,            true,               false",
            "Content-Type, application/*,    Content-Type,       application/json,   true",
            "Content-Type, application/*,    Content-Type,       application/xml,    true",
            "Content-Type, application/*,    Content-Type,       text/html,          false",
            "X-Version,    v?,               X-Version,          v1,                 true",
            "X-Version,    v?,               X-Version,          v2,                 true",
            "X-Version,    v?,               X-Version,          v10,                false",
            "X-**-Key,     v**ue,            X-Some-Complex-Key, v-complex-ue,       true",
            "X-**-Key,     v**ue,            X-Some-Key,         val,                false",
            "X-Custom-*,   N/A,              X-Custom-Header,    anything,           true",
            "X-Custom-*,   N/A,              X-Other,            anything,           false"
        ]
    )
    fun `header matcher simple scenarios`(
        patternKey: String,
        patternValue: String?,
        actualKey: String,
        actualValue: String,
        expected: Boolean,
    ) {
        val matcher = Matchers.header(patternKey, patternValue)
        assertEquals(expected, matcher.matches(ctx(headers = mapOf(actualKey to listOf(actualValue)))))
    }

    @Test
    fun `headers with multiple values matches if at least one matches`() {
        val matcher = Matchers.header("Accept", "*json*")

        assertTrue(
            matcher.matches(
                ctx(
                    headers = mapOf(
                        "Accept" to listOf(
                            "text/html",
                            "application/xml",
                            "application/json",
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `headers with multiple values fails if none match`() {
        val matcher = Matchers.header("Accept", "*json*")
        val headers = mapOf("Accept" to listOf("text/html", "application/xml"))

        assertFalse(matcher.matches(ctx(headers = headers)))
    }

    @Test
    fun `method matcher is case insensitive`() {
        assertTrue(Matchers.method("GET").matches(ctx(method = "get")))
        assertTrue(Matchers.method("post").matches(ctx(method = "POST")))
    }

    @Test
    fun `path matcher is case insensitive`() {
        assertTrue(Matchers.path("/api/users").matches(ctx(path = "/API/users")))
        assertTrue(Matchers.path("/api/users").matches(ctx(path = "/api/Users")))
    }

    @Test
    fun `header matcher is case insensitive`() {
        assertTrue(
            Matchers.header("accept", "*Json*")
                .matches(ctx(headers = mapOf("Accept" to listOf("text/html", "application/json"))))
        )
    }

    @Test
    fun `ALL logic works correctly`() {
        val matcher = Matchers.all(
            Matchers.method("POST"),
            Matchers.path("/api/users"),
        )

        assertTrue(matcher.matches(ctx(method = "POST", path = "/api/users")))
        assertFalse(matcher.matches(ctx(method = "GET", path = "/api/users")))
        assertFalse(matcher.matches(ctx(method = "POST", path = "/other")))
        assertTrue(Matchers.all().matches(ctx()))
    }

    @Test
    fun `ANY logic works correctly`() {
        val matcher = Matchers.any(
            Matchers.method("POST"),
            Matchers.path("/home"),
        )

        assertTrue(matcher.matches(ctx(method = "POST", path = "/other")))
        assertTrue(matcher.matches(ctx(method = "GET", path = "/home")))
        assertFalse(matcher.matches(ctx(method = "GET", path = "/other")))
        assertFalse(Matchers.any().matches(ctx()))
    }

    @Test
    fun `NONE logic works correctly`() {
        val matcher = Matchers.none(
            Matchers.method("DELETE"),
            Matchers.path("/admin/**"),
        )

        assertTrue(matcher.matches(ctx(method = "POST", path = "/api/users")))
        assertFalse(matcher.matches(ctx(method = "DELETE", path = "/api/users")))
        assertFalse(matcher.matches(ctx(method = "POST", path = "/admin/users")))
        assertTrue(Matchers.none().matches(ctx()))
    }

    @Test
    fun `NOT logic works correctly`() {
        val matcher = Matchers.not(Matchers.method("GET"))

        assertTrue(matcher.matches(ctx(method = "POST")))
        assertFalse(matcher.matches(ctx(method = "GET")))
    }

    @Test
    fun `ALWAYS logic works correctly`() {
        assertTrue(Matchers.always().matches(ctx()))
    }

    @Test
    fun `NEVER logic works correctly`() {
        assertFalse(Matchers.never().matches(ctx()))
    }

    @Test
    fun `complex logic works correctly`() {
        val matcher = Matchers.all(
            Matchers.any(
                Matchers.method("POST"),
                Matchers.method("PUT"),
                Matchers.method("DELETE"),
            ),

            Matchers.path("/api/**"),

            Matchers.none(
                Matchers.path("/api/admin/**"),
                Matchers.path("/api/health"),
                Matchers.path("/api/metrics"),
            ),

            Matchers.any(
                Matchers.header("X-Beta-User"),
                Matchers.header("X-Canary", "true"),
            ),

            Matchers.not(
                Matchers.header("X-Blacklist-User"),
            ),
        )

        // Positive case
        assertTrue(
            matcher.matches(
                ctx(
                    method = "POST",
                    path = "/api/v1/orders",
                    headers = mapOf("X-Beta-User" to listOf("1"))
                )
            )
        )

        // GET is not allowed
        assertFalse(
            matcher.matches(
                ctx(
                    method = "GET",
                    path = "/api/v1/orders",
                    headers = mapOf("X-Beta-User" to listOf("1"))
                )
            )
        )

        // Admin paths are excluded
        assertFalse(
            matcher.matches(
                ctx(
                    method = "DELETE",
                    path = "/api/admin/users",
                    headers = mapOf("X-Beta-User" to listOf("1"))
                )
            )
        )

        // Health check is excluded
        assertFalse(
            matcher.matches(
                ctx(
                    method = "GET",
                    path = "/api/health",
                    headers = mapOf("X-Canary" to listOf("true"))
                )
            )
        )

        // Missing required headers
        assertFalse(
            matcher.matches(
                ctx(
                    method = "PUT",
                    path = "/api/v1/profile",
                    headers = emptyMap()
                )
            )
        )

        // Blacklisted user
        assertFalse(
            matcher.matches(
                ctx(
                    method = "POST",
                    path = "/api/v1/data",
                    headers = mapOf(
                        "X-Beta-User" to listOf("1"),
                        "X-Blacklist-User" to listOf("1"),
                    )
                )
            )
        )
    }
}
