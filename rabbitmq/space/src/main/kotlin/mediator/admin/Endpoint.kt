package mediator.admin

import com.rabbitmq.client.*
import mediator.MAIN_EXCHANGE
import mediator.core.*

typealias ReceiverCallback = (Message) -> Unit

class Receiver(
    private val recvChannel: Channel,
    recvExchange: String,
    recvKeys: List<String>,
    onReceived: ReceiverCallback
) {

    init {
        // Create private queue for incoming messages.
        val recvQueue = recvChannel.queueDeclare().queue
        for (key: String in recvKeys) {
            recvChannel.queueBind(recvQueue, recvExchange, key)
        }

        val deliverCallback = DeliverCallback { _, message ->
            val msg = Message.decode(message.body)
            onReceived(msg)
            recvChannel.basicAck(message.envelope.deliveryTag, false)
        }
        recvChannel.basicConsume(recvQueue, false, deliverCallback, CancelCallback {})
    }
}

class ReceiverClientEndpoint(host: String, private val receiver: Receiver) : ClientEndpoint(host)
class ReceiverContractorEndpoint(host: String, private val receiver: Receiver) : ContractorEndpoint(host)

enum class NoticeTarget {
    CLIENTS,
    CONTRACTORS,
    ALL
}

class AdminEndpoint(host: String, private val receiver: Receiver) : BaseEndpoint(host) {

    private val channel: Channel = connection.createChannel()

    fun notify(msg: Notice, who: NoticeTarget) {
        val props = AMQP.BasicProperties.Builder().build()
        val key = when (who) {
            NoticeTarget.CLIENTS -> CLIENT_KEY
            NoticeTarget.CONTRACTORS -> CONTRACTOR_KEY
            NoticeTarget.ALL -> ALL_KEY
        }

        channel.basicPublish(NOTICE_EXCHANGE, key, props, Message.encode(msg))
    }
}

class ReceiverEndpointBuilder : SimpleEndpointBuilder() {

    private var onReceivedCallback: ReceiverCallback = { _ -> }

    override fun setHost(host: String): ReceiverEndpointBuilder {
        super.setHost(host)
        return this
    }

    fun setOnReceived(callback: ReceiverCallback): ReceiverEndpointBuilder {
        this.onReceivedCallback = callback
        return this
    }

    override fun newClientInstance(): ReceiverClientEndpoint {
        val connection = connFactory.newConnection()
        val channel = connection.createChannel()
        val receiver = Receiver(channel, NOTICE_EXCHANGE, listOf(CLIENT_KEY, ALL_KEY), onReceivedCallback)

        return ReceiverClientEndpoint(host, receiver)
    }

    override fun newContractorInstance(): ReceiverContractorEndpoint {
        val connection = connFactory.newConnection()
        val channel = connection.createChannel()
        val receiver = Receiver(channel, NOTICE_EXCHANGE, listOf(CONTRACTOR_KEY, ALL_KEY), onReceivedCallback)

        return ReceiverContractorEndpoint(host, receiver)
    }

    fun newAdminInstance(): AdminEndpoint {
        val connection = connFactory.newConnection()
        val channel = connection.createChannel()
        val receiver = Receiver(channel, MAIN_EXCHANGE, listOf(LOG_KEY), onReceivedCallback)

        return AdminEndpoint(host, receiver)
    }
}
