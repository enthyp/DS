package mediator

import com.rabbitmq.client.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.Closeable
import java.util.*

open class BaseEndpoint(host: String) : Closeable {

    protected val connection: Connection
    protected val json = Json(JsonConfiguration.Stable)

    init {
        val connFactory = ConnectionFactory()
        connFactory.host = host
        connection = connFactory.newConnection()
    }

    override fun close() {
        connection.close()
    }

    companion object {
        @JvmStatic
        protected val EXCHANGE_NAME = "main"
    }
}

class ClientEndpoint(host: String) : BaseEndpoint(host) {

    private val channel: Channel = connection.createChannel()
    private val callbacks: MutableMap<String, (Message) -> Unit> = mutableMapOf()

    init {
        channel.exchangeDeclare(EXCHANGE_NAME, "topic")

        // Prepare channel to receive confirmations.
        val deliverCallback = DeliverCallback { _, message ->
            val msgId = message.properties.correlationId

            synchronized(callbacks) {
                callbacks[msgId]?.let { callback ->
                    val msg = Message.decode(message.body)
                    callback(msg)
                    callbacks.remove(msgId)
                }
            }
        }

        channel.basicConsume("amq.rabbitmq.reply-to", true, deliverCallback, CancelCallback {})
    }

    fun post(commission: Commission, callback: (Message) -> Unit) {
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo("amq.rabbitmq.reply-to")
            .build()

        val key = commission.type.toString()
        synchronized(callbacks) {
            channel.basicPublish(EXCHANGE_NAME, key, props, Message.encode(commission))
            callbacks[corrId] = callback
        }
    }
}

class ContractorEndpoint(host: String) : BaseEndpoint(host) {

    private val channels = List(2) { _ -> connection.createChannel() }

    init {
        for (channel in channels) {
            channel.exchangeDeclare(EXCHANGE_NAME, "topic")
            channel.basicQos(1)
        }
    }

    fun register(service1: ServiceType, service2: ServiceType, callback: (Message) -> Confirmation?) {
        val services = listOf(service1, service2)

        for (i in 0 until 2) {
            val service = services[i]
            val channel = channels[i]

            channel.queueDeclare(service.toString(), false, false, false, null)
            channel.queueBind(service.toString(), EXCHANGE_NAME, service.toString())

            val deliverCallback = DeliverCallback { _, message ->
                val msg = Message.decode(message.body)
                val response = callback(msg)

                val replyProps = AMQP.BasicProperties.Builder()
                    .correlationId(message.properties.correlationId)
                    .build()

                if (response != null) {
                    channel.basicPublish("", message.properties.replyTo, replyProps, Message.encode(response))
                    channel.basicAck(message.envelope.deliveryTag, false)
                }
            }

            channel.basicConsume(service.toString(), false, deliverCallback, CancelCallback {})
        }

        while (true) {
            synchronized(this) {
                (this as Object).wait()
            }
        }
    }
}
