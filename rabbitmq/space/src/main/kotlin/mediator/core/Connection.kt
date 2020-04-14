package mediator.core

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.Closeable

interface EndpointConnection : Closeable {
    fun createChannel(): Channel
}

open class BaseEndpointConnection(host: String) : EndpointConnection {

    protected val connection: Connection

    init {
        val connFactory = ConnectionFactory()
        connFactory.host = host
        connection = connFactory.newConnection()
    }

    override fun createChannel(): Channel = connection.createChannel()

    override fun close() {
        connection.close()
    }
}

interface EndpointConnectionFactory {
    fun setHost(host: String): EndpointConnectionFactory
    fun setClientMode(): EndpointConnectionFactory
    fun setContractorMode(): EndpointConnectionFactory
    fun newInstance(): EndpointConnection
}

class BaseConnectionFactory : EndpointConnectionFactory {

    private lateinit var host: String

    override fun setHost(host: String): BaseConnectionFactory {
        this.host = host
        return this
    }

    override fun setClientMode(): BaseConnectionFactory {
        return this
    }

    override fun setContractorMode(): BaseConnectionFactory {
        return this
    }

    override fun newInstance(): EndpointConnection {
        return BaseEndpointConnection(host)
    }
}
