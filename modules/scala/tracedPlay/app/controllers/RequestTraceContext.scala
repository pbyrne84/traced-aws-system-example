package controllers
import io.opentelemetry.api.trace.{Span, SpanContext, TraceFlags, TraceState}
import io.opentelemetry.context.Context
import play.api.mvc.RequestHeader
import java.security.SecureRandom

object RequestTraceContext {
  private val TraceIdHeader = "X-Trace-Id"

  private val TraceIdRegex = "^[0-9a-f]{32}$".r
  private val SpanIdRegex = "^[0-9a-f]{16}$".r

  private val random = new SecureRandom()

  def parentContextFromRequest(rh: RequestHeader): Context = {
    val traceId =
      rh.headers
        .get(TraceIdHeader)
        .map(_.trim.toLowerCase)
        .filter(isValidTraceId)
        .getOrElse(newTraceId())

    val parentSpanId = newSpanId()

    val remoteParent =
      SpanContext.createFromRemoteParent(
        traceId,
        parentSpanId,
        TraceFlags.getSampled,
        TraceState.getDefault
      )

    Context.current().`with`(Span.wrap(remoteParent))
  }

  private def isValidTraceId(value: String): Boolean =
    TraceIdRegex.matches(value) &&
      value != "00000000000000000000000000000000"

  def traceIdFromRequestOrNew(rh: RequestHeader): String =
    rh.headers
      .get(TraceIdHeader)
      .map(_.trim.toLowerCase)
      .filter(isValidTraceId)
      .getOrElse(newTraceId())

  private def newTraceId(): String =
    Option(randomHex(16)).filter(isValidTraceId).getOrElse(newTraceId())

  private def newSpanId(): String =
    Option(randomHex(8)).filter(isValidSpanId).getOrElse(newSpanId())

  private def isValidSpanId(value: String): Boolean =
    SpanIdRegex.matches(value) &&
      value != "0000000000000000"

  private def randomHex(bytes: Int): String = {
    val buffer = new Array[Byte](bytes)
    random.nextBytes(buffer)
    buffer.map(b => f"${b & 0xff}%02x").mkString
  }
}
