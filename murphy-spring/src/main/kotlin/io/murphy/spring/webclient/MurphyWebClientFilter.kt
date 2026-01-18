package io.murphy.spring.webclient

import io.murphy.core.MurphyContext
import io.murphy.core.MurphyResponse
import io.murphy.core.MurphyScenario
import io.murphy.core.effect.DelayEffect
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.Optional

class MurphyWebClientFilter(
    private val scenario: MurphyScenario,
) : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val headers = mutableMapOf<String, List<String>>()
        request.headers().forEach { name, values ->
            headers[name] = values
        }

        val context = MurphyContext(
            url = request.url().toString(),
            path = request.url().path,
            method = request.method().name(),
            headers = headers,
        )

        val rule = scenario.findRule(context) ?: return next.exchange(request)

        var pipeline: Mono<Optional<MurphyResponse>> = Mono.just(Optional.empty())

        rule.effects.forEach { effect ->
            pipeline = pipeline.flatMap { previousResponse ->
                // Any previous response short-circuits the chain
                if (previousResponse.isPresent) {
                    Mono.just(previousResponse)
                } else {
                    Mono.fromCallable { Optional.ofNullable(effect.apply(context)) }.let { mono ->
                        // Execute in a bounded elastic scheduler to avoid blocking for delay effects
                        if (effect is DelayEffect) {
                            mono.subscribeOn(Schedulers.boundedElastic())
                        } else {
                            mono
                        }
                    }
                }
            }
        }

        return pipeline.flatMap { optionalResponse ->
            if (optionalResponse.isPresent) {
                val murphyResponse = optionalResponse.get()
                Mono.just(
                    ClientResponse.create(HttpStatusCode.valueOf(murphyResponse.code))
                        .headers { h -> murphyResponse.headers.forEach { (name, values) -> h.addAll(name, values) } }
                        .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(murphyResponse.body)))
                        .build()
                )
            } else {
                next.exchange(request)
            }
        }
    }
}
