package price.session

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import price.PriceServiceManager
import price.persistence.PersistenceManager

final case class PriceEntry(price: Int, store: String)

object SessionManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[PriceServiceManager.ReplyComparePrices])
    extends Request

  def apply(stores: Array[String], persistenceManager: ActorRef[PersistenceManager.RequestHandleQuery]): Behavior[Request] =
    Behaviors.receive { (context, msg) =>
      msg match {
        case RequestComparePrices(product, replyTo) =>
          context.spawnAnonymous(SessionActor(product, stores, persistenceManager, replyTo))
          Behaviors.same
      }
    }
}
