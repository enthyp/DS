package price.persistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.SessionActor
import slick.jdbc.PostgresProfile.api._


object PersistenceManager {

  sealed trait Command

  final case class RequestHandleQuery(product: String, replyTo: ActorRef[SessionActor.DatabaseLookupResponse])
    extends Command

  def apply(dbSession: SlickSession): Behavior[Command] =
    Behaviors.setup { context =>
      setupDB(dbSession)

      Behaviors.receive { (context, msg) =>
        msg match {
          case RequestHandleQuery(product, replyTo) =>
            context.spawnAnonymous(Worker(product, replyTo, dbSession))
        }
        Behaviors.same
      }
    }

  private def setupDB(dbSession: SlickSession): Unit = {
    // Create counting table if necessary
    val requests = TableQuery[Requests]
    dbSession.db.run(requests.schema.createIfNotExists)
  }
}
