package controllers

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.{Context, Scope}
import org.slf4j.Logger
import play.api.mvc.{EssentialAction, EssentialFilter, Result}
import tracing.RequestTraceContext

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RequestScopeFilter @Inject() (tracer: Tracer, requestTraceContext: RequestTraceContext) extends EssentialFilter {

  protected lazy val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  override def apply(next: EssentialAction): EssentialAction = { (rh) =>
    implicit val ec: ExecutionContext = ExecutionContext.parasitic
    val requestId = rh.id.toString
    val baggage = Baggage.builder().put("request_id", requestId).build()
    val baggageScope: Scope = baggage.storeInContext(Context.current()).makeCurrent

    val parentContext =
      requestTraceContext.parentContextFromRequest(rh)

    val span =
      tracer
        .spanBuilder("incoming-request")
        .setParent(parentContext)
        .startSpan()

    val scope = span.makeCurrent()

    val s = tracer
      .spanBuilder("woof")
      .setAttribute("product.id", "sss")
      .startSpan();

    next(rh).map { (result: Result) =>
      println("oneandonly")
      s.end()
      result
    }
  }
}
