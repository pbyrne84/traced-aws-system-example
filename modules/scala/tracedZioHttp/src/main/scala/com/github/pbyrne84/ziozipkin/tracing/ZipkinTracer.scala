package com.github.pbyrne84.ziozipkin.tracing

import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporterBuilder
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import zio.*

object TracerConfig {
  val live: ZLayer[Any, Nothing, TracerConfig] = ZLayer {
    ZIO.succeed(TracerConfig("http://localhost:9411"))
  }
}

final case class TracerConfig(host: String)

object ZipkinTracer {

  def live: RLayer[TracerConfig, Tracer] =
    ZLayer {
      for {
        config <- ZIO.service[TracerConfig]
        tracer <- makeTracer(config.host)
      } yield tracer
    }

  private def makeTracer(host: String): Task[Tracer] = {
    for {
      spanExporter <- ZIO.attempt(new ZipkinSpanExporterBuilder().setEndpoint(host).build())
      spanProcessor <- ZIO.succeed(SimpleSpanProcessor.create(spanExporter))
      tracerProvider <- ZIO.attempt(
        SdkTracerProvider
          .builder()
          .setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "zio-example")))
          .addSpanProcessor(spanProcessor)
          .build()
      )
      openTelemetry <- ZIO.succeed(OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build())
      tracer <- ZIO.succeed(openTelemetry.getTracer("zio.telemetry.opentelemetry.example.JaegerTracer"))
    } yield tracer
  }

}
