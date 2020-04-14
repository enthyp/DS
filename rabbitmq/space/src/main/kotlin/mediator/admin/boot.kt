package mediator.admin

import com.rabbitmq.client.Channel
import mediator.MAIN_EXCHANGE
import mediator.core.EndpointConnection

const val NOTICE_EXCHANGE = "notice"

const val ALL_KEY = "all"

const val CLIENT_NOTICE_QUEUE = "clients"
const val CLIENT_KEY = "clients"

const val CONTRACTOR_NOTICE_QUEUE = "contractors"
const val CONTRACTOR_KEY = "contractors"

const val LOG_QUEUE = "log"
const val LOG_KEY = "#"

fun adminInit(conn: EndpointConnection): List<String> {
    val channel: Channel = conn.createChannel()
    channel.exchangeDeclare(NOTICE_EXCHANGE, "direct", false, true, null)

    // One queue for client notifications.
    channel.queueDeclare(CLIENT_NOTICE_QUEUE, false, false, false, null)
    channel.queueBind(CLIENT_NOTICE_QUEUE, NOTICE_EXCHANGE, CLIENT_KEY)
    channel.queueBind(CLIENT_NOTICE_QUEUE, NOTICE_EXCHANGE, ALL_KEY)

    // And another for contractor notifications.
    channel.queueDeclare(CONTRACTOR_NOTICE_QUEUE, false, false, false, null)
    channel.queueBind(CONTRACTOR_NOTICE_QUEUE, NOTICE_EXCHANGE, CONTRACTOR_KEY)
    channel.queueBind(CONTRACTOR_NOTICE_QUEUE, NOTICE_EXCHANGE, ALL_KEY)

    // And a queue to log every message from the main exchange.
    channel.queueDeclare(LOG_QUEUE, false, false, false, null)
    channel.queueBind(LOG_QUEUE, MAIN_EXCHANGE, LOG_KEY)

    return listOf(LOG_QUEUE, CONTRACTOR_NOTICE_QUEUE, CLIENT_NOTICE_QUEUE)
}

