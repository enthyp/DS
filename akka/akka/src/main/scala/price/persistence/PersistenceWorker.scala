package price.persistence

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.SessionActor

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PersistenceWorker {

  sealed trait Command

  private final case class GotCount(count: Int)
    extends Command

  final case class Finished() extends Command

  def apply(product: String, dbSession: SlickSession, replyTo: ActorRef[SessionActor.DatabaseLookupResponse]): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val ec: ExecutionContext = context.executionContext

      val count: Future[Int] = RequestsDAO.lookup (product, dbSession)
      context.pipeToSelf (count) {
        case Success (value) =>
          GotCount (value)
        case Failure (exception) =>
          exception match {
            case e: Exception =>
              context.log.error (s"Lookup failed with: ${e.getMessage} ${e.toString}")
              Finished()
          }
      }

      Behaviors.receive { (context, msg) =>
        msg match {
          case GotCount(count) =>
            replyTo ! SessionActor.DatabaseLookupResponse(count, product)
            val done: Future[Done] = RequestsDAO.increment(product, dbSession)

            context.pipeToSelf(done) {
              case Success(_) =>
                context.log.info(s"Successful increment for $product")
                Finished()
              case Failure(exception) =>
                context.log.info(s"Failed increment for $product with $exception")
                Finished()
            }
            Behaviors.same
          case Finished() =>
            Behaviors.stopped
        }
      }
    }
}
