package com.github.fstien.ktor.header.forwarding

import io.ktor.application.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


val log = KotlinLogging.logger { }

internal data class HeaderContextElement(
        val headers: Map<String, List<String>>
): AbstractCoroutineContextElement(HeaderContextElement) {
    companion object Key: CoroutineContext.Key<HeaderContextElement>
}

class HeaderForwardingServer(
        val headers: List<String>,
        val filters: List<(String) -> Boolean>
) {
    class Configuration {
        val headers = mutableListOf<String>()

        fun header(name: String) {
            headers.add(name)
        }

        val filters = mutableListOf<(String) -> Boolean>()

        fun filter(predicate: (String) -> Boolean) {
            filters.add(predicate)
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, HeaderForwardingServer> {
        override val key = AttributeKey<HeaderForwardingServer>("HeaderForwarding")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): HeaderForwardingServer {
            val config = Configuration().apply(configure)
            val feature = HeaderForwardingServer(config.headers, config.filters)

            val headerForwarding = PipelinePhase("HeaderForwarding")
            pipeline.insertPhaseAfter(ApplicationCallPipeline.Monitoring, headerForwarding)

            pipeline.intercept(headerForwarding) {

                val requestHeaders: Map<String, List<String>> = call.request.headers.toMap()
                val forwardedHeaders: Map<String, List<String>> = requestHeaders
                        .filterKeys { header ->
                            header in config.headers || config.filters.any { it(header) }
                        }

                withContext(HeaderContextElement(forwardedHeaders)) {
                    proceed()
                }
            }

            return feature
        }
    }
}


