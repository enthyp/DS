import scala.language.postfixOps
import scala.util.control.Breaks._
import SimulationSupervisor.Command
import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import price.PriceServiceManager
import price.persistence.PersistenceManager
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object SimulationSupervisor {

  sealed trait Command

  final case class RequestComparePrices(clientId: Int, product: String) extends Command

  final case class Die() extends Command

  def apply(numClients: Int, stores: Array[String], session: SlickSession): Behavior[Command] =
    Behaviors.setup { context =>

      new SimulationSupervisor(context, numClients, stores, session)
    }
}

class SimulationSupervisor(context: ActorContext[Command],
                           numClients: Int,
                           stores: Array[String],
                           dbSession: SlickSession)
  extends AbstractBehavior[Command](context) {

  import SimulationSupervisor._

  private val persistenceManager = context.spawn(PersistenceManager(dbSession), "persistence-manager")

  private val priceServiceManager =
    context.spawn(PriceServiceManager(stores, persistenceManager), "price-service-manager")

  private val clients = (0 to numClients) map { id =>
    context.spawn(ClientActor(id, priceServiceManager), s"client-$id")
  } toArray

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case RequestComparePrices(id, product) =>
      if (id < numClients)
        clients(id) ! ClientActor.RequestComparePrices(product)
      else
        println(s"No such client: $id")
      this
    case Die() =>
      Behaviors.stopped
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Simulation shutdown")
      this
  }
}

object Simulation {
  def main(args: Array[String]): Unit = {
    val numClients = 5
    val stores = Array("7-11", "Żabka", "Lewiatan")
    val session: SlickSession = setupDB()
    val system: ActorSystem[Command] = ActorSystem(SimulationSupervisor(numClients, stores, session), "simulation")

    breakable {
      for (line <- io.Source.stdin.getLines()) {
        try {
          if (line == "q") {
            system ! SimulationSupervisor.Die()
            break
          }

          val Array(clientId, productName) = line split (":") map (_.trim())
          system ! SimulationSupervisor.RequestComparePrices(clientId toInt, productName)
        } catch {
          case _: MatchError => println("Incorrect input!")
          case e: Exception =>
            e.printStackTrace()
        }
      }
    }

    session.close()
  }

  private def setupDB(): SlickSession = {
    val dbConfig = DatabaseConfig.forConfig[JdbcProfile]("slick-postgres")
    SlickSession.forConfig(dbConfig)
  }
}
