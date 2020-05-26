package price

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}

import scala.util.Random
import scala.language.postfixOps

object SessionActor {

  sealed trait Command

  final case class PriceLookupResponse(price: Int, store: String) extends Command

  final case class DatabaseLookupResponse(count: Int, product: String) extends Command

  private final case class TimedOut() extends Command

  def apply(product: String,
            stores: Array[String],
            replyTo: ActorRef[PriceServiceManager.ReplyComparePrices]): Behavior[Command] =
    Behaviors.setup { context =>
      for (store <- stores)
        context.spawnAnonymous(PriceLookupActor(store, product, context.self))

      Behaviors.withTimers { timers =>
        new SessionActor(context, product, stores, replyTo, timers)
      }
    }
}

class SessionActor(context: ActorContext[SessionActor.Command],
                   product: String,
                   stores: Array[String],
                   replyTo: ActorRef[PriceServiceManager.ReplyComparePrices],
                   timers: TimerScheduler[SessionActor.Command])
  extends AbstractBehavior[SessionActor.Command](context) {

  import SessionActor._

  // TODO: affected by lifecycle?
  timers.startSingleTimer(TimedOut(), TimedOut(), const.ComparePricesTimeout)

  private var sampledStores = Random.shuffle(stores.toList).take(2)
  private var pending: Set[String] = sampledStores.toSet
  private var replies: Map[String, Option[PriceEntry]] = Map.empty

  override def onMessage(msg: Command): Behavior[Command] = awaiting()

  private def awaiting(): Behavior[Command] = Behaviors.receiveMessage { msg =>
    msg match {
      case PriceLookupResponse(price, store) => onPriceLookupResponse(price, store)
      case DatabaseLookupResponse(count, product) =>
      case TimedOut() => collect()

    }
    Behaviors.same
  }

  private def onPriceLookupResponse(price: Int, store: String): Behavior[Command] = {
    if (pending contains store) {
      pending -= store
      replies += store -> Option(PriceEntry(price, store))
    }

    if (pending isEmpty)
      collect()
    else
      Behaviors.same
  }

  private def collect(): Behavior[Command] = {
    for (store <- pending)
      replies += store -> None

    val reply1 = replies(sampledStores(0))
    val reply2 = replies(sampledStores(1))

    val price: Option[PriceEntry] = (reply1, reply2) match {
      case (Some(entry1), Some(entry2)) =>
        if (entry1.price < entry2.price)
          reply1
        else
          reply2
      case (Some(_), None) => reply1
      case _ => reply2
    }

    // TODO: DB also
    replyTo ! PriceServiceManager.ReplyComparePrices(product, price, Option(1))
    Behaviors.stopped
  }
}
