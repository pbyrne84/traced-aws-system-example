package com.github.pbyrne84.ziozipkin

import com.github.pbyrne84.ziozipkin.client.TracingClient
import com.github.pbyrne84.ziozipkin.route.{RoutesBuild, ZioRoutes}
import com.github.pbyrne84.ziozipkin.tracing.{B3HTTPResponseTracing, ZipkinExportingTracer}
import org.slf4j.bridge.SLF4JBridgeHandler
import zio._
import zio.http._
import zio.http.netty.NettyConfig
import zio.http.netty.NettyConfig.LeakDetectionLevel
import zio.logging.backend.SLF4J

import scala.util.Try

object ZIOHttpZipkinApp extends ZIOAppDefault {
  private val PORT = 58479
  private val loggingLayer = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  SLF4JBridgeHandler.install()

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> loggingLayer

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    RoutesBuild.routesBuild.flatMap((routes: ZioRoutes) => startService(routes))
  }

  private def startService(routes: ZioRoutes): ZIO[ZIOAppArgs, Any, Nothing] = {
    ZIOAppArgs.getArgs.flatMap { args =>
      // Configure thread count using CLI
      val nThreads: Int = args.headOption.flatMap(x => Try(x.toInt).toOption).getOrElse(0)

      val layeredRoutes =
        routes.routes.mapError { e =>
          Response(
            Status.InternalServerError,
            body = Body.fromString(s"I haz problem ${e.toString}")
          )
        }

      val config = Server.Config.default
        .port(PORT)
      val nettyConfig = NettyConfig.default
        .leakDetection(LeakDetectionLevel.PARANOID)
        .maxThreads(nThreads)
      val configLayer = ZLayer.succeed(config)
      val nettyConfigLayer = ZLayer.succeed(nettyConfig)

      (Server.install(layeredRoutes).flatMap { port =>
        Console.printLine(s"Started server on port: $port")
      } *> ZIO.never)
        .provide(
          configLayer,
          nettyConfigLayer,
          Server.customized,
          Scope.default,
          zio.telemetry.opentelemetry.Tracing.live,
          TracingClient.tracingClientLayer,
          ZipkinExportingTracer.live,
          B3HTTPResponseTracing.layer,
          ZClient.default,
          loggingLayer
        )
    }
  }

}
