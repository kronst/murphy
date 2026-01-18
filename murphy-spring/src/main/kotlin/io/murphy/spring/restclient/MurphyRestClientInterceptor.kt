package io.murphy.spring.restclient

import io.murphy.core.MurphyContext
import io.murphy.core.MurphyScenario
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class MurphyRestClientInterceptor(
    private val scenario: MurphyScenario,
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val headers = mutableMapOf<String, List<String>>()
        request.headers.forEach { name, values ->
            headers[name] = values
        }

        val context = MurphyContext(
            url = request.uri.toString(),
            path = request.uri.path,
            method = request.method.name(),
            headers = headers,
        )

        val rule = scenario.findRule(context)

        rule?.effects?.forEach { effect ->
            val murphyResponse = effect.apply(context)
            if (murphyResponse != null) {
                return MurphyClientHttpResponse(murphyResponse)
            }
        }

        return execution.execute(request, body)
    }
}
