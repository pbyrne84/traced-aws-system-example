package tracing

import io.opentelemetry.api.trace.{Span, SpanContext, TraceFlags, TraceState}
import io.opentelemetry.context.Context
import play.api.mvc.RequestHeader

import java.security.SecureRandom
import javax.inject.Inject

class RequestTraceContext @Inject() (hexStringGenerator: HexStringGenerator) {
  private val TraceIdHeader = "X-B3-TraceId"
  private val SpanIdHeader = "X-B3-SpanId"

  private val random = new SecureRandom()

  def parentContextFromRequest(rh: RequestHeader): Context = {
    val maybeTraceIdHeader = rh.headers
      .get(B3Headers.name.traceId)

    val unsafe = B3Headers.TraceId.createUnsafe
    val traceId =
      maybeTraceIdHeader
        .flatMap(traceIdHeader => B3Headers.TraceId.attempt(traceIdHeader).toOption)
        .getOrElse(unsafe)

    val maybeSpanId = rh.headers
      .get(B3Headers.name.spanId)

    val spanId = maybeSpanId
      .flatMap(spanIdHeader => B3Headers.SpanId.attempt(spanIdHeader).toOption)
      .getOrElse(B3Headers.SpanId.createUnsafe)

    val remoteParent =
      SpanContext.createFromRemoteParent(
        traceId.value,
        spanId.value,
        TraceFlags.getSampled,
        TraceState.getDefault
      )

    Context.current().`with`(Span.wrap(remoteParent))
  }
}
