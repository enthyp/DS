package price

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

final case class PriceEntry(price: Int, store: String)

object PriceServiceManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[ReplyComparePrices]) extends Request

  final case class ReplyComparePrices(product: String, priceEntry: Option[PriceEntry], requestCount: Option[Int])

  def apply(stores: Array[String]): Behavior[Request] =
    Behaviors.setup { context => new PriceServiceManager(context, stores)}
}

class PriceServiceManager(context: ActorContext[PriceServiceManager.Request], val stores: Array[String])
  extends AbstractBehavior[PriceServiceManager.Request](context) {
  import PriceServiceManager._

  override def onMessage(msg: PriceServiceManager.Request): Behavior[PriceServiceManager.Request] =
    msg match {
      case RequestComparePrices(product, replyTo) =>
        context.spawnAnonymous(SessionActor(product, stores, replyTo))
        this
    }
}
