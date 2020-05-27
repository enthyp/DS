package price.persistence

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}


object RequestsDAO {
  def lookup(product: String, dbSession: SlickSession)(implicit executionContext: ExecutionContext): Future[Int] = {
    val requests = TableQuery[Requests]

    dbSession.db.run(
      requests.filter(_.query === product).map(_.count).result.headOption
    ) map {
      case Some(value) => value
      case None => 0
    }
  }
}
