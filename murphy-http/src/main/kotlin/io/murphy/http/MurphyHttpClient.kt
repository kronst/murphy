package io.murphy.http

import io.murphy.core.Effect
import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import io.murphy.core.MurphyScenario
import io.murphy.core.effect.DelayEffect
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

class MurphyHttpClient(
    private val delegate: HttpClient,
    private val scenario: MurphyScenario,
) : HttpClient() {

    override fun <T : Any> send(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>
    ): HttpResponse<T> {
        val context = request.toMurphyContext()
        val rule = scenario.findRule(context)

        rule?.effects?.forEach { effect ->
            val murphyResponse = effect.apply(context)
            if (murphyResponse != null) {
                return murphyResponse.toHttpResponse(request = request, responseBodyHandler = responseBodyHandler)
            }
        }

        return delegate.send(request, responseBodyHandler)
    }

    override fun <T : Any> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>
    ): CompletableFuture<HttpResponse<T>> {
        val context = request.toMurphyContext()
        val rule = scenario.findRule(context) ?: return delegate.sendAsync(request, responseBodyHandler)

        var future = CompletableFuture.completedFuture<MurphyResponse?>(null)

        rule.effects.forEach { effect ->
            future = future.thenCompose { murphyResponse ->
                murphyResponse?.let { CompletableFuture.completedFuture(it) }
                    ?: applyEffectAsync(effect = effect, context = context)
            }
        }

        return future.thenCompose { murphyResponse ->
            murphyResponse?.toHttpResponseAsync(request, responseBodyHandler)
                ?: delegate.sendAsync(request, responseBodyHandler)
        }
    }

    override fun <T : Any> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
        pushPromiseHandler: HttpResponse.PushPromiseHandler<T>,
    ): CompletableFuture<HttpResponse<T>> {
        val context = request.toMurphyContext()
        if (scenario.findRule(context) != null) {
            return sendAsync(request = request, responseBodyHandler = responseBodyHandler)
        }
        return delegate.sendAsync(request, responseBodyHandler, pushPromiseHandler)
    }

    override fun cookieHandler(): Optional<CookieHandler> = delegate.cookieHandler()
    override fun connectTimeout(): Optional<Duration> = delegate.connectTimeout()
    override fun followRedirects(): Redirect = delegate.followRedirects()
    override fun proxy(): Optional<ProxySelector> = delegate.proxy()
    override fun sslContext(): SSLContext = delegate.sslContext()
    override fun sslParameters(): SSLParameters = delegate.sslParameters()
    override fun authenticator(): Optional<Authenticator> = delegate.authenticator()
    override fun version(): Version = delegate.version()
    override fun executor(): Optional<Executor> = delegate.executor()

    private fun applyEffectAsync(
        effect: Effect,
        context: MurphyContext,
    ): CompletableFuture<MurphyResponse?> {
        if (effect is DelayEffect) {
            val d = effect.duration
            if (d <= 0) return CompletableFuture.completedFuture(null)

            val delayedExecutor = executor()
                .map { CompletableFuture.delayedExecutor(d, TimeUnit.MILLISECONDS, it) }
                .orElseGet { CompletableFuture.delayedExecutor(d, TimeUnit.MILLISECONDS) }

            return CompletableFuture.supplyAsync({ null }, delayedExecutor)
        }

        return executor()
            .map { CompletableFuture.supplyAsync({ effect.apply(context) }, it) }
            .orElseGet { CompletableFuture.supplyAsync { effect.apply(context) } }
    }

    private fun HttpRequest.toMurphyContext(): MurphyContext {
        return MurphyContext(
            url = uri().toString(),
            path = uri().path,
            method = method(),
            headers = headers().map(),
        )
    }

    private fun <T : Any> MurphyResponse.toHttpResponse(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
    ): MurphyHttpResponse<T> {
        val responseInfo = MurphyResponseInfo(this)
        val subscriber = responseBodyHandler.apply(responseInfo)
        subscriber.onSubscribe(MurphySubscription(subscriber = subscriber, body = body))

        return MurphyHttpResponse(
            murphyResponse = this,
            request = request,
            body = subscriber.body.toCompletableFuture().join(),
        )
    }

    private fun <T : Any> MurphyResponse.toHttpResponseAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
    ): CompletableFuture<HttpResponse<T>> {
        val responseInfo = MurphyResponseInfo(this)
        val subscriber = responseBodyHandler.apply(responseInfo)
        subscriber.onSubscribe(MurphySubscription(subscriber = subscriber, body = body))

        return subscriber.body.toCompletableFuture().thenApply { body ->
            MurphyHttpResponse(
                murphyResponse = this,
                request = request,
                body = body,
            )
        }
    }

    private class MurphyResponseInfo(
        private val response: MurphyResponse,
    ) : HttpResponse.ResponseInfo {
        override fun statusCode(): Int = response.code
        override fun headers(): HttpHeaders = HttpHeaders.of(response.headers) { _, _ -> true }
        override fun version(): Version = Version.HTTP_1_1
    }

    private class MurphySubscription(
        private val subscriber: Flow.Subscriber<in List<ByteBuffer>>,
        private val body: ByteArray,
    ) : Flow.Subscription {
        private var completed = false

        override fun request(n: Long) {
            if (n > 0 && !completed) {
                completed = true
                if (body.isNotEmpty()) {
                    subscriber.onNext(listOf(ByteBuffer.wrap(body)))
                }
                subscriber.onComplete()
            }
        }

        override fun cancel() {
            completed = true
        }
    }

    companion object {
        @JvmStatic
        fun decorate(client: HttpClient, scenario: MurphyScenario): MurphyHttpClient {
            return MurphyHttpClient(delegate = client, scenario = scenario)
        }
    }
}
