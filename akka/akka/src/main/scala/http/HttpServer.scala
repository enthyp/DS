package http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import price.PriceServiceManager

import scala.util.{Failure, Success}

object HttpServer {
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    implicit val classicSystem: akka.actor.ActorSystem = system.classicSystem
    import system.executionContext

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(argv: Array[String]): Unit = {
    val stores = Array("7-11", "Żabka", "Lewiatan")

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val priceServiceManager =
        context.spawn(PriceServiceManager(stores), "price-service-manager")
      context.watch(priceServiceManager)  // TODO: handle Terminate? Supervise?

      val routes = new ServiceRoutes(priceServiceManager)(context.system)
      startHttpServer(routes.serviceRoutes, context.system)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "HTTP-server")
  }
}
