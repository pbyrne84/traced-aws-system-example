package com.github.pbyrne84.ziozipkin.tracing

import com.github.pbyrne84.ziozipkin.client.B3
import zio.http.{Header, Headers, Request, Response}
import zio.telemetry.opentelemetry.Tracing
import zio.{Task, ZIO}

trait TracedRequest {

  def traced[A, B](
      spanName: String,
      request: Request,
      defaultToAlwaysSample: Boolean = true
  )(call: => ZIO[A, B, Response]): ZIO[Tracing with B3HTTPResponseTracing with A, Any, Response] = {

    for {
      defaultingSampledRequest <- headersWithSamplingAdded(request, defaultToAlwaysSample)
      tracedOperation <- B3Tracing.startTracing(spanName, defaultingSampledRequest)(
        for {
          _ <- ZIO.logInfo("starting request")
          result <- call
          newTracedHeaders <- B3HTTPResponseTracing.appendHeadersToResponse(result.headers)
          _ <- ZIO.logInfo("finished request")
        } yield result.copy(headers = newTracedHeaders)
      )
    } yield tracedOperation
  }

  private def headersWithSamplingAdded(request: Request, defaultToAlwaysSample: Boolean): Task[Request] = {
    ZIO.attempt {
      val headersWithDefaultingSampleHeaders =
        B3.defaultSampledHeader(request.headers, defaultToAlwaysSample)

      val headersWithTracing = Headers(headersWithDefaultingSampleHeaders)

      request.copy(headers = headersWithTracing)
    }
  }
}
