package com.github.fstien.ktor.header.forwarding

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.util.*

class HeaderForwardingClient {
    class Config

    companion object : HttpClientFeature<Config, HeaderForwardingClient> {
        override val key: AttributeKey<HeaderForwardingClient> = AttributeKey("HeaderForwardingClient")

        override fun prepare(block: Config.() -> Unit): HeaderForwardingClient {
            return HeaderForwardingClient()
        }

        override fun install(feature: HeaderForwardingClient, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.State) {
                val headerContextElement = coroutineContext[HeaderContextElement]

                if (headerContextElement == null) {
                    log.error("headerContextElement not found in coroutine context, ensure that the Ktor application has the HeaderForwardingServer feature installed.")
                    return@intercept
                }

                headerContextElement.headers.forEach { (header, values) ->
                    values.forEach { value ->
                        context.headers.append(header, value)
                    }
                }
            }
        }
    }
}
