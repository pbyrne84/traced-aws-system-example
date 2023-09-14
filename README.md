# traced-aws-system-example

Download zipkin from 

**https://zipkin.io/pages/quickstart.html**

<https://search.maven.org/remote_content?g=io.zipkin&a=zipkin-server&v=LATEST&c=exec>

And then just start with 

```shell
java -jar zipkin-server-2.24.3-exec.jar
```
2.24.3 may be an older version.


## Example play log output with tracing auto added across request headers with mdc also auto propagated across futures


```json
{
  "timestamp" : "2023-09-14 08:45:06.997",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "kamon.instrumentation.play.GuiceModule$KamonLoader",
  "message" : "Reconfiguring Kamon with Play's Config",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.039",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "kamon.instrumentation.play.GuiceModule$KamonLoader",
  "message" : "play.core.server.AkkaHttpServerProvider",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.040",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "kamon.instrumentation.play.GuiceModule$KamonLoader",
  "message" : "10 seconds",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.098",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "kamon.zipkin.ZipkinReporter",
  "message" : "Started the Zipkin reporter",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.106",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "kamon.Init",
  "message" : "\n _\n| |\n| | ____ _ _ __ ___   ___  _ __\n| |/ / _  |  _ ` _ \\ / _ \\|  _ \\\n|   < (_| | | | | | | (_) | | | |\n|_|\\_\\__,_|_| |_| |_|\\___/|_| |_|\n=====================================\nInitializing Kamon Telemetry \u001B[1m\u001B[32mv2.6.1\u001B[0m\u001B[0m / Kanela \u001B[1m\u001B[32mv1.0.17\u001B[0m\u001B[0m\n",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.151",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "play.api.http.EnabledFilters",
  "message" : "Enabled Filters (see <https://www.playframework.com/documentation/latest/Filters>):\n\n    play.filters.csrf.CSRFFilter\n    play.filters.headers.SecurityHeadersFilter\n    play.filters.hosts.AllowedHostsFilter\n",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.179",
  "level" : "INFO",
  "thread" : "play-dev-mode-akka.actor.default-dispatcher-5",
  "mdc" : {
    "kamonSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "kamonSpanName" : "empty"
  },
  "logger" : "play.api.Play",
  "message" : "Application started (Dev) (no global state)",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.409",
  "level" : "INFO",
  "thread" : "application-akka.actor.default-dispatcher-4",
  "mdc" : {
    "request" : "HttpRequest(HttpMethod(GET),/,List(Timeout-Access: <function1>, Remote-Address: 127.0.0.1:53326, Raw-Request-URI: /, Tls-Session-Info: Session(1694677469631|SSL_NULL_WITH_NULL_NULL), Accept: application/json, X-B3-Sampled: 1, X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124, X-B3-SpanId: a2fb4a1d1a96d312, Host: localhost:9000, Connection: Keep-Alive, User-Agent: Apache-HttpClient/4.5.14 (Java/17.0.8), Accept-Encoding: br,deflate,gzip,x-gzip),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1))",
    "kamonSpanId" : "f4a845998693bbc8",
    "kamonParentSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "action" : "tp-controller-show-homepage",
    "kamonSpanName" : "operation",
    "entity" : "None"
  },
  "logger" : "controllers.ExampleTracedController",
  "message" : "start of processing",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.412",
  "level" : "INFO",
  "thread" : "application-akka.actor.default-dispatcher-4",
  "mdc" : {
    "request" : "HttpRequest(HttpMethod(GET),/,List(Timeout-Access: <function1>, Remote-Address: 127.0.0.1:53326, Raw-Request-URI: /, Tls-Session-Info: Session(1694677469631|SSL_NULL_WITH_NULL_NULL), Accept: application/json, X-B3-Sampled: 1, X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124, X-B3-SpanId: a2fb4a1d1a96d312, Host: localhost:9000, Connection: Keep-Alive, User-Agent: Apache-HttpClient/4.5.14 (Java/17.0.8), Accept-Encoding: br,deflate,gzip,x-gzip),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1))",
    "kamonSpanId" : "f4a845998693bbc8",
    "kamonParentSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "action" : "tp-child-action",
    "kamonSpanName" : "operation",
    "entity" : "None"
  },
  "logger" : "controllers.ExampleTracedController",
  "message" : "banana",
  "context" : "default"
}
```


**Child request to /test - kamonSpanId:f4a845998693bbc8 is now in kamonParentSpanId as this is how Zipkin/Jaeger draws call chains**

```json
{
  "timestamp" : "2023-09-14 08:45:07.541",
  "level" : "INFO",
  "thread" : "application-akka.actor.default-dispatcher-4",
  "mdc" : {
    "request" : "HttpRequest(HttpMethod(GET),/test,List(Timeout-Access: <function1>, Remote-Address: 127.0.0.1:53342, Raw-Request-URI: /test, Tls-Session-Info: Session(1694677469631|SSL_NULL_WITH_NULL_NULL), X-B3-SpanId: 6cfa406e5e3e014e, context-tags: upstream.name=traced-play;, X-B3-Sampled: 1, X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124, X-B3-ParentSpanId: f4a845998693bbc8, Host: localhost:9000, Accept: */*, user-agent: AHC/2.1),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1))",
    "kamonSpanId" : "41ef6a23288f9dbd",
    "kamonParentSpanId" : "6cfa406e5e3e014e",
    "upstream.name" : "traced-play",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "action" : "tp-controller-test-request-call-back",
    "kamonSpanName" : "operation",
    "entity" : "None"
  },
  "logger" : "controllers.ExampleTracedController",
  "message" : "child test call",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.546",
  "level" : "INFO",
  "thread" : "application-akka.actor.default-dispatcher-4",
  "mdc" : {
    "request" : "HttpRequest(HttpMethod(GET),/test,List(Timeout-Access: <function1>, Remote-Address: 127.0.0.1:53342, Raw-Request-URI: /test, Tls-Session-Info: Session(1694677469631|SSL_NULL_WITH_NULL_NULL), X-B3-SpanId: 6cfa406e5e3e014e, context-tags: upstream.name=traced-play;, X-B3-Sampled: 1, X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124, X-B3-ParentSpanId: f4a845998693bbc8, Host: localhost:9000, Accept: */*, user-agent: AHC/2.1),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1))",
    "kamonSpanId" : "41ef6a23288f9dbd",
    "kamonParentSpanId" : "6cfa406e5e3e014e",
    "upstream.name" : "traced-play",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "action" : "tp-controller-test-request-call-back",
    "kamonSpanName" : "operation",
    "entity" : "None"
  },
  "logger" : "controllers.ExampleTracedController",
  "message" : "start of processing",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.548",
  "level" : "INFO",
  "thread" : "application-akka.actor.default-dispatcher-4",
  "mdc" : {
    "request" : "HttpRequest(HttpMethod(GET),/test,List(Timeout-Access: <function1>, Remote-Address: 127.0.0.1:53342, Raw-Request-URI: /test, Tls-Session-Info: Session(1694677469631|SSL_NULL_WITH_NULL_NULL), X-B3-SpanId: 6cfa406e5e3e014e, context-tags: upstream.name=traced-play;, X-B3-Sampled: 1, X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124, X-B3-ParentSpanId: f4a845998693bbc8, Host: localhost:9000, Accept: */*, user-agent: AHC/2.1),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1))",
    "kamonSpanId" : "41ef6a23288f9dbd",
    "kamonParentSpanId" : "6cfa406e5e3e014e",
    "upstream.name" : "traced-play",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "action" : "tp-controller-test-request-call-back",
    "kamonSpanName" : "operation",
    "entity" : "None"
  },
  "logger" : "controllers.ExampleTracedController",
  "message" : "Successfully processed request without exception: GET /test",
  "context" : "default"
}
```
```json
{
  "timestamp" : "2023-09-14 08:45:07.573",
  "level" : "INFO",
  "thread" : "application-akka.actor.default-dispatcher-4",
  "mdc" : {
    "request" : "HttpRequest(HttpMethod(GET),/,List(Timeout-Access: <function1>, Remote-Address: 127.0.0.1:53326, Raw-Request-URI: /, Tls-Session-Info: Session(1694677469631|SSL_NULL_WITH_NULL_NULL), Accept: application/json, X-B3-Sampled: 1, X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124, X-B3-SpanId: a2fb4a1d1a96d312, Host: localhost:9000, Connection: Keep-Alive, User-Agent: Apache-HttpClient/4.5.14 (Java/17.0.8), Accept-Encoding: br,deflate,gzip,x-gzip),HttpEntity.Strict(none/none,0 bytes total),HttpProtocol(HTTP/1.1))",
    "kamonSpanId" : "f4a845998693bbc8",
    "kamonParentSpanId" : "a2fb4a1d1a96d312",
    "kamonTraceId" : "463ac35c9f6413ad48485a3953bb6124",
    "action" : "tp-controller-show-homepage",
    "kamonSpanName" : "operation",
    "entity" : "None"
  },
  "logger" : "controllers.ExampleTracedController",
  "message" : "Successfully processed request without exception: GET /",
  "context" : "default"
}
```