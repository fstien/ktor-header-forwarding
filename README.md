
 [ ![Download](https://api.bintray.com/packages/fstien/ktor-header-forwarding/ktor-header-forwarding/images/download.svg) ](https://bintray.com/fstien/ktor-header-forwarding/ktor-header-forwarding/_latestVersion)
![GitHub](https://img.shields.io/github/license/fstien/ktor-header-forwarding.svg?color=green&style=popout)
[![Unit Tests Actions Status](https://github.com/fstien/ktor-header-forwarding/workflows/Unit%20Tests/badge.svg)](https://github.com/{userName}/{repoName}/actions)


# Ktor Header Forwarding

Ktor features for forwarding HTTP headers from incoming server requests to outgoing client requests. 

## Installation

Available for download from [jcenter](https://bintray.com/fstien/ktor-header-forwarding/ktor-header-forwarding).
### Maven
Add the following dependency to your pom.xml:
    <dependency>
      <groupId>com.github.fstien</groupId>
      <artifactId>ktor-header-forwarding</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
    </dependency>

### Gradle
Add the following to your dependencies in your build.gradle:

    implementation 'com.github.fstien:ktor-header-forwarding:0.1.0'

## Example
For use with Istio's distributed tracing features, see [ktor-istio-distributed-tracing-demo](https://github.com/fstien/ktor-istio-distributed-tracing-demo).

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
