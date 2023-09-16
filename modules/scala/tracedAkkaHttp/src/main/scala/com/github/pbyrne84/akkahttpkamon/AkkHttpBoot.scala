package com.github.pbyrne84.akkahttpkamon

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import kamon.Kamon

import scala.concurrent.{ExecutionContextExecutor, Future}

object AkkHttpBoot extends StrictLogging with ErrorAccumulatingCirceSupport {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {

    Kamon.init()

    val port = 8080
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)

    println(s"Server now online. Please navigate to http://localhost:$port/test-success")
    println(s"or please navigate to http://localhost:$port/test-fail")
//    StdIn.readLine() // let it run until user presses return
//    bindingFuture
//      .flatMap(_.unbind()) // trigger unbinding from the port
//      .onComplete(_ => system.terminate()) // and shutdown when done
  }

  private def routes: Route =
    pathPrefix("test-success") {
      concat {
        pathEnd {
          concat {
            get {
              onSuccess(Future.successful {
                logger.info("hello")
                "result"
              }) { a: String =>
                complete(StatusCodes.OK)
              }
            }
          }
        }
      }
    } ~
      pathPrefix("test-fail") {
        concat {
          pathEnd {
            concat {
              get {
                onComplete(Future.failed[String] {
                  throw new RuntimeException("I had a problem")
                }) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          }
        }
      }

}
