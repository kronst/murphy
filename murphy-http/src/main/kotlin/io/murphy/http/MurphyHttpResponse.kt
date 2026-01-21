package io.murphy.http

import io.murphy.core.MurphyResponse
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import javax.net.ssl.SSLSession

class MurphyHttpResponse<T : Any>(
    private val murphyResponse: MurphyResponse,
    private val request: HttpRequest,
    private val body: T,
) : HttpResponse<T> {

    override fun statusCode(): Int = murphyResponse.code

    override fun request(): HttpRequest = request

    override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()

    override fun headers(): HttpHeaders {
        return HttpHeaders.of(murphyResponse.headers) { _, _ -> true }
    }

    override fun body(): T = body

    override fun sslSession(): Optional<SSLSession> = Optional.empty()

    override fun uri(): URI = request.uri()

    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
}
