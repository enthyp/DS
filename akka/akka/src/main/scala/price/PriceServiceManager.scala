package price

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession

final case class PriceEntry(price: Int, store: String)

object PriceServiceManager {

  sealed trait Request

  final case class RequestComparePrices(product: String, replyTo: ActorRef[ReplyComparePrices]) extends Request

  final case class ReplyComparePrices(product: String, priceEntry: Option[PriceEntry], requestCount: Option[Int])

  def apply(stores: Array[String], dbSession: SlickSession): Behavior[Request] =
    Behaviors.receive { (context, msg) =>
      msg match {
        case RequestComparePrices(product, replyTo) =>
          context.spawnAnonymous(SessionActor(product, stores, dbSession, replyTo))
          Behaviors.same
      }
    }
}
