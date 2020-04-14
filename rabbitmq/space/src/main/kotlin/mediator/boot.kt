package mediator

import mediator.admin.adminInit
import mediator.core.BaseConnectionFactory
import mediator.core.EndpointConnection
import mediator.core.ServiceType

const val MAIN_EXCHANGE = "main"
const val RPC_REPLY_KEY = "rpc"     // to bind reply queues to main exchange
const val QUEUE_TTL = 60000         // 60s

typealias PluginBootRunner = (EndpointConnection) -> List<String>

val plugins = listOf<PluginBootRunner>(
    ::adminInit
)

class Queues(private val conn: EndpointConnection) : AutoCloseable {

    private val queues = mutableListOf<String>()

    fun register(queue: String) {
        queues.add(queue)
    }

    override fun close() {
        val channel = conn.createChannel()
        queues.forEach { q -> channel.queueDelete(q); println("Delete $q") }
    }
}

// Kotlin magic works.
// See https://kotlinlang.org/docs/reference/lambdas.html # Function literals with receiver
fun withQueues(conn: EndpointConnection, block: Queues.() -> Unit) = Queues(conn).use(block)

fun main(argv: Array<String>) {
    val connectionFactory = BaseConnectionFactory().setHost(argv[0])
    val connection = connectionFactory.newInstance()

    connection.use { conn ->
        withQueues(conn) {
            val channel = connection.createChannel()
            channel.exchangeDeclare(MAIN_EXCHANGE, "topic", false, true, null)

            for (service in ServiceType.values()) {
                val queueName = service.toString()
                channel.queueDeclare(queueName, false, false, false, null)
                channel.queueBind(queueName, MAIN_EXCHANGE, service.toString())
                register(queueName)
            }

            // Initialize plugins.
            for (plugin: PluginBootRunner in plugins) {
                val pluginQueues = plugin(connection)
                pluginQueues.forEach { register(it) }
            }

            println("SYSTEM INITIALIZED")
            readLine()
            println("SYSTEM SHUTDOWN")
        }
    }
}
