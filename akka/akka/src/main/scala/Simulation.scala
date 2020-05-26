import scala.language.postfixOps
import SimulationSupervisor.Command
import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import price.PriceServiceManager


object SimulationSupervisor {

  sealed trait Command

  final case class RequestComparePrices(clientId: Int, product: String) extends Command

  final case class Die() extends Command

  def apply(numClients: Int, stores: Array[String]): Behavior[Command] =
    Behaviors.setup(context => new SimulationSupervisor(context, numClients, stores))
}

class SimulationSupervisor(context: ActorContext[Command], numClients: Int, stores: Array[String])
  extends AbstractBehavior[Command](context) {

  import SimulationSupervisor._

  private val priceServiceManager = context.spawn(PriceServiceManager(), "price-service-manager")

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
      context.stop(context.self)
      this
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
    val system = ActorSystem(SimulationSupervisor(numClients, stores), "simulation")

    for (line <- io.Source.stdin.getLines()) {
      try {
        val Array(clientId, productName) = line split (":") map (_.trim())
        system ! SimulationSupervisor.RequestComparePrices(clientId toInt, productName)
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }
}
