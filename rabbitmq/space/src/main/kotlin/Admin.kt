import mediator.admin.NoticeTarget
import mediator.admin.ReceiverEndpointBuilder
import mediator.core.Message
import mediator.core.Notice

class AdminConsole(private val host: String) {
    fun run() {
        val endpointBuilder = ReceiverEndpointBuilder()
            .setHost(host)
            .setOnReceived { msg -> onLogReceived(msg) }

        val adminEndpoint = endpointBuilder.newAdminInstance()

        adminEndpoint.use { endpoint ->
            val inputLines = generateSequence { readLine() }
            println("Running...")

            inputLines.forEach { line ->
                val parts = line.split(":")
                if (parts.size != 2) {
                    onInputError()
                } else {
                    val msg = Notice(parts[1])
                    val target = when(parts[0]) {
                        "clients" -> NoticeTarget.CLIENTS
                        "contractors" -> NoticeTarget.CONTRACTORS
                        "all" -> NoticeTarget.ALL
                        else -> { onInputError(); null }
                    }

                    target?.let { endpoint.notify(msg, target) }
                }
            }
        }
    }

    private fun onInputError() {
        println("What are you doing?")
    }

    private fun onLogReceived(msg: Message) {
        println("Log: $msg")
    }
}

fun main(argv: Array<String>) {
    val adminConsole = AdminConsole(argv[0])
    adminConsole.run()
}
