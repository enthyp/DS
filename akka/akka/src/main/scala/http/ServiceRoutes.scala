package http

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import price.PriceServiceManager
import price.PriceServiceManager.{ReplyComparePrices, RequestComparePrices}

import scala.concurrent.Future
import akka.util.Timeout

class ServiceRoutes(priceServiceManager: ActorRef[PriceServiceManager.Request])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private implicit val timeout: Timeout = const.HttpTimeout

  def comparePrices(product: String): Future[ReplyComparePrices] =
    priceServiceManager.ask(ref => RequestComparePrices(product, ref))

  val serviceRoutes: Route =
    pathPrefix("price") {
      path(Segment) { product =>
        get {
          onSuccess(comparePrices(product)) { response =>
            complete(response)
          }
        }
      }
    }
}
