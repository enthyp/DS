package mediator.core

import com.rabbitmq.client.*
import mediator.MAIN_EXCHANGE
import java.io.Closeable
import java.util.*

open class BaseEndpoint(host: String) : Closeable {

    protected val connection: Connection

    init {
        val connFactory = ConnectionFactory()
        connFactory.host = host
        connection = connFactory.newConnection()
    }

    override fun close() {
        connection.close()
    }
}

open class ClientEndpoint(host: String) : BaseEndpoint(host) {

    private val channel: Channel = connection.createChannel()
    private val callbacks: MutableMap<String, (Message) -> Unit> = mutableMapOf()
    private val replyQueue: String = UUID.randomUUID().toString()

    init {
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

            channel.basicAck(message.envelope.deliveryTag, false)
        }

        channel.queueDeclare(replyQueue, false, false, true, null)
        channel.queueBind(replyQueue, MAIN_EXCHANGE, replyQueue)
        channel.basicConsume(replyQueue, false, deliverCallback, CancelCallback {})
    }

    fun post(commission: Commission, callback: (Message) -> Unit) {
        val corrId = UUID.randomUUID().toString()

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(replyQueue)
            .build()

        val key = commission.type.toString()
        synchronized(callbacks) {
            channel.basicPublish(MAIN_EXCHANGE, key, props, Message.encode(commission))
            callbacks[corrId] = callback
        }
    }
}

open class ContractorEndpoint(host: String) : BaseEndpoint(host) {

    private val channels = List(2) { _ -> connection.createChannel() }

    init {
        for (channel in channels) {
            channel.basicQos(1)
        }
    }

    fun register(service1: ServiceType, service2: ServiceType, callback: (Message) -> Confirmation?) {
        assert(service1 != service2)
        val services = listOf(service1, service2)

        for (i in 0 until 2) {
            val service = services[i]
            val channel = channels[i]

            val deliverCallback = DeliverCallback { _, message ->
                val msg = Message.decode(message.body)
                val response = callback(msg)

                val replyProps = AMQP.BasicProperties.Builder()
                    .correlationId(message.properties.correlationId)
                    .build()

                if (response != null) {
                    channel.basicPublish(
                        MAIN_EXCHANGE, message.properties.replyTo, replyProps, Message.encode(response)
                    )
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

open class SimpleEndpointBuilder {

    protected lateinit var host: String
    protected val connFactory = ConnectionFactory()

    open fun setHost(host: String): SimpleEndpointBuilder {
        this.host = host
        this.connFactory.host = host
        return this
    }

    open fun newClientInstance(): ClientEndpoint {
        return ClientEndpoint(host)
    }

    open fun newContractorInstance(): ContractorEndpoint {
        return ContractorEndpoint(host)
    }
}
