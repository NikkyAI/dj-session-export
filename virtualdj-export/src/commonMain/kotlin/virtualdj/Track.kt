package virtualdj

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class Track(
    val position: Int,
    @Serializable(with=DurationSerializer::class)
    val time: Duration,
//    @Serializable(with=InstantSerializer::class)
//    val timestamp: Instant,
//    @Serializable(with=DurationSerializer::class)
//    val duration: Duration?,
    val artist: String,
    val title: String,
//    val album: String?,
//    val year: String?,
//    val genre: String?,
//    val bpm: Float?,
//    val key: String?,
)

