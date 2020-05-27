package http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import org.jsoup.Jsoup

import scala.concurrent.{ExecutionContextExecutor, Future}

object OpinionDAO {

  def checkOpinion(product: String)(implicit system: ActorSystem[_]): Future[String] = {
    val url = s"https://www.opineo.pl/?szukaj=$product&s=2"

    implicit val ec: ExecutionContextExecutor = system.executionContext
    implicit val materializer: Materializer = Materializer(system.classicSystem)
    val timeout = const.HtmlParseTimeout

    val response: Future[HttpResponse] = Http.get(system).singleRequest(HttpRequest(uri = url))
    response
      .flatMap { rsp => rsp.entity.toStrict(timeout) }
      .map { rsp => rsp.data.utf8String }
      .map { body => extractMerit(body) }
  }

  private def extractMerit(htmlBody: String): String = {
    import scala.jdk.CollectionConverters._

    try {
      val doc = Jsoup.parse(htmlBody)
      val itemSection = doc.select("div.shl_i.pl_i").first()
      val meritSection = itemSection.select("div.pl_attr").first()
      val merits = meritSection.select("ul li")
      merits.asScala.map(el => el.text).foldLeft("")(_ + "\n-> " + _)
    } catch {
      case _ => "Failed to extract any merits..."
    }
  }
}
