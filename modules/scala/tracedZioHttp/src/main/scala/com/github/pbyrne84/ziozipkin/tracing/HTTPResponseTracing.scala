package com.github.pbyrne84.ziozipkin.tracing

import io.opentracing.SpanContext
import zio.http._
import zio.telemetry.opentracing.OpenTracing
import zio.{ZIO, ZLayer}

object HTTPResponseTracing {}

trait HTTPResponseTracing {
  def appendHeadersToResponse(headers: Headers): ZIO[OpenTracing, Nothing, Headers]

  protected def currentContext: ZIO[OpenTracing, Nothing, SpanContext] =
    ZIO.serviceWithZIO[OpenTracing](_.getCurrentSpanContextUnsafe)
}

object B3HTTPResponseTracing {
  val layer: ZLayer[Any, Nothing, B3HTTPResponseTracing] = ZLayer(
    ZIO.succeed(new B3HTTPResponseTracing)
  )

  def appendHeadersToResponse(
      currentHeaders: Headers
  ): ZIO[OpenTracing with B3HTTPResponseTracing, Nothing, Headers] = {
    ZIO.serviceWithZIO[B3HTTPResponseTracing](_.appendHeadersToResponse(currentHeaders))
  }

}

class B3HTTPResponseTracing extends HTTPResponseTracing {
  override def appendHeadersToResponse(headers: Headers): ZIO[OpenTracing, Nothing, Headers] = {
    currentContext.map(context => createHeadersFromContext(context, headers))
  }

  private def createHeadersFromContext(context: SpanContext, headers: Headers): Headers = {

    headers.combine(
      Headers(
        Header.Custom(B3JaegerTracer.headerName.traceId, context.toTraceId),
        Header.Custom(B3JaegerTracer.headerName.spanId, context.toSpanId),
        Header.Custom(B3JaegerTracer.headerName.sampled, "1")
      )
    )
  }
}
