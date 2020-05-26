import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import price.PriceServiceManager

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
          response.price1 match {
            case Some(price) => println(s"${response.product} in store 1: $price")
            case None => println(s"${response.product} in store 1: N/A")
          }
          response.price2 match {
            case Some(price) => println(s"${response.product} in store 2: $price")
            case None => println(s"${response.product} in store 2: N/A")
          }
          response.requestCount match {
            case Some(count) => println(s"${response.product} requests so far: $count")
            case None => println(s"${response.product} requests so far: N/A")
          }

          Behaviors.same
      }
    }
}
