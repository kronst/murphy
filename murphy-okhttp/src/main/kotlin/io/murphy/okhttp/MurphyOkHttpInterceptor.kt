package io.murphy.okhttp

import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import io.murphy.core.MurphyScenario
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MurphyOkHttpInterceptor(
    private val scenario: MurphyScenario,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val context = MurphyContext(
            url = request.url.toString(),
            path = request.url.encodedPath,
            method = request.method,
            headers = request.headers.toMultimap(),
        )

        val rule = scenario.findRule(context)

        rule?.effects?.forEach { effect ->
            val murphyResponse = effect.apply(context)
            if (murphyResponse != null) {
                return murphyResponse.toResponse(request)
            }
        }

        return chain.proceed(request)
    }

    private fun MurphyResponse.toResponse(request: Request): Response {
        val contentType = headers["Content-Type"]?.firstOrNull()?.toMediaTypeOrNull()
        val responseBody = body.toResponseBody(contentType)

        val builder = Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(code)
            .message("Chaos Induced")
            .body(responseBody)

        headers.forEach { (name, values) ->
            values.forEach { value -> builder.addHeader(name = name, value = value) }
        }

        return builder.build()
    }
}
