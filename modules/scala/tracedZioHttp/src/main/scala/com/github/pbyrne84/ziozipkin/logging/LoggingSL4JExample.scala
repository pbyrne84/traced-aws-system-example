package com.github.pbyrne84.ziozipkin.logging

import com.github.pbyrne84.ziozipkin.tracing.{B3JaegerTracer, B3Tracing}
import io.jaegertracing.internal.JaegerTracer
import org.slf4j
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import zio.logging.LogAnnotation
import zio.logging.backend.SLF4J
import zio.telemetry.opentracing.OpenTracing
import zio.{Cause, ZIO, ZIOAppDefault}

import java.util.logging.{Level, Logger}

object LoggingSL4JExample extends ZIOAppDefault {

  override def run = {

    System.setProperty("JAEGER_SERVICE_NAME", "moo")

    val logger = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

    new LoggingSL4JExample().run.provide(
      logger,
      B3JaegerTracer.createTracerLayer(serviceName = "boop", traceId = "a7bcaeac5232cc7b", spanId = "cc0a0801a0da8a62")
    )
  }
}

class LoggingSL4JExample {

  // needed for util->sl4j logging
  // "jul-to-slf4j"
  SLF4JBridgeHandler.install()

  private val javaUtilLogger: Logger = Logger.getLogger(getClass.getName)
  javaUtilLogger.setLevel(Level.INFO)

  val sl4jLogger: slf4j.Logger = LoggerFactory.getLogger(getClass)

  def run: ZIO[OpenTracing & JaegerTracer, Throwable, Unit] = {
    ZIO.serviceWithZIO[OpenTracing] { tracing =>
      (for {
        _ <- tracing.setBaggageItem("foo", "bar")
        op1 <- createOperation("banana1").fork
        op2 <- createOperation("banana2").fork
        _ <- op1.join
        _ <- op2.join
      } yield ()) @@ LogAnnotation.UserId("user-id")
    }

  }

  private def createOperation(spanName: String): ZIO[OpenTracing & JaegerTracer, Throwable, Unit] = {
    // Wrap like this to get child spans
    B3Tracing
      .serverSpan(spanName) {
        {
          for {
            _ <- ZIO.logInfo("I am the devil")

            // this will set the mdc from the logging context and then reset it back after
            // similarly to how the zio.logging.backend.SL4J.closeLogEntry operates.
            // ZIOHack as the name implies is a hack. It abuses package name to get access.
            // Definitely a less than ideal solution however adult anyone feels.
            _ <- JavaLogging.attemptWithMdcLoggingUsingTracing {
              javaUtilLogger.severe(s"util meowWithMdcNoHack $getThreadName")
            }

            _ <- ZIO.attempt {
              javaUtilLogger.severe(s"util meowWithNoMdcMdc $getThreadName")
            }
            _ <- JavaLogging.attemptWithMdcLoggingUsingTracing(sl4jLogger.info(s"woofWithMdc $getThreadName"))
            _ <- JavaLogging.attemptWithMdcLoggingUsingTracing(sl4jLogger.info(s"woofWithMdc2 $getThreadName"))
            _ <- ZIO.attempt(sl4jLogger.info(s"woofNoMdc $getThreadName"))
            a = new RuntimeException("I had problems and the ice cream didn't help")
            _ <- ZIO.logCause("dying for a cause", Cause.die(a))
          } yield ()
        } @@ ExampleLogAnnotations.kitty("kitty")

      } @@ ExampleLogAnnotations.kitty("kitty")
  }

  private def getThreadName =
    Thread.currentThread().getName

}
