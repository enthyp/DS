package mediator.core

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.lang.IndexOutOfBoundsException


@Serializer(forClass = ServiceType::class)
object ServiceTypeSerializer {
    override val descriptor: SerialDescriptor
        get() = StringDescriptor

    override fun deserialize(decoder: Decoder): ServiceType {
        return ServiceType.valueOf(decoder.decodeString().toUpperCase())
    }

    override fun serialize(encoder: Encoder, obj: ServiceType) {
        encoder.encodeString(obj.name.toLowerCase())
    }
}


@Serializable(ServiceTypeSerializer::class)
enum class ServiceType {
    PASSENGER_TRANSPORT,
    CARGO_TRANSPORT,
    SATELLITE_LAUNCH
}

open class Message {
    companion object {
        private val json = Json(JsonConfiguration.Stable)

        fun encode(msg: Commission): ByteArray {
            var bytes = ByteArray(0)
            bytes = bytes.plus(0.toByte())
            val jsonStr = json.stringify(Commission.serializer(), msg)
            return bytes.plus(jsonStr.toByteArray(charset("UTF-8")))
        }

        fun encode(msg: Confirmation): ByteArray {
            var bytes = ByteArray(0)
            bytes = bytes.plus(1.toByte())
            val jsonStr = json.stringify(Confirmation.serializer(), msg)
            return bytes.plus(jsonStr.toByteArray(charset("UTF-8")))
        }

        fun encode(msg: Notice): ByteArray {
            var bytes = ByteArray(0)
            bytes = bytes.plus(2.toByte())
            val jsonStr = json.stringify(Notice.serializer(), msg)
            return bytes.plus(jsonStr.toByteArray(charset("UTF-8")))
        }

        fun decode(bytes: ByteArray): Message {
            val code = bytes[0]
            val body = bytes.sliceArray(1 until bytes.size).toString(charset("UTF-8"))

            return when (code) {
                0.toByte() -> json.fromJson(
                    Commission.serializer(), json.parseJson(body))
                1.toByte() -> json.fromJson(
                    Confirmation.serializer(), json.parseJson(body))
                2.toByte() -> json.fromJson(
                    Notice.serializer(), json.parseJson(body))
                else -> throw IndexOutOfBoundsException()
            }
        }
    }
}

@Serializable
data class Commission(val from: String, val commissionId: String, val type: ServiceType) : Message() {
    override fun toString(): String {
        return "$commissionId : $type"
    }
}


@Serializable
data class Confirmation(val from: String, val commissionId: String) : Message() {
    override fun toString(): String {
        return "$commissionId : CONFIRMED"
    }
}

@Serializable
data class Notice(val body: String) : Message() {
    override fun toString(): String {
        return body
    }
}
