import mediator.*
import java.util.*

class Agency(private val name: String, private val host: String) {

    fun run() {
        ClientEndpoint(host).use { endpoint ->
            val inputLines = generateSequence { readLine() }

            inputLines.forEach { line ->
                val serviceType: ServiceType? = when (line) {
                    "cargo" -> ServiceType.CARGO_TRANSPORT
                    "passengers" -> ServiceType.PASSENGER_TRANSPORT
                    "satellite" -> ServiceType.SATELLITE_LAUNCH
                    else -> null
                }

                if (serviceType != null) {
                    val requestId = UUID.randomUUID().toString()
                    val commission = Commission(name, requestId, serviceType)
                    endpoint.post(commission) { msg -> onMessage(msg) }
                } else {
                    println("Unknown service type: $line")
                }
            }
        }
    }

    private fun onMessage(msg: Message) {
        when(msg) {
            is Confirmation -> onConfirmation(msg)
            is Notice -> println("Administration notice: ${msg.body}")
            else -> println("Message type not handled: $msg")
        }
    }

    private fun onConfirmation(confirmation: Confirmation) {
        println("Task executed by ${confirmation.from}: $confirmation")
    }
}

fun main(argv: Array<String>) {
    val agency = Agency(argv[0], argv[1])
    agency.run()
}
