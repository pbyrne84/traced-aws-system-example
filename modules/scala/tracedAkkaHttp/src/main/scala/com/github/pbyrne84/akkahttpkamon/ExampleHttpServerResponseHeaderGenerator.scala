package com.github.pbyrne84.akkahttpkamon

import com.typesafe.scalalogging.StrictLogging
import kamon.context.Context
import kamon.instrumentation.http.HttpServerResponseHeaderGenerator
import kamon.trace.Span

class ExampleHttpServerResponseHeaderGenerator extends HttpServerResponseHeaderGenerator with StrictLogging {
  override def headers(context: Context): Map[String, String] = {

    Map(
      "bananana" -> "banana"
    )

  }
}
