package http

import price.PriceEntry
import price.PriceServiceManager.ReplyComparePrices

object JsonFormats {
  import spray.json.DefaultJsonProtocol._

  implicit val priceEntryJsonFormat = jsonFormat2(PriceEntry)
  implicit val replyComparePricesJsonFormat = jsonFormat3(ReplyComparePrices)
}
