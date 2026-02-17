package mixxx

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val position: Int,
    @Serializable(with=DurationSerializer::class)
    val time: Duration,
    @Serializable(with=InstantSerializer::class)
    val startAt: Instant,
//    @Serializable(with=DurationSerializer::class)
//    val duration: Duration?,
    val title: String,
    val artist: String?,
    val album: String?,
    val year: String?,
    val genre: String?,
    val bpm: Float?,
    val key: String?,
)