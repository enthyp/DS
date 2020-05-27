import scala.language.postfixOps
import scala.util.control.Breaks._
import SimulationSupervisor.Command
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors
import price.PriceServiceManager

object SimulationSupervisor {

  sealed trait Command

  final case class RequestComparePrices(clientId: Int, product: String) extends Command

  final case class Die() extends Command

  def apply(numClients: Int, stores: Array[String]): Behavior[Command] =
    Behaviors.setup { context =>
      val priceServiceManager =
        context.spawn(PriceServiceManager(stores), "price-service-manager")

      val clients = (0 to numClients) map { id =>
        context.spawn(ClientActor(id, priceServiceManager), s"client-$id")
      } toArray

      Behaviors
        .receiveMessage[Command] {
          case RequestComparePrices(id, product) =>
            if (id < numClients)
              clients(id) ! ClientActor.RequestComparePrices(product)
            else
              println(s"No such client: $id")
            Behaviors.same
          case Die() =>
            Behaviors.stopped
        }
        .receiveSignal {
          case (context, PostStop) =>
            context.log.info("Simulation shutdown")
            Behaviors.same
        }
    }
}


object Simulation {
  def main(args: Array[String]): Unit = {
    val numClients = 5
    val stores = Array("7-11", "Żabka", "Lewiatan")
    val system: ActorSystem[Command] = ActorSystem(SimulationSupervisor(numClients, stores), "simulation")

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
  }
}
