package price.persistence
import slick.jdbc.PostgresProfile.api._

class Requests(tag: Tag) extends Table[(String, Int)](tag, "requests") {
  def query = column[String]("query", O.PrimaryKey)
  def count = column[Int]("count")
  def * = (query, count)
}
