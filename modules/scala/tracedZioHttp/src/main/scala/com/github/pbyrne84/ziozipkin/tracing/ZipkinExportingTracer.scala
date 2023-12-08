package com.github.pbyrne84.ziozipkin.tracing

import com.typesafe.scalalogging.StrictLogging
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import zio.{ZIO, ZLayer}

/** We are not going to send the span details anywhere, we are just going to manage the creation of
  * child spans etc.
  *
  * TracerProvider.noop().tracerBuilder("").build() build something that does not create child span
  * ids and it makes things confusing if we are trying to see child spans etc.
  */
object ZipkinExportingTracer extends StrictLogging {

  val live: ZLayer[Any, Throwable, Tracer] = ZLayer(
    for {
      spanProcessor <- ZIO.attempt(SimpleSpanProcessor.create(createZipkinSpanExporter))
      tracerProvider <- ZIO.succeed(
        SdkTracerProvider
          .builder()
          .addSpanProcessor(spanProcessor)
          .setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "zio_http_demo")))
          .build()
      )
      openTelemetry <- ZIO.succeed(
        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
      )
      tracer <- ZIO.succeed(
        openTelemetry.getTracer(getClass.getName)
      )
    } yield tracer
  )

  private def createZipkinSpanExporter: ZipkinSpanExporter = {
    ZipkinSpanExporter
      .builder()
      .setEndpoint("http://127.0.0.1:9411/api/v2/spans")
      .build()
  }

}
