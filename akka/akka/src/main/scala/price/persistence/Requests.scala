package price.persistence
import slick.jdbc.PostgresProfile.api._

class Requests(tag: Tag) extends Table[(String, Int)](tag, "REQUESTS") {
  def query = column[String]("QUERY", O.PrimaryKey)
  def count = column[Int]("COUNT")
  def * = (query, count)
}
