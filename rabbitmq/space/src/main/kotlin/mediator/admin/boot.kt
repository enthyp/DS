package mediator.admin

import mediator.QueueInitializer

const val NOTICE_EXCHANGE = "notice"

const val ALL_KEY = "all"
const val CLIENT_KEY = "clients"
const val CONTRACTOR_KEY = "contractors"
const val LOG_KEY = "#"

val adminBoot: QueueInitializer = { channel ->
    channel.exchangeDeclare(NOTICE_EXCHANGE, "direct", false, true, null)
    listOf()
}
