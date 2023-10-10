package com.github.pbyrne84.ziozipkin.client

import com.github.pbyrne84.ziozipkin.logging.ExampleLogAnnotations
import com.github.pbyrne84.ziozipkin.tracing.{B3Tracing, HTTPResponseTracing}
import io.opentelemetry.api.trace.{SpanId, TraceId}
import zio.http._
import zio.telemetry.opentelemetry.Tracing
import zio.{ZIO, ZLayer}

object B3 {

  // current context returns this as filler if the tracing is not working properly
  val emptyTraceId: String = "0".padTo(TraceId.getLength, "0").mkString
  val emptySpanId: String = "0".padTo(SpanId.getLength, "0").mkString

  object header {
    val traceId: String = "X-B3-TraceId"
    val spanId: String = "X-B3-SpanId"
    val sampled: String = "X-B3-Sampled"
  }

  def defaultSampledHeader(
      headers: Headers,
      defaultToAlwaysSample: Boolean = true
  ): Headers = {
    def generateDefaultHeader: Header = {
      val sample = if (defaultToAlwaysSample) {
        "1"
      } else {
        "0"
      }

      Header.Custom(B3.header.sampled, sample)
    }

    val lowercaseSampledHeaderName = B3.header.sampled.toLowerCase
    val maybeSampledHeader =
      headers.find(header => {
        header.headerName.toLowerCase == lowercaseSampledHeaderName
      })

    maybeSampledHeader match {
      case Some(header) =>
        if (List("0", "1").contains(header.renderedValue)) {
          headers
        } else {
          val headersWithoutSampled = headers.filterNot { header =>
            header.headerName.toLowerCase == lowercaseSampledHeaderName
          }.toList

          Headers(headersWithoutSampled :+ generateDefaultHeader)
        }

      case None => headers ++ Headers(generateDefaultHeader)
    }
  }

}

object TracingClient {
  def request(
      url: String,
      method: Method = Method.GET,
      headers: Headers = Headers.empty,
      content: Body = Body.empty
  ): ZIO[Any with Tracing with Client with TracingClient, Throwable, Response] = {
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
  ): ZIO[Any with Tracing with Client, Throwable, Response] =
    B3Tracing.serverSpan(s"${method.toString.toLowerCase}-client-call") {
      for {
        _ <- ZIO
          .logInfo(s"calling remote service")
        appendedHeaders <- HTTPTracing.appendHeadersToResponse(headers)
        response <- Client.request(
          url = url,
          method = method,
          headers = appendedHeaders,
          content = content
        )
        _ <- B3Tracing.serverSpan("client-call-status") {
          // we can now look for things that are not okay using span_name = client-call-status and message != OK as a search value
          // we should also log the payload we sent at that point.
          ZIO.logInfo(response.status.toString)
        }
      } yield response
    } @@ ExampleLogAnnotations.clientRequest(method, url)
}
