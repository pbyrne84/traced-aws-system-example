package com.github.pbyrne84.ziozipkin

import com.github.pbyrne84.ziozipkin.client.TracingClient
import com.github.pbyrne84.ziozipkin.route.{RoutesBuild, ZioRoutes}
import com.github.pbyrne84.ziozipkin.tracing.{B3HTTPResponseTracing, ZipkinExportingTracer}
import io.jaegertracing.Configuration
import io.jaegertracing.internal.samplers.ConstSampler
import io.jaegertracing.zipkin.ZipkinV2Reporter
import io.opentracing.Tracer
import org.apache.http.client.utils.URIBuilder
import org.slf4j.bridge.SLF4JBridgeHandler
import zio._
import zio.http._
import zio.http.netty.NettyConfig
import zio.http.netty.NettyConfig.LeakDetectionLevel
import zio.logging.backend.SLF4J
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.tracing.Tracing
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

import scala.util.Try
final case class TracerConfig(host: String)
final case class BackendConfig(host: String, port: Int)
final case class ProxyConfig(host: String, port: Int)
final case class AppConfig(proxy: ProxyConfig, backend: BackendConfig, tracer: TracerConfig)

object JaegerTracer {

  def live(serviceName: String): RLayer[AppConfig, Tracer] =
    ZLayer {
      for {
        config <- ZIO.service[AppConfig]
        tracer <- makeTracer(config.tracer.host, serviceName)
      } yield tracer
    }

  def makeTracer(host: String, serviceName: String): Task[io.jaegertracing.internal.JaegerTracer] =
    for {
      url <- ZIO.attempt(new URIBuilder().setScheme("http").setHost(host).setPath("/api/v2/spans").build.toString)
      senderBuilder <- ZIO.attempt(OkHttpSender.newBuilder.compressionEnabled(true).endpoint(url))
      tracer <- ZIO.attempt(
        new Configuration(serviceName).getTracerBuilder
          .withSampler(new ConstSampler(true))
          .withReporter(new ZipkinV2Reporter(AsyncReporter.create(senderBuilder.build)))
          .build
      )
    } yield tracer

}

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
          TracingClient.tracingClientLayer,
          Tracing.live(true),
          ZipkinExportingTracer.live,
          B3HTTPResponseTracing.layer,
          JaegerTracer.live("moo"),
          ZClient.default,
          OpenTelemetry.contextZIO,
          loggingLayer
        )
    }
  }

}
