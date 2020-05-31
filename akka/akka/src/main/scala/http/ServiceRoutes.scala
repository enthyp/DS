package http

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import price.{PriceEntry, PriceServiceManager}
import price.PriceServiceManager.{ReplyComparePrices, RequestComparePrices}

import scala.concurrent.Future
import akka.util.Timeout

import scala.util.{Failure, Success}

class ServiceRoutes(priceServiceManager: ActorRef[PriceServiceManager.Request])(implicit val system: ActorSystem[_]) {

  private implicit val timeout: Timeout = const.HttpTimeout

  def comparePrices(product: String): Future[ReplyComparePrices] =
    priceServiceManager.ask(ref => RequestComparePrices(product, ref))

  def checkOpinion(product: String): Future[String] =
    OpinionDAO.checkOpinion(product)

  val serviceRoutes: Route =
    concat(
    pathPrefix("price") {
      path(Segment) { product =>
        get {
          onSuccess(comparePrices(product)) { response =>
            val responseStr: String = response.priceEntry match {
              case Some(PriceEntry(price, store)) =>
                s"${response.product} best price: $price in store ${store}\n"
              case None =>
                s"Failed to get any results for ${response.product}\n"
            }

            response.requestCount match {
              case Some(count) =>
                complete(responseStr + s"${response.product} requests so far: $count")
              case None =>
                complete(responseStr + s"${response.product} requests so far: N/A")
            }
          }
        }
      }
    },
      pathPrefix("review") {
        path(Segment) { product =>
          get {
            onComplete(checkOpinion(product)) {
              case Success(response) => complete(response)
              case Failure(_) => complete(s"Failed to fetch opinion about $product")
            }
          }
        }
      }
    )
}
