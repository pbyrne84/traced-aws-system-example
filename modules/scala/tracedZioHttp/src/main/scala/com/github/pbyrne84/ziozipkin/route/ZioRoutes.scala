package com.github.pbyrne84.ziozipkin.route

import com.github.pbyrne84.ziozipkin.logging.JavaLogging
import com.github.pbyrne84.ziozipkin.tracing.TracedRequest
import org.slf4j
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import zio.http._
import zio.logging.backend.SLF4J
import zio.telemetry.opentracing.OpenTracing
import zio.{Scope, ULayer, ZIO, ZLayer}

object RoutesBuild {

  lazy val routesBuild: ZIO[Any with Scope, Throwable, ZioRoutes] = ZLayer
    .make[ZioRoutes](
      ZioRoutes.routesLayer
    )
    .build
    .map(_.get[ZioRoutes])
}

object ZioRoutes {
  val routesLayer: ULayer[ZioRoutes] = ZLayer.succeed(new ZioRoutes)

}

class ZioRoutes extends TracedRequest {
  SLF4JBridgeHandler.install()
  val logger = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  val sl4jLogger: slf4j.Logger = LoggerFactory.getLogger(getClass)

  val routes: Routes[Any, Response] = {
    Routes(
      Method.GET / "test" -> handler { (req: Request) =>
        traced("zio-test-route", req) {
          val value: ZIO[OpenTracing, Throwable, Response] = {
            ZIO
              .serviceWithZIO[OpenTracing] { tracing =>
                for {
                  _ <- JavaLogging.attemptWithMdcLoggingUsingTracing(sl4jLogger.info("wppf"))
                  response <- ZIO.attempt(Response.text("content"))
                } yield response

              }
          }

          value
        }.mapError(a => Response.text(a.toString))
      }
    ).provide(logger)
  }

}
