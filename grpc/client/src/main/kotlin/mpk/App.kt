package mpk

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import java.io.IOException
import java.lang.NumberFormatException
import java.text.ParseException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) = runBlocking {
    val address = "192.168.100.106"
    val port = 50051
    val clientId = 1

    Executors.newFixedThreadPool(10).asCoroutineDispatcher().use { dispatcher ->
        val channelBuilder = ManagedChannelBuilder
            .forAddress(address, port)
            .keepAliveTime(100, TimeUnit.MILLISECONDS)  // extremely short just to show
            .keepAliveTimeout(100, TimeUnit.MILLISECONDS)
            .usePlaintext()
            .executor(dispatcher.asExecutor())
        MpkClient(clientId, channelBuilder).use { client ->
            cmd(client, dispatcher)
        }
    }
}

suspend fun cmd(client: MpkClient, dispatcher: CoroutineDispatcher) = coroutineScope {
    while (true) {
        val line = readLine()

        when (CmdState.of(line)) {
            CmdState.INIT -> {}
            CmdState.SCHEDULE -> launch(dispatcher) { client.getSchedule() }
            CmdState.LINES -> {
                try {
                    val subscription = parseLines()
                    launch (dispatcher) { client.subscribe(subscription) }
                } catch (e: IOException) {
                    onInputError("")
                } catch (e: ParseException) {
                    e.message?.let { msg -> onInputError(msg) }
                } catch (e: NumberFormatException) {
                    onInputError("Integers required.")
                }
            }
            CmdState.EXIT -> return@coroutineScope
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
            return when (line?.trim()) {
                null -> INIT
                "get schedule" -> SCHEDULE
                "subscribe" -> LINES
                "exit" -> EXIT
                else -> ERR
            }
        }
    }
}

data class Subscription(val stopId: Int, val duration: Int, val lines: List<Mpk.Line>)

fun parseLines(): Subscription {
    print("Stop ID: ")
    val stopId = readLine()?.trim()?.toInt()

    print("Duration: ")
    val duration = readLine()?.trim()?.toInt()

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

    if (stopId != null && duration != null && !lines.isNullOrEmpty()) {
        return Subscription(stopId, duration, lines)
    } else {
        throw ParseException("", 0)
    }
}
