package com.github.fstien.ktor.header.forwarding

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeaderForwardingTest {

    private var clientRequest: HttpRequestData? = null
    private val httpClient = HttpClient(MockEngine) {
        install(HeaderForwardingClient)
        engine {
            addHandler { request ->
                clientRequest = request
                when (request.url.encodedPath) {
                    "name" -> {
                        respond("Francois", HttpStatusCode.OK)
                    }
                    else -> error("Unhandled path: ${request.url.encodedPath}")
                }
            }
        }
    }

    @BeforeEach
    fun setup() {
        clientRequest = null
    }

    @Test
    fun `forward a single header from server to client requests`() = withTestApplication {
        // prepare
        val path = "/greeting"
        application.setUpRouteThatUsesHttpClient(path)

        application.install(HeaderForwardingServer) {
            header("X-Correlation-ID")
        }

        // act
        val correlationId = "45ccff40-cd12-41d2-bc6c-337e6df6d0c5"
        handleRequest(HttpMethod.Get, path) {
            addHeader("X-Correlation-ID", correlationId)
        }

        // assert
        assertThat(clientRequest?.headers?.get("X-Correlation-ID")).isEqualTo(correlationId)
    }

    @Test
    fun `forward multiple values of the same header`() = withTestApplication {
        // prepare
        val path = "/greeting"
        application.setUpRouteThatUsesHttpClient(path)

        val headerName = "CustomHeader"
        application.install(HeaderForwardingServer) {
            header(headerName)
        }

        // act
        handleRequest(HttpMethod.Get, path) {
            addHeader(headerName, "value1")
            addHeader(headerName, "value2")
        }

        // assert
        assertThat(clientRequest?.headers?.getAll(headerName)).isEqualTo(listOf("value1", "value2"))
    }

    @Test
    fun `forward multiple headers using a filter`() = withTestApplication {
        // prepare
        val path = "/greeting"
        application.setUpRouteThatUsesHttpClient(path)

        application.install(HeaderForwardingServer) {
            filter { header -> header.startsWith("X-B3-") }
        }

        val traceId = "4112330345"
        val parentSpanId = "4902266870"
        val spanId = "1331775032"
        val sampled = "true"

        // act
        handleRequest(HttpMethod.Get, path) {
            addHeader("X-B3-TraceId", traceId)
            addHeader("X-B3-ParentSpanId", parentSpanId)
            addHeader("X-B3-SpanId", spanId)
            addHeader("X-B3-Sampled", sampled)
        }

        // assert
        with(clientRequest?.headers!!) {
            assertThat(get("X-B3-TraceId")).isEqualTo(traceId)
            assertThat(get("X-B3-ParentSpanId")).isEqualTo(parentSpanId)
            assertThat(get("X-B3-SpanId")).isEqualTo(spanId)
            assertThat(get("X-B3-Sampled")).isEqualTo(sampled)
        }
    }

    @Test
    fun `only forwards headers once if there is overlap between config`() = withTestApplication {
        // prepare
        val path = "/greeting"
        application.setUpRouteThatUsesHttpClient(path)

        application.install(HeaderForwardingServer) {
            header("X-B3-ParentSpanId")
            filter { header -> header.startsWith("X-B3-") }
        }

        val traceId = "4112330345"
        val parentSpanId = "4902266870"
        val spanId = "1331775032"
        val sampled = "true"

        // act
        handleRequest(HttpMethod.Get, path) {
            addHeader("X-B3-TraceId", traceId)
            addHeader("X-B3-ParentSpanId", parentSpanId)
            addHeader("X-B3-SpanId", spanId)
            addHeader("X-B3-Sampled", sampled)
        }

        // assert
        with(clientRequest?.headers!!) {
            val parentSpanHeaders = getAll("X-B3-ParentSpanId")
            assertThat(parentSpanHeaders?.size).isEqualTo(1)
            assertThat(parentSpanHeaders).isEqualTo(listOf(parentSpanId))
        }
    }

    @Test
    fun `other headers are unaffected in server or client requests`() = withTestApplication {
        // prepare
        val path = "/greeting"

        var serverIncomingHeaders: Headers? = null
        application.routing {
            get(path) {
                serverIncomingHeaders = call.request.headers

                val name = httpClient.get<String>("/name") {
                    header("ExampleClientRequestHeader", "mockValue")
                }

                call.respond(HttpStatusCode.OK, "Hello $name")
            }
        }

        application.install(HeaderForwardingServer) {
            header("CustomHeader")
        }

        val correlationId = "681425ce-f5d3-49a7-8ea7-7541374b90b7"

        // act
        handleRequest(HttpMethod.Get, path) {
            addHeader("X-Correlation-ID", correlationId)
        }

        // assert
        assertThat(serverIncomingHeaders?.get("X-Correlation-ID")).isEqualTo(correlationId)
        assertThat(clientRequest?.headers?.get("ExampleClientRequestHeader")).isEqualTo("mockValue")
    }

    private fun Application.setUpRouteThatUsesHttpClient(path: String) {
        routing {
            get(path) {
                val name = httpClient.get<String>("/name")
                call.respond(HttpStatusCode.OK, "Hello $name")
            }
        }
    }
}