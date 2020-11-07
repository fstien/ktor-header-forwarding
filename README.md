# Ktor Header Forwarding

Ktor features for forwarding HTTP headers from incoming server requests to outgoing client requests. 

## Usage

Install the `HeaderForwardingServer` feature as follows in a module function: 

```kotlin 
install(HeaderForwardingServer) {
    header("X-Correlation-ID")
}
```
You can either configure individual headers by passing them to `header()`. You can also forward a range of headers by defining a filter. For example, to forward all headers of the [b3 propagation](https://github.com/openzipkin/b3-propagation) format:
```kotlin
install(HeaderForwardingServer) {
    filter { header -> header.startsWith("X-B3-") }
}
```
Then install the `HeaderForwardingClient` feature on the Ktor HTTP client:
```kotlin
install(HeaderForwardingClient) 
```

#### [MIT](./LICENSE) License