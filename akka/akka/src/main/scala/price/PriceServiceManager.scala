package price

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.persistence.PersistenceManager

final case class PriceEntry(price: Int, store: String)

object PriceServiceManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[ReplyComparePrices]) extends Request

  final case class ReplyComparePrices(product: String, priceEntry: Option[PriceEntry], requestCount: Option[Int])

  def apply(stores: Array[String], persistenceManager: ActorRef[PersistenceManager.Command]): Behavior[Request] =
    Behaviors.setup { context => new PriceServiceManager(context, stores, persistenceManager)}
}

class PriceServiceManager(context: ActorContext[PriceServiceManager.Request],
                          val stores: Array[String],
                          val persistenceManager: ActorRef[PersistenceManager.Command])
  extends AbstractBehavior[PriceServiceManager.Request](context) {
  import PriceServiceManager._

  override def onMessage(msg: PriceServiceManager.Request): Behavior[PriceServiceManager.Request] =
    msg match {
      case RequestComparePrices(product, replyTo) =>
        context.spawnAnonymous(SessionActor(product, stores, persistenceManager, replyTo))
        this
    }
}
