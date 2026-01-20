package io.murphy.ktor

import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import io.murphy.core.effect.DelayEffect
import kotlinx.coroutines.delay

val MurphyPlugin = createClientPlugin("MurphyPlugin", ::MurphyPluginConfig) {
    val scenario = pluginConfig.scenario ?: return@createClientPlugin

    on(Send) { request ->
        val context = MurphyContext(
            url = request.url.toString(),
            path = request.url.encodedPath,
            method = request.method.value,
            headers = request.headers.build().toMap(),
        )

        val rule = scenario.findRule(context) ?: return@on proceed(request)

        rule.effects.forEach { effect ->
            if (effect is DelayEffect) {
                delay(effect.duration)
            } else {
                val murphyResponse = effect.apply(context)
                if (murphyResponse != null) {

                    @OptIn(InternalAPI::class)
                    return@on HttpClientCall(
                        client = client,
                        requestData = request.build(),
                        responseData = murphyResponse.toHttpResponseData(request),
                    )
                }
            }
        }

        proceed(request)
    }
}

private fun MurphyResponse.toHttpResponseData(request: HttpRequestBuilder): HttpResponseData {
    return HttpResponseData(
        statusCode = HttpStatusCode.fromValue(code),
        requestTime = GMTDate(),
        headers = Headers.build {
            headers.forEach { (name, values) ->
                appendAll(name, values)
            }
        },
        version = HttpProtocolVersion.HTTP_1_1,
        body = ByteReadChannel(body),
        callContext = request.executionContext,
    )
}
