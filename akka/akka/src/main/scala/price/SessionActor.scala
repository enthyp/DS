package price

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.persistence.Worker

import scala.language.postfixOps
import scala.util.Random

object SessionActor {

  sealed trait Command

  final case class PriceLookupResponse(price: Int, store: String) extends Command

  final case class DatabaseLookupResponse(count: Int, product: String) extends Command

  private final case class TimedOut() extends Command

  def apply(product: String,
            stores: Array[String],
            dbSession: SlickSession,
            replyTo: ActorRef[PriceServiceManager.ReplyComparePrices]): Behavior[Command] =
    Behaviors.setup { context =>
      // Spawn price and DB lookup actors
      val sampledStores = Random.shuffle(stores.toList).take(2).toArray

      for (store <- sampledStores)
        context.spawnAnonymous(PriceLookupActor(store, product, context.self))
      context.spawnAnonymous(Worker(product, dbSession, context.self))

      Behaviors.withTimers { timers =>
        new SessionActor(context, product, sampledStores, dbSession, replyTo, timers)
      }
    }
}

class SessionActor(context: ActorContext[SessionActor.Command],
                   product: String,
                   stores: Array[String],
                   dbSession: SlickSession,
                   replyTo: ActorRef[PriceServiceManager.ReplyComparePrices],
                   timers: TimerScheduler[SessionActor.Command])
  extends AbstractBehavior[SessionActor.Command](context) {

  import SessionActor._

  // TODO: affected by lifecycle?
  timers.startSingleTimer(TimedOut(), TimedOut(), const.ComparePricesTimeout)

  private var pending: Set[String] = stores.toSet
  private var replies: Map[String, Option[PriceEntry]] = Map.empty
  private var requestCount: Option[Int] = None

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case PriceLookupResponse(price, store) => onPriceLookupResponse(price, store)
      case DatabaseLookupResponse(count, product) => onDBLookupResponse(count, product)
      case TimedOut() => collect()
    }
  }

  private def onPriceLookupResponse(price: Int, store: String): Behavior[Command] = {
    if (pending contains store) {
      pending -= store
      replies += store -> Option(PriceEntry(price, store))
    }

    if (pending.isEmpty && requestCount.isDefined)
      collect()
    else
      this
  }

  private def onDBLookupResponse(count: Int, product: String): Behavior[Command] = {
    if (product == this.product && requestCount.isEmpty) {
      requestCount = Option(count)
    }
    if (pending isEmpty)
      collect()
    else
      this
  }

  private def collect(): Behavior[Command] = {
    for (store <- pending)
      replies += store -> None

    val reply1 = replies(stores(0))
    val reply2 = replies(stores(1))

    val price: Option[PriceEntry] = (reply1, reply2) match {
      case (Some(entry1), Some(entry2)) =>
        if (entry1.price < entry2.price)
          reply1
        else
          reply2
      case (Some(_), None) => reply1
      case _ => reply2
    }

    replyTo ! PriceServiceManager.ReplyComparePrices(product, price, requestCount)
    Behaviors.stopped
  }
}
