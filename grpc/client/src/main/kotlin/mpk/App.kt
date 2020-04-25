package mpk

import com.google.protobuf.Empty
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.Closeable
import java.io.IOException
import java.lang.NumberFormatException
import java.text.ParseException
import java.util.concurrent.Executors
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
            println("SCHEDULE: ${response.await()}")
        } catch (e: StatusException) {
            onCommunicationError(e.message)
        }
    }

    suspend fun subscribe(stopId: Int, lines: List<Mpk.Line>) = supervisorScope {
        val request = Mpk.NotifyRequest.newBuilder()
            .setClientId(clientId)
            .setStopId(stopId)
            .setMinutesAhead(5)  // has no effect anyways
            .addAllLines(lines)
            .build()

        try {
            stub.subscribe(request).collect { response ->
                println("OBSERVED: $response")
            }

            println("STREAM ENDED")
        } catch (e: StatusException) {
            onCommunicationError(e.message)
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}

fun onCommunicationError(msg: String?) = println("Connection error: $msg")


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
    while (true) {
        val line = readLine()
        when (CmdState.of(line)) {
            CmdState.INIT -> {}
            CmdState.SCHEDULE -> client.getSchedule()
            CmdState.LINES -> {
                try {
                    val (stopId, lines) = parseLines()
                    client.subscribe(stopId, lines)
                } catch (e: IOException) {
                    onInputError("")
                } catch (e: ParseException) {
                    e.message?.let { msg -> onInputError(msg) }
                } catch (e: NumberFormatException) {
                    onInputError("Integers required.")
                }
            }
            CmdState.EXIT -> return
            else -> onInputError("")
        }
    }
}

fun onInputError(msg: String) = println("Incorrect input! $msg")

enum class CmdState {
    INIT,
    SCHEDULE,
    LINES,
    EXIT,
    ERR;

    companion object {
        @JvmStatic
        fun of(line: String?): CmdState {
            return when (line) {
                null -> INIT
                "get schedule" -> SCHEDULE
                "subscribe" -> LINES
                "exit" -> EXIT
                else -> ERR
            }
        }
    }
}

data class Subscription(val stopId: Int, val lines: List<Mpk.Line>)

fun parseLines(): Subscription {
    print("Stop ID: ")
    val stopId = readLine()?.trim()?.toInt()

    print("Lines: ")
    val lineStr = readLine()?.trim()?.split(",")?.map { p -> p.trim() }
    val lines = lineStr?.mapNotNull { line ->
        val parts = line.split(" ").map { p -> p.trim() }
        if (parts.size == 2) {
            val number = parts[0].toInt()
            val direction = parts[1].toBoolean()
            Mpk.Line.newBuilder()
                .setDirection(direction)
                .setNumber(number)
                .build()
        } else {
            throw ParseException("Incorrect line description: $line", 0)
        }
    }

    if (stopId != null && !lines.isNullOrEmpty()) {
        return Subscription(stopId, lines)
    } else {
        throw ParseException("", 0)
    }
}
