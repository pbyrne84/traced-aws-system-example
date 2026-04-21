package com.github.pbyrne84.ziozipkin.tracing

import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.propagation.B3TextMapCodec
import io.jaegertracing.internal.samplers.ConstSampler
import io.jaegertracing.zipkin.ZipkinV2Reporter
import io.jaegertracing.{internal, Configuration}
import io.opentracing.propagation.{Format, TextMapAdapter}
import org.apache.http.client.utils.URIBuilder
import zio.http.Headers
import zio.telemetry.opentracing.OpenTracing
import zio.{Task, ZIO, ZLayer}
import zipkin2.reporter.AsyncReporter
import zipkin2.reporter.okhttp3.OkHttpSender

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava

object B3JaegerTracer {

  object headerName {
    val traceId = "X-B3-TraceId"
    val spanId = "X-B3-SpanId"
    val sampled = "X-B3-Sampled"
  }

  def makeTracer(host: String, serviceName: String): Task[internal.JaegerTracer] =
    for {
      url <- ZIO.attempt(new URIBuilder().setScheme("http").setHost(host).setPath("/api/v2/spans").build.toString)
      senderBuilder <- ZIO.attempt(OkHttpSender.newBuilder.compressionEnabled(true).endpoint(url))
      tracer <- ZIO.attempt(
        createB3Builder(serviceName)
          .withReporter(new ZipkinV2Reporter(AsyncReporter.create(senderBuilder.build)))
          .build
      )
    } yield tracer

  private def createB3Builder(serviceName: String): JaegerTracer.Builder = {
    val b3Codec = new B3TextMapCodec.Builder().build()
    new Configuration(serviceName).getTracerBuilder
      .withSampler(new ConstSampler(true))
      .registerInjector(Format.Builtin.HTTP_HEADERS, b3Codec)
      .registerExtractor(Format.Builtin.HTTP_HEADERS, b3Codec)
  }

  def createTracerLayerFromRequestHeaders(
      serviceName: String,
      headers: Headers
  ): ZLayer[Any, Throwable, JaegerTracer & OpenTracing] = {

    val traceId = headers.headers.get(B3JaegerTracer.headerName.traceId).getOrElse("")
    val spanId = headers.headers.get(B3JaegerTracer.headerName.spanId).getOrElse("")

    createTracerLayer(serviceName, traceId, spanId)
  }

  def createTracerLayer(
      serviceName: String,
      traceId: String,
      spanId: String
  ): ZLayer[Any, Throwable, JaegerTracer & OpenTracing] = {

    val tracerLayer: ZLayer[Any, Nothing, JaegerTracer] = ZLayer(ZIO.succeed(createTracer(serviceName)))
    val tracerService: ZLayer[JaegerTracer, Nothing, OpenTracing] =
      ZLayer.scoped(ZIO.service[JaegerTracer].flatMap { (jaegerTracer: JaegerTracer) =>
        val b3HeaderMap: util.Map[String, String] = Map(
          B3JaegerTracer.headerName.traceId -> traceId,
          B3JaegerTracer.headerName.spanId -> spanId
        ).asJava

        val spanContext = createTracer(serviceName).extract(
          Format.Builtin.HTTP_HEADERS,
          new TextMapAdapter(b3HeaderMap)
        )

        val span = jaegerTracer.buildSpan("aaa").asChildOf(spanContext).start()

        jaegerTracer.activateSpan(span)
        OpenTracing.scoped(jaegerTracer, "ROOT")
      })

    tracerLayer ++ (tracerLayer >>> tracerService)

  }

  def createTracer(serviceName: String): JaegerTracer = {
    createB3Builder(serviceName).build()
  }

}
