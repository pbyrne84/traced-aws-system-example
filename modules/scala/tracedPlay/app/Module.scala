import com.google.inject.AbstractModule
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes

object Module {

  val tracer = {

    if (!GlobalOpenTelemetry.isSet) {

      val serviceResource = Resource.getDefault.merge(
        Resource.builder
          .put(ServiceAttributes.SERVICE_NAME, "play-app")
          .put(ServiceAttributes.SERVICE_VERSION, "1.0.0")
          .build
      )

      val tracerProvider: SdkTracerProvider = SdkTracerProvider.builder
        .addSpanProcessor(BatchSpanProcessor.builder(LoggingSpanExporter.create()).build)
        .setResource(serviceResource)
        .build

      val openTelemetry: OpenTelemetrySdk =
        OpenTelemetrySdk.builder.setTracerProvider(tracerProvider).buildAndRegisterGlobal

      GlobalOpenTelemetry.getTracer("application")
    } else {
      GlobalOpenTelemetry.getTracer("application")
    }

  }
}

class Module extends AbstractModule {

  private val tracer: Tracer = Module.tracer

  override def configure(): Unit = {
    bind(classOf[Tracer]).toInstance(tracer)
  }
}
