package price.persistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.SessionActor

object PersistenceManager {

  sealed trait Command

  final case class RequestHandleQuery(product: String, replyTo: ActorRef[SessionActor.DatabaseLookupResponse])
    extends Command

  def apply(dbSession: SlickSession): Behavior[Command] =
    Behaviors.receive { (context, msg) =>
      msg match {
        case RequestHandleQuery(product, replyTo) =>
          context.spawnAnonymous(PersistenceWorker(product, dbSession, replyTo))
          Behaviors.same
      }
    }
}
