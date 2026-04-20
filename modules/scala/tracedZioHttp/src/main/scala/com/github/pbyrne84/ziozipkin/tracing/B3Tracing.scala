package com.github.pbyrne84.ziozipkin.tracing

import com.github.pbyrne84.ziozipkin.logging.ExampleLogAnnotations
import io.opentelemetry.api.trace.StatusCode
import zio.telemetry.opentracing.OpenTracing
import zio.{Trace, ZIO}

object B3Tracing {

  // Creates a new span and makes sure all the new details gets set in the logging context.
  // Very confusing just using Tracing.span otherwise.
  // Span names should be fairly non unique as we can use them to search in kibana etc.
  // So you can use traceId and spanName to locate things. Though you can also make things a bit
  // more flexible like client-call-success or client-call-fail as then it is quite easy
  // to look at those things over a period to spot any errant patterns.
  // There are fun games of spot the difference and less than fun ones.
  //
  // A less fun game of spot the difference  is to strip nulls in all payloads
  // then the spec has to be continuously referenced on any problems.
  // I put that in the evil category of how to make someones day bad.
  //
  // Implicit trace will just mark where the serverSpan was called unless it has been passed down
  // e.g  "trace": "com.github.pbyrne84.zio2playground.http.TracingHttp.initialiseB3Trace.applyOrElse(TracingHttp.scala:41)"
  def serverSpan[R, E, A](
      name: String
  )(effect: => ZIO[R, E, A])(implicit trace: Trace): ZIO[R with OpenTracing, E, A] = {

    ZIO.serviceWithZIO[OpenTracing] { tracing =>
      for {
        _ <- tracing.setBaggageItem("span-name", name)
        spanContext <- tracing.getCurrentSpanContextUnsafe
        traceId = spanContext.toTraceId
        spanId = spanContext.toSpanId
        parentSpanId = ""
        result <- effect @@ ExampleLogAnnotations.stringTraceId(traceId) @@
          ExampleLogAnnotations.parentSpanId(parentSpanId) @@
          ExampleLogAnnotations.stringSpanId(spanId) @@
          ExampleLogAnnotations.stringSpanName(name) @@
          ExampleLogAnnotations.trace(trace.toString)

      } yield result
    }

  }

  // Not sure what really should be used at this point
  private def defaultErrorMapper[E]: PartialFunction[E, StatusCode] = { case _ =>
    StatusCode.ERROR
  }
}
