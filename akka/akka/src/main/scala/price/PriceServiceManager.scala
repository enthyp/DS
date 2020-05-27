package price

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import price.persistence.PersistenceManager

final case class PriceEntry(price: Int, store: String)

object PriceServiceManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[ReplyComparePrices]) extends Request

  final case class ReplyComparePrices(product: String, priceEntry: Option[PriceEntry], requestCount: Option[Int])

  def apply(stores: Array[String], persistenceManager: ActorRef[PersistenceManager.RequestHandleQuery]): Behavior[Request] =
    Behaviors.receive { (context, msg) =>
      msg match {
        case RequestComparePrices(product, replyTo) =>
          context.spawnAnonymous(SessionActor(product, stores, persistenceManager, replyTo))
          Behaviors.same
      }
    }
}
