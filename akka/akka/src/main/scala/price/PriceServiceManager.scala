package price

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

object PriceServiceManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[ReplyComparePrices]) extends Request

  final case class ReplyComparePrices(product: String, price1: Option[Int], price2: Option[Int], requestCount: Option[Int])

  def apply(): Behavior[Request] =
    Behaviors.setup { context => new PriceServiceManager(context)}
}

class PriceServiceManager(context: ActorContext[PriceServiceManager.Request])
  extends AbstractBehavior[PriceServiceManager.Request](context) {
  import PriceServiceManager._

  override def onMessage(msg: PriceServiceManager.Request): Behavior[PriceServiceManager.Request] =
    msg match {
      case RequestComparePrices(product, replyTo) =>
        val msg = ReplyComparePrices(product, Option(10), Option(150), Option(3))
        replyTo ! msg
        this
    }
}
