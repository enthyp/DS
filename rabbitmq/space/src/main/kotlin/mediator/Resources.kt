package mediator

class Resources : AutoCloseable {
    private val resources = mutableListOf<AutoCloseable>()

    fun <T: AutoCloseable> T.use(): T {
        resources += this
        return this
    }

    override fun close() {
        var exception: Exception? = null
        for (resource in resources.reversed()) {
            try {
                resource.close()
            } catch (closeException: Exception) {
                if (exception == null) {
                    exception = closeException
                } else {
                    exception.addSuppressed(closeException)
                }
            }
        }
        if (exception != null) throw exception
    }
}

// Uses function literal with a receiver as parameter!
inline fun <T> withResources(block: Resources.() -> T): T = Resources().use(block)
