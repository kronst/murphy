package io.murphy.spring.restclient

import io.murphy.core.MurphyResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpResponse
import java.io.ByteArrayInputStream
import java.io.InputStream

internal class MurphyClientHttpResponse(
    private val response: MurphyResponse,
) : ClientHttpResponse {

    override fun getStatusCode(): HttpStatusCode = HttpStatusCode.valueOf(response.code)

    override fun getStatusText(): String = "Chaos Induced"

    override fun getHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            response.headers.forEach { (name, values) ->
                addAll(name, values)
            }
        }
    }

    override fun getBody(): InputStream = ByteArrayInputStream(response.body)

    override fun close() {
        /* no-op */
    }
}
