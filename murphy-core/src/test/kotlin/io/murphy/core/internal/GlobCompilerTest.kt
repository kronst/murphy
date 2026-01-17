package io.murphy.core.internal

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobCompilerTest {

    @ParameterizedTest(name = "Path pattern=\"{0}\", path=\"{1}\", shouldMatch={2}")
    @CsvSource(
        // Basic wildcards
        "/api/*, /api/users, true",
        "/api/*, /api/users/123, false",
        "/api/**, /api/users/123, true",

        // Single character wildcard
        "/api/user?, /api/users, true",
        "/api/user?, /api/user, false",
        "file?.txt, file1.txt, true",
        "file?.txt, file12.txt, false",

        // Mixed
        "/files/*/images/**, /files/user/images/photo.jpg, true",
        "/files/*/images/**, /files/user/docs/photo.jpg, false",
        "/img/*.png, /img/photo.png, true",
        "/img/*.png, /img/photo.jpg, false",
        "/docs/??.pdf, /docs/ab.pdf, true",
        "/docs/??.pdf, /docs/a.pdf, false",

        // Exact match
        "/login, /login, true",
        "/login, /logout, false",

        // Case insensitivity
        "/API/USERS, /api/users, true",
        "/Api/*, /api/Items, true",

        // Edge cases
        "*, /anything/here, false",
        "**, /anything/here, true",
    )
    fun `should match paths correctly`(pattern: String, path: String, shouldMatch: Boolean) {
        val regex = GlobCompiler.compile(pattern = pattern, isPath = true)
        val matches = regex.matcher(path).matches()

        if (shouldMatch) {
            assertTrue(matches)
        } else {
            assertFalse(matches)
        }
    }

    @ParameterizedTest(name = "Header pattern=\"{0}\", header=\"{1}\", shouldMatch={2}")
    @CsvSource(
        // Basic wildcards
        "application/*, application/json, true",
        "application/*, text/html, false",
        "X-Custom-*, X-Custom-Header, true",
        "X-Custom-*, X-Other-Header, false",
        "X-*-Token, X-Auth-Token, true",
        "X-*-Token, X-Auth-Header, false",
        "text/**, text/html; charset=UTF-8, true",
        "text/**, application/json, false",

        // Single character wildcard
        "Content-???, Content-123, true",
        "Content-???, Content-12, false",
        "X-?-Header, X-A-Header, true",
        "X-?-Header, X-AB-Header, false",

        // Case insensitivity
        "content-type, Content-Type, true",
        "X-CUSTOM-HEADER, x-custom-header, true",

        // Edge cases
        "*, any-header, true",
        "**, any-header, true",
    )
    fun `should match headers correctly`(pattern: String, header: String, shouldMatch: Boolean) {
        val regex = GlobCompiler.compile(pattern = pattern, isPath = false)
        val matches = regex.matcher(header).matches()

        if (shouldMatch) {
            assertTrue(matches)
        } else {
            assertFalse(matches)
        }
    }
}
