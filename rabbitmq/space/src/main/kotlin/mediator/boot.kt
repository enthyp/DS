package mediator

import com.rabbitmq.client.ConnectionFactory
import mediator.admin.adminBoot
import mediator.core.ServiceType

const val MAIN_EXCHANGE = "main"

val coreBoot: QueueInitializer = { channel ->
    channel.exchangeDeclare(MAIN_EXCHANGE, "topic", false, true, null)

    val declaredQueues = mutableListOf<String>()
    for (service in ServiceType.values()) {
        val queueName = service.toString()
        channel.queueDeclare(queueName, false, false, false, null)
        channel.queueBind(queueName, MAIN_EXCHANGE, service.toString())
        declaredQueues += queueName
    }

    declaredQueues
}

// This function must be running for the whole time of system operation.
fun main(argv: Array<String>) {
    val connectionFactory = ConnectionFactory()
    connectionFactory.host = argv[0]
    val connection = connectionFactory.newConnection()

    connection.use { conn ->
        val channel = conn.createChannel()

        withQueues(channel) {
            // Module initialization.
            register(coreBoot)
            register(adminBoot)

            println("SYSTEM INITIALIZED\nPress any key to shut it down...")
            readLine()
            println("SYSTEM SHUTDOWN")
        }
    }
}
