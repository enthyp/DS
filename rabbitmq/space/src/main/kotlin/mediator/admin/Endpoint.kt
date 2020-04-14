package mediator.admin

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import mediator.core.BaseEndpoint
import mediator.core.EndpointConnection
import mediator.core.Message
import mediator.core.Notice


enum class NoticeTarget {
    CLIENTS,
    CONTRACTORS,
    ALL
}

class AdminEndpoint(connection: EndpointConnection) : BaseEndpoint(connection) {

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
