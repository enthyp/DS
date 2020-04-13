import mediator.*

class Provider(
    private val name: String,
    private val host: String,
    private val s1: ServiceType,
    private val s2: ServiceType
) {

    fun run() {
        ContractorEndpoint(host).use { endpoint ->
            endpoint.register(s1, s2) { msg -> onMessage(msg) }
        }
    }

    private fun onMessage(msg: Message) : Confirmation? {
        return when (msg) {
            is Commission -> onCommission(msg)
            is Notice -> { println("Administration notice: ${msg.body}"); return null }
            else -> { println("Message type not handled: $msg"); return null }
        }
    }

    private fun onCommission(commission: Commission): Confirmation {
        println("Got some work to do from ${commission.from}: $commission")
        return Confirmation(name, commission.commissionId)
    }
}

fun main(argv: Array<String>) {
    val s1 = ServiceType.valueOf(argv[2])
    val s2 = ServiceType.valueOf(argv[3])

    val provider = Provider(argv[0], argv[1], s1, s2)
    provider.run()
}
