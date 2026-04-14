package com.github.pbyrne84.ziozipkin.logging

import org.slf4j.MDC
import zio.{FiberRefs, Task, Trace, ZIO}
import zio.logging.LogContext

import scala.jdk.CollectionConverters.MapHasAsJava

object JavaLogging {

  def attemptWithMdcLogging[A](code: => A)(implicit trace: Trace): Task[A] = {
    ZIO.getFiberRefs.flatMap { fiberRefs: FiberRefs =>
      val refsKeys = fiberRefs.fiberRefs

      val entries: Map[String, String] = refsKeys.flatMap { key =>
        fiberRefs.get(key) match {
          case Some(logContext: LogContext) => logContext.asMap
          case _ => Map.empty
        }
      }.toMap

      val mdcAtStart = Option(MDC.getCopyOfContextMap)
      try {
        // Follows similar logic to FiberRuntime.log
        MDC.setContextMap(entries.asJava)
        val result = code
        ZIO.succeed(result)
      } finally {
        MDC.setContextMap(mdcAtStart.orNull)
      }

    }
  }
}
