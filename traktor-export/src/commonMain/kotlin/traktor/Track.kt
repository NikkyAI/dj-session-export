package traktor

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class Track(
    @Serializable(with = InstantSerializer::class)
    val startAt: Instant,
    @Serializable(with = DurationSerializer::class)
    val time: Duration,
    @Serializable(with = DurationSerializer::class)
    val playDuration: Duration,
    @Serializable(with = DurationSerializer::class)
    val endTime: Duration,
    val title: String,
    val artist: String?,
    val album: String?,
    val label: String?,
    val remixer: String?,
    val key: String?,
    val genre: String?,
    val file: String?,
) {
    @Serializable(with = InstantSerializer::class)
    val endAt = startAt + playDuration
}