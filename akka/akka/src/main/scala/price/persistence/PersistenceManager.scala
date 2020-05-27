package price.persistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.session.SessionActor
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

object PersistenceManager {

  sealed trait Command

  final case class RequestHandleQuery(product: String, replyTo: ActorRef[SessionActor.DatabaseLookupResponse])
    extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val dbSession = setupDB()

      Behaviors
        .receive[Command] { (context, msg: Command) =>
          msg match {
            case RequestHandleQuery(product, replyTo) =>
              context.spawnAnonymous(PersistenceWorker(product, dbSession, replyTo))
              Behaviors.same
          }
        }
        .receiveSignal {
          case (context, PostStop) =>
            dbSession.close()
            context.log.info("Closed DB session")
            Behaviors.same
        }
    }

  private def setupDB(): SlickSession = {
    val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("slick-postgres")
    val requests = TableQuery[Requests]
    dbConfig.db.run(requests.schema.createIfNotExists)
    SlickSession.forConfig(dbConfig)
  }
}
