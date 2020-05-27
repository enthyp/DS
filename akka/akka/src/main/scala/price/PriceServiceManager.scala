package price

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import price.persistence.PersistenceManager
import price.session.SessionManager

final case class PriceEntry(price: Int, store: String)

object PriceServiceManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[ReplyComparePrices]) extends Request

  final case class ReplyComparePrices(product: String, priceEntry: Option[PriceEntry], requestCount: Option[Int])

  def apply(stores: Array[String]): Behavior[Request] =
    Behaviors.setup { context =>

      val persistenceManager =
        context.spawn(PersistenceManager(), "persistence-manager")

      val sessionManager =
        context.spawn(SessionManager(stores, persistenceManager), "price-service-manager")

      Behaviors.receiveMessage {
        case RequestComparePrices(product, replyTo) =>
          sessionManager ! SessionManager.RequestComparePrices(product, replyTo)
          Behaviors.same
      }
    }
}
