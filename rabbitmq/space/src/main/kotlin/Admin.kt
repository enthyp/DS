import mediator.admin.AdminEndpoint
import mediator.admin.AdminModeConnectionFactory
import mediator.admin.NoticeTarget
import mediator.core.Commission
import mediator.core.Message
import mediator.core.Notice

class AdminConsole(private val host: String) {
    private val connectionFactory = AdminModeConnectionFactory()
        .setHost(host)
        .setAdminMode()
        .setOnRecvCallback { msg -> onLogReceived(msg) }

    fun run() {
        val connection = connectionFactory.newInstance()

        AdminEndpoint(connection).use { endpoint ->
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
