package mediator

import com.rabbitmq.client.Channel

// Declares a list of non-auto-delete queues that have to be cleaned up.
typealias QueueInitializer = (Channel) -> List<String>

class QueueHandler(private val channel: Channel) : AutoCloseable {

    private val registeredQueues = mutableListOf<String>()

    fun register(initializer: QueueInitializer) {
        val declaredQueues = initializer(channel)
        registeredQueues += declaredQueues
    }

    override fun close() {
        registeredQueues.forEach { q -> channel.queueDelete(q) }
    }
}

// Kotlin magic works.
// See https://kotlinlang.org/docs/reference/lambdas.html # Function literals with receiver
fun withQueues(channel: Channel, block: QueueHandler.() -> Unit) = QueueHandler(channel).use(block)
