package com.github.pbyrne84.ziozipkin.client

import com.github.pbyrne84.ziozipkin.logging.ExampleLogAnnotations
import com.github.pbyrne84.ziozipkin.tracing.{B3Tracing, HTTPResponseTracing}
import zio.http._
import zio.telemetry.opentracing.OpenTracing
import zio.{&, Scope, ZIO, ZLayer}

object TracingClient {
  def request(
      url: String,
      method: Method = Method.GET,
      headers: Headers = Headers.empty,
      content: Body = Body.empty
  ): ZIO[Any with OpenTracing with Client & Scope with TracingClient, Throwable, Response] = {
    ZIO.service[TracingClient].flatMap(_.request(url, method, headers, content))
  }

  val tracingClientLayer: ZLayer[HTTPResponseTracing, Nothing, TracingClient] = ZLayer {
    for {
      httpTracing <- ZIO.service[HTTPResponseTracing]
    } yield new TracingClient(httpTracing)
  }

}

class TracingClient(HTTPTracing: HTTPResponseTracing) {
  def request(
      url: String,
      method: Method = Method.GET,
      headers: Headers = Headers.empty,
      content: Body = Body.empty
  ): ZIO[OpenTracing with Client & Scope, Throwable, Response] =
    B3Tracing.serverSpan(s"${method.toString.toLowerCase}-client-call") {
      for {
        _ <- ZIO
          .logInfo(s"calling remote service")
        appendedHeaders <- HTTPTracing.appendHeadersToResponse(headers)
        request1 = Request(url = URL(Path(url)), method = method, headers = appendedHeaders, body = content)

        response <- Client.request(
          request1
        )
        _ <- B3Tracing.serverSpan("client-call-status") {
          // we can now look for things that are not okay using span_name = client-call-status and message != OK as a search value
          // we should also log the payload we sent at that point.
          ZIO.logInfo(response.status.toString)
        }
      } yield response
    } @@ ExampleLogAnnotations.clientRequest(method, url)
}
