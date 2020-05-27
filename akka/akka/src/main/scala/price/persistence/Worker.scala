package price.persistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.SessionActor

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Worker {

  sealed trait Command

  private final case class GotCount(count: Int)
    extends Command

  private final case class Done() extends Command

  def apply(product: String, dbSession: SlickSession, replyTo: ActorRef[SessionActor.Command]): Behavior[Command] =
    Behaviors.setup { context =>
      replyTo ! SessionActor.DatabaseLookupResponse(1, "CHUJ")

      implicit val ec: ExecutionContext = context.executionContext

      val count: Future[Int] = RequestsDAO.lookup (product, dbSession)
      context.pipeToSelf (count) {
        case Success (value) =>
          GotCount (value)
        case Failure (exception) =>
          exception match {
            case _: NoSuchElementException => GotCount (0)
            case e: Exception =>
              context.log.error (s"Lookup failed with: ${
                e.getMessage
              } ${
                e.toString
              }")
              Done ()
          }
      }

      Behaviors.receive { (context, msg) =>
        msg match {
          case GotCount(count) =>
            replyTo ! SessionActor.DatabaseLookupResponse(count, product)
            context.log.info(s"Responded with $count to ${replyTo.path}")
            Behaviors.same
          case Done() =>
            Behaviors.stopped
        }
      }
    }
}
