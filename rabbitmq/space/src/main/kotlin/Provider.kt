import mediator.admin.ReceiverEndpointBuilder
import mediator.core.*

class Provider(
    private val name: String,
    private val host: String,
    private val s1: ServiceType,
    private val s2: ServiceType
) {
    fun run() {
        val endpointBuilder = ReceiverEndpointBuilder()
            .setHost(host)
            .setOnReceived { msg -> onMessage(msg) }

        val contractorEndpoint = endpointBuilder.newContractorInstance()

        contractorEndpoint.use { endpoint ->
            println("Running...")
            endpoint.register(s1, s2) { msg -> onMessage(msg) }
        }
    }

    private fun onMessage(msg: Message) : Confirmation? {
        return when (msg) {
            is Commission -> onCommission(msg)
            is Notice -> { println("Administration: $msg"); return null }
            else -> { println("Message not handled: $msg"); return null }
        }
    }

    private fun onCommission(commission: Commission): Confirmation {
        println(commission)
        return Confirmation(name, commission.commissionId)
    }
}

fun main(argv: Array<String>) {
    val s1 = ServiceType.valueOf(argv[2])
    val s2 = ServiceType.valueOf(argv[3])

    val provider = Provider(argv[0], argv[1], s1, s2)
    provider.run()
}
