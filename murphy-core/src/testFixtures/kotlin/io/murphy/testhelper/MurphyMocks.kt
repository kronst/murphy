package io.murphy.testhelper

import io.murphy.core.MurphyContext

object MurphyMocks {

    fun ctx(
        path: String = "/",
        method: String = "GET",
        headers: Map<String, List<String>> = emptyMap(),
    ): MurphyContext {
        return MurphyContext(
            url = "https://test.com$path",
            path = path,
            method = method,
            headers = headers,
        )
    }
}
