import scala.concurrent.duration._
import scala.language.postfixOps

package object const {
  final val HttpTimeout: FiniteDuration = 1000 millis
  final val HtmlParseTimeout: FiniteDuration = 500 millis
  final val ComparePricesTimeout: FiniteDuration = 300 millis
  final val PriceLower: Int = 1
  final val PriceUpper: Int = 10
  final val PriceLookupTimeLower: Int = 100
  final val PriceLookupTimeUpper: Int = 500
}
