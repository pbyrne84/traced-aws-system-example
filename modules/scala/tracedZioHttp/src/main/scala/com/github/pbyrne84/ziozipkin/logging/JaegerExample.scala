package com.github.pbyrne84.ziozipkin.logging

import com.github.pbyrne84.ziozipkin.tracing.B3JaegerTracer
import io.jaegertracing.internal.JaegerTracer
import io.opentracing.propagation.{Format, TextMapAdapter}

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava

object JaegerExample {
  def main(args: Array[String]): Unit = {
    val jaegerTracer: JaegerTracer = B3JaegerTracer.createTracer("jaeger-example")

    val java: util.Map[String, String] = Map(
      B3JaegerTracer.headerName.traceId -> "e7bcaeac5232cc7b",
      B3JaegerTracer.headerName.spanId -> "e7bcaeac5232cc7c"
    ).asJava

    val spanContext = jaegerTracer.extract(
      Format.Builtin.HTTP_HEADERS,
      new TextMapAdapter(
        java
      )
    )

    val span = jaegerTracer.buildSpan("aaa").asChildOf(spanContext).start()

    jaegerTracer.activateSpan(span)

    println(spanContext.toSpanId)
    println(spanContext.toTraceId)
  }
}
