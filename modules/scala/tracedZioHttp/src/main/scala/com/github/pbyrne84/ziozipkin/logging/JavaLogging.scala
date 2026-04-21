package com.github.pbyrne84.ziozipkin.logging

import com.github.pbyrne84.ziozipkin.tracing.B3JaegerTracer
import org.slf4j.MDC
import zio.logging.LogContext
import zio.telemetry.opentracing.OpenTracing
import zio.{FiberRefs, Task, Trace, ZIO}

import scala.jdk.CollectionConverters._

object JavaLogging {

  def attemptWithMdcLoggingUsingTracing[A](code: => A)(implicit trace: Trace): ZIO[OpenTracing, Throwable, A] = {
    ZIO
      .serviceWithZIO[OpenTracing] { tracing =>
        val mdcAtStart = Option(MDC.getCopyOfContextMap)

        (for {
          context <- tracing.getCurrentSpanContextUnsafe
          logContextMap <- getLogContextMap
          baggageMap = context.baggageItems().asScala.map(entry => entry.getKey -> entry.getValue).toMap
          traceMap = Map(
            B3JaegerTracer.headerName.traceId -> context.toTraceId,
            B3JaegerTracer.headerName.spanId -> context.toSpanId
          )
          _ = MDC.setContextMap((logContextMap ++ baggageMap ++ traceMap).asJava)
          result <- ZIO.attempt(code)
          _ = MDC.setContextMap(mdcAtStart.orNull)
        } yield result).onError(__ => ZIO.succeed(MDC.setContextMap(mdcAtStart.orNull)))
      }
  }

  private def getLogContextMap: Task[Map[String, String]] = {
    ZIO.getFiberRefs.map { (fiberRefs: FiberRefs) =>
      val refsKeys = fiberRefs.fiberRefs

      refsKeys.flatMap { key =>
        fiberRefs.get(key) match {
          case Some(logContext: LogContext) => logContext.asMap
          case _                            => Map.empty
        }
      }.toMap
    }
  }

}
