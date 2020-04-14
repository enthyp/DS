import mediator.admin.AdminModeConnectionFactory
import mediator.core.*
import java.util.*

class Agency(private val name: String, private val host: String) {

    private val connectionFactory: EndpointConnectionFactory = AdminModeConnectionFactory()
        .setHost(host)
        .setClientMode()
        .setOnRecvCallback { msg -> onMessage(msg) }

    fun run() {
        val connection = connectionFactory.newInstance()

        ClientEndpoint(connection).use { endpoint ->
            val inputLines = generateSequence { readLine() }
            println("Running...")

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
            is Notice -> println("Administration notice: $msg")
            else -> println("Message type not handled: $msg")
        }
    }

    private fun onConfirmation(confirmation: Confirmation) {
        println("${confirmation.from}: $confirmation")
    }
}

fun main(argv: Array<String>) {
    val agency = Agency(argv[0], argv[1])
    agency.run()
}
