import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import price.{PriceEntry, PriceServiceManager}

object ClientActor {

  sealed trait Command

  final case class RequestComparePrices(product: String) extends Command

  private final case class WrappedReplyComparePrices(response: PriceServiceManager.ReplyComparePrices) extends Command

  def apply(id: Int, serviceManager: ActorRef[PriceServiceManager.Request]): Behavior[Command] =
    Behaviors.setup[Command] { context =>
      val serviceResponseMapper: ActorRef[PriceServiceManager.ReplyComparePrices] =
        context.messageAdapter(reply => WrappedReplyComparePrices(reply))

      Behaviors.receiveMessage[Command] {
        case RequestComparePrices(product) =>
          serviceManager ! PriceServiceManager.RequestComparePrices(product, serviceResponseMapper)
          Behaviors.same
        case WrappedReplyComparePrices(response) =>
          response.priceEntry match {
            case Some(PriceEntry(price, store)) =>
              println(s"${response.product} best price: $price in store ${store}")
            case None =>
              println(s"Failed to get any results for ${response.product}")
          }

          response.requestCount match {
            case Some(count) => println(s"${response.product} requests so far: $count")
            case None => println(s"${response.product} requests so far: N/A")
          }

          Behaviors.same
      }
    }
}
