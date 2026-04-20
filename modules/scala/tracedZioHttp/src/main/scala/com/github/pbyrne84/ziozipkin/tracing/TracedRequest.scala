package com.github.pbyrne84.ziozipkin.tracing

import zio.ZIO
import zio.http.{Request, Response}
import zio.telemetry.opentracing.OpenTracing

trait TracedRequest {

  def traced(
      spanName: String,
      request: Request
  )(call: => ZIO[OpenTracing, Throwable, Response]): ZIO[Any, Throwable, Response] = {

    B3Tracing
      .serverSpan(spanName) {
        for {
          _ <- ZIO.logInfo("starting request")
          result <- call
          newTracedHeaders <- B3HTTPResponseTracing.appendHeadersToResponse(result.headers)
          _ <- ZIO.logInfo("finished request")
        } yield result.copy(headers = newTracedHeaders)

      }
      .provide(
        B3JaegerTracer
          .createTracerLayerFromRequestHeaders(serviceName = "boop", request.headers),
        B3HTTPResponseTracing.layer
      )
  }

}
