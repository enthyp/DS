package mpk

import com.google.protobuf.Empty
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.supervisorScope
import java.io.Closeable
import java.lang.Exception
import java.util.concurrent.TimeUnit

class MpkClient constructor(
    private val clientId: Int,
    channelBuilder: ManagedChannelBuilder<*>
) : Closeable {

    private val channel = channelBuilder.build()
    private val stub = MpkPublisherGrpcKt.MpkPublisherCoroutineStub(channel)

    suspend fun getSchedule() = supervisorScope {
        val empty = Empty.newBuilder().build()
        val response = async { stub.getSchedule(empty) }
        try {
            displayMsg("SCHEDULE: ${response.await()}")
        } catch (e: StatusException) {
            displayError(e.message)
        }
    }

    suspend fun subscribe(subscription: Subscription) = supervisorScope {
        val requestBuilder = Mpk.NotifyRequest.newBuilder()
            .setClientId(clientId)
            .setStopId(subscription.stopId)
            .setMinutesAhead(5)  // has no effect anyways
            .addAllLines(subscription.lines)

        // Try to receive exactly countDown observations (normally would be a time interval)
        var countDown = subscription.duration
        val backoffPolicy = ExponentialBackoff()

        while (countDown > 0) {
            try {
                requestBuilder.duration = countDown
                val request = requestBuilder.build()

                stub.subscribe(request).collect { response ->
                    displayMsg("OBSERVED: $response")
                    countDown -= 1
                }

                displayMsg("STREAM ENDED")
            } catch (e: StatusException) {
                displayError(e.message)

                if (e.status.code == Status.INVALID_ARGUMENT.code) {
                    break
                } else {
                    try {
                        backoffPolicy.delay()
                        backoffPolicy.advance()
                        displayMsg("Retrying...")
                    } catch (e: ExponentialBackoff.Timeout) {
                        displayError("server failed to deliver.")
                        break
                    }
                }
            }
        }
    }

    private fun displayMsg(msg: String) = println(msg)

    private fun displayError(msg: String?) = println("Connection error: $msg")

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

// Exponential backoff state wrapper.
class ExponentialBackoff {
    private val factor = 1.1  // simple example so don't wait for eternity
    private var backoff = 1
    private var backoffInterval: Long = 1000

    class Timeout : Exception()

    fun advance() {
        if (backoff < 15) {
            backoff += 1

            if (backoff < 10) {
                backoffInterval = (backoffInterval * factor).toLong()
            }
        } else {
            throw Timeout()
        }
    }

    suspend fun delay() = kotlinx.coroutines.delay(backoffInterval)
}
