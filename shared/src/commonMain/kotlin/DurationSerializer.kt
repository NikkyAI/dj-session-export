import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.optional
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(BetaInteropApi::class)
val durationTimestampFormat = LocalTime.Format {
//    optional {
    hour(Padding.ZERO)
    char(':')
//    }
    minute(Padding.ZERO)
    char(':')
    second(Padding.ZERO)
}

object DurationSerializer : KSerializer<Duration> {

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Duration", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Duration) {
        val localTime = value.toComponents() { days, hours, minutes, seconds, _ ->
            if (days > 0) {
                listOf(
                    days,
                    hours,
                    minutes,
                    seconds
                )
            } else {
                listOf(
                    hours,
                    minutes,
                    seconds
                )
            }.joinToString(":") {
                it.toString().padStart(2, '0')
            }
//            "$days:${hours.toString().padStart(2, '0')}:"
        }
        encoder.encodeString(localTime)
    }

    override fun deserialize(decoder: Decoder): Duration {
        val durationString = decoder.decodeString()
        val components = durationString
            .split(":")
            .map { it.toInt() }
            .reversed()
        return components[0].seconds +
                components[1].minutes +
                components[2].hours +
                (components.getOrNull(3)?.days ?: Duration.ZERO)
//        val localTime = LocalTime.parse(, durationTimestampFormat)
//        return localTime.toMillisecondOfDay().milliseconds
    }
}