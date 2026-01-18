package io.murphy.spring.restclient

import io.murphy.core.Effects
import io.murphy.core.Effects.withProbability
import io.murphy.core.Matchers
import io.murphy.core.MurphyRule
import io.murphy.core.MurphyScenario
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class MurphyRestClientInterceptorTest {

    @Test
    fun `interceptor blocks request and returns response`() {
        val json = """{"message": "I'm a teapot"}"""
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.json(code = 418, json))
            .build()

        val client = RestClient.builder()
            .requestInterceptor(MurphyRestClientInterceptor(MurphyScenario.from(rule)))
            .build()

        val response = client.get()
            .uri("/test")
            .exchange { _, res -> res }

        assertEquals(418, response.statusCode.value())
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(json, response.bodyTo(String::class.java))
    }

    @Test
    fun `interceptor applies delay before proceeding to network`() {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.latency(delay))
            .build()

        val builder = RestClient.builder()
            .requestInterceptor(MurphyRestClientInterceptor(MurphyScenario.from(rule)))

        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        server.expect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess())

        val start = System.currentTimeMillis()
        client.get().uri("/test").retrieve().toBodilessEntity()
        val elapsed = System.currentTimeMillis() - start

        assert(elapsed >= delay)
        server.verify()
    }

    @Test
    fun `interceptor applies delay before returning response`() {
        val delay = 100L
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(
                Effects.latency(delay),
                Effects.status(202),
            )
            .build()

        val builder = RestClient.builder()
            .requestInterceptor(MurphyRestClientInterceptor(MurphyScenario.from(rule)))

        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        val start = System.currentTimeMillis()
        val response = client.get().uri("/test").exchange { _, res -> res }
        val elapsed = System.currentTimeMillis() - start

        assertEquals(202, response.statusCode.value())
        assert(elapsed >= delay)
        server.verify()
    }

    @Test
    fun `interceptor proceeds to network if matcher does not match`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.method("POST"))
            .causes(Effects.status(500))
            .build()

        val builder = RestClient.builder()
            .requestInterceptor(MurphyRestClientInterceptor(MurphyScenario.from(rule)))

        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        server.expect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess("Mock Server Response", MediaType.TEXT_PLAIN))

        val response = client.get().uri("/test").exchange { _, res -> res }

        assertEquals(200, response.statusCode.value())
        assertEquals("Mock Server Response", response.bodyTo(String::class.java))
        server.verify()
    }

    @Test
    fun `interceptor proceeds to network if probability is not met`() {
        val rule = MurphyRule.builder()
            .matches(Matchers.always())
            .causes(Effects.status(500).withProbability(0.0))
            .build()

        val builder = RestClient.builder()
            .requestInterceptor(MurphyRestClientInterceptor(MurphyScenario.from(rule)))

        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        server.expect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andRespond(MockRestResponseCreators.withSuccess("Mock Server Response", MediaType.TEXT_PLAIN))

        val response = client.get().uri("/test").exchange { _, res -> res }

        assertEquals(200, response.statusCode.value())
        assertEquals("Mock Server Response", response.bodyTo(String::class.java))
        server.verify()
    }

    @Test
    fun `scenario with multiple rules - first match wins`() {
        val scenario = MurphyScenario.from(
            MurphyRule.builder()
                .matches(Matchers.path("/api/foo"))
                .causes(Effects.status(201))
                .build(),
            MurphyRule.builder()
                .matches(Matchers.path("/api/**"))
                .causes(Effects.status(500))
                .build()
        )

        val client = RestClient.builder()
            .requestInterceptor(MurphyRestClientInterceptor(scenario))
            .build()

        val responseFoo = client.get().uri("/api/foo").exchange { _, res -> res }
        assertEquals(201, responseFoo.statusCode.value())

        val responseBar = client.get().uri("/api/bar").exchange { _, res -> res }
        assertEquals(500, responseBar.statusCode.value())
    }
}
