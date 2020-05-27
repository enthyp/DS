package price.session

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps

object PriceLookupActor {

  sealed trait Command

  private final case class LookedUp() extends Command

  def apply(store: String, product: String, replyTo: ActorRef[SessionActor.Command]): Behavior[Command] =
    Behaviors.withTimers { timers =>
      val lookupTime =
        const.PriceLookupTimeLower + Random.nextInt(const.PriceLookupTimeUpper - const.PriceLookupTimeLower + 1)
      timers.startSingleTimer(LookedUp, LookedUp(), lookupTime millis)

      Behaviors.receiveMessage { _ =>
        val price = 1 + Random.nextInt(10)
        replyTo ! SessionActor.PriceLookupResponse(price, store)
        Behaviors.stopped
      }
    }

}
