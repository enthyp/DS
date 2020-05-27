package price.persistence

import akka.Done
import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}


object RequestsDAO {

  private val requests = TableQuery[Requests]

  def lookup(product: String, dbSession: SlickSession)(implicit executionContext: ExecutionContext): Future[Int] = {
    dbSession.db.run(
      requests.filter(_.query === product).map(_.count).result.headOption
    ) map {
      case Some(value) => value
      case None => 0
    }
  }

  def increment(product: String, dbSession: SlickSession)(implicit executionContext: ExecutionContext): Future[Done] = {
    val query =
      sqlu"""
            INSERT INTO requests (query, count)
            VALUES (${product}, 1)
            ON CONFLICT (query)
            DO UPDATE SET count = requests.count + 1
        """
    dbSession.db.run(query)
    Future.successful(Done)
  }
}
