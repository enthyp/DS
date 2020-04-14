package mediator.admin

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import mediator.core.BaseEndpointConnection
import mediator.core.EndpointConnection
import mediator.core.EndpointConnectionFactory
import mediator.core.Message


open class ReceivingEndpointConnection(
    host: String,
    recvQueue: String,
    recvCallback: (Message) -> Unit
) :
    BaseEndpointConnection(host) {

    private val recvChannel: Channel = connection.createChannel()

    init {
        val deliverCallback = DeliverCallback { _, message ->
            val msg = Message.decode(message.body)
            recvCallback(msg)
            recvChannel.basicAck(message.envelope.deliveryTag, false)
        }

        recvChannel.basicConsume(recvQueue, false, deliverCallback, CancelCallback {})
    }
}

class AdminModeConnectionFactory : EndpointConnectionFactory {

    enum class ConnectionType {
        CLIENT,
        CONTRACTOR,
        ADMIN
    }

    private lateinit var host: String
    private var type: ConnectionType =
        ConnectionType.CLIENT
    private var onRecvCallback: (Message) -> Unit = { _ -> }

    override fun setHost(host: String): AdminModeConnectionFactory {
        this.host = host
        return this
    }

    override fun setClientMode(): AdminModeConnectionFactory {
        this.type = ConnectionType.CLIENT
        return this
    }

    override fun setContractorMode(): AdminModeConnectionFactory {
        this.type = ConnectionType.CONTRACTOR
        return this
    }

    fun setAdminMode(): AdminModeConnectionFactory {
        this.type = ConnectionType.ADMIN
        return this
    }

    fun setOnRecvCallback(callback: (Message) -> Unit): AdminModeConnectionFactory {
        this.onRecvCallback = callback
        return this
    }

    override fun newInstance(): EndpointConnection {
        return when (type) {
            ConnectionType.CLIENT -> ReceivingEndpointConnection(host, CLIENT_NOTICE_QUEUE, onRecvCallback)
            ConnectionType.CONTRACTOR -> ReceivingEndpointConnection(host, CONTRACTOR_NOTICE_QUEUE, onRecvCallback)
            ConnectionType.ADMIN -> ReceivingEndpointConnection(host, LOG_QUEUE, onRecvCallback)
        }
    }
}
