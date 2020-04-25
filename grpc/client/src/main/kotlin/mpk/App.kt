package mpk

import com.google.protobuf.Empty
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MpkClient constructor(
    private val clientId: Int,
    channelBuilder: ManagedChannelBuilder<*>
) : Closeable {

    private val channel = channelBuilder.build()
    private val stub = MpkPublisherGrpcKt.MpkPublisherCoroutineStub(channel)

    suspend fun getSchedule() = coroutineScope {
        val empty = Empty.newBuilder().build()
        val response = async { stub.getSchedule(empty) }
        println("SCHEDULE: ${response.await()}")
    }

    suspend fun subscribe(stopId: Int, lines: List<Mpk.Line>) = coroutineScope {
        val request = Mpk.NotifyRequest.newBuilder()
            .setClientId(clientId)
            .setStopId(stopId)
            .setMinutesAhead(5)  // has no effect anyways
            .addAllLines(lines)
            .build()

        stub.subscribe(request).collect{ response ->
            println("OBSERVED: $response")
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}


fun main(args: Array<String>) = runBlocking {
    val address = "localhost"
    val port = 50051
    val clientId = 1

    Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        val channelBuilder = ManagedChannelBuilder
            .forAddress(address, port)
            .usePlaintext()
            .executor(dispatcher.asExecutor())
        MpkClient(clientId, channelBuilder).use { client ->
            cmd(client)
        }
    }
}

suspend fun cmd(client: MpkClient) {
    val inputLines = generateSequence { readLine() }

    inputLines.forEach { line ->
        when(line) {
            "get schedule" -> client.getSchedule()
            else -> {
                val parts = line.split(":").map { p -> p.trim() }
                if (parts.size != 2) {
                    println("Incorrect input!")
                } else {
                    val stopId = parts[0].toInt()
                    val lines = parseLines(parts[1])
                    client.subscribe(stopId, lines)
                }
            }
        }
    }
}

fun parseLines(lines: String): List<Mpk.Line> {
    return lines.split(",").map { line ->
        val parts = line.trim().split(" ").map { p -> p.trim() }
        val number = parts[0].toInt()
        val direction = parts[1].trim().toBoolean()
        Mpk.Line.newBuilder()
            .setDirection(direction)
            .setNumber(number)
            .build()
    }
}
