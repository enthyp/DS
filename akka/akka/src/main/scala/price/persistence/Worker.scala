package price.persistence

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Sink
import price.SessionActor
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object Worker {

  sealed trait Event

  private final case class GotCount(count: Int) extends Event

  private final case class Done() extends Event

  def apply(product: String,
            replyTo: ActorRef[SessionActor.DatabaseLookupResponse],
            dbSession: SlickSession): Behavior[Event] =
    Behaviors.setup { context =>
      val materializer: Materializer = Materializer(context)
      implicit val ec: ExecutionContextExecutor = context.executionContext

      val count: Future[Int] = lookup(product)(dbSession, materializer)
      context.pipeToSelf(count) {
        case Success(value) =>
          GotCount(value)
        case Failure(exception) =>
          exception match {
            case _: NoSuchElementException => GotCount(0)
            case e: Exception =>
              context.log.error(s"Lookup failed with: ${e.getMessage} ${e.toString}")
              Done()
          }
      }

      receive(product, replyTo)
    }

  private def receive(product: String,
                      replyTo: ActorRef[SessionActor.DatabaseLookupResponse]): Behavior[Event] =
    Behaviors.receive { (context, msg) =>
      msg match {
        case GotCount(count) =>
          replyTo ! SessionActor.DatabaseLookupResponse(count, product)
          context.log.info(s"Responded with $count to ${replyTo.path}")

          val inc: Future[Unit] = increment(product)
          context.pipeToSelf(inc) { _ => Done() }
          Behaviors.same
        case Done() =>
          Behaviors.stopped
      }
    }

  private def lookup(product: String)(implicit session: SlickSession, materializer: Materializer): Future[Int] = {
    Slick
      .source(TableQuery[Requests].filter(_.query === product).map(_.count).result)
      .runWith(Sink.last)
  }

  private def increment(product: String): Future[Unit] = {
    Future.successful()
  }
}
