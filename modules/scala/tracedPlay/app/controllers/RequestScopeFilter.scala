package controllers

import io.opentelemetry.context.{Context, Scope}
import org.slf4j.{Logger, MDC}
import play.api.mvc.{ControllerComponents, EssentialAction, EssentialFilter, Result}
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Tracer
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RequestScopeFilter @Inject() (tracer: Tracer) extends EssentialFilter {

  protected lazy val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  override def apply(next: EssentialAction): EssentialAction = { rh =>
    implicit val ec: ExecutionContext = ExecutionContext.parasitic
    val requestId = rh.id.toString
    val baggage = Baggage.builder().put("request_id", requestId).build()
    val baggageScope: Scope = baggage.storeInContext(Context.current()).makeCurrent

    MDC.put("meow", "noop")

    println("??? " + MDC.getCopyOfContextMap)

    val s = tracer
      .spanBuilder("woof")
      .setAttribute("product.id", "sss")
      .startSpan();

    next(rh).map { (result: Result) =>
      s.end()
      result
    }
  }
}
