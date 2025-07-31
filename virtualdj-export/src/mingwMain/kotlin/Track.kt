import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

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

@Serializable
data class M3UTrack(
    val position: Int,
    @Serializable(with=DurationSerializer::class)
    val time: Duration,
    @Serializable(with=InstantSerializer::class)
    val startAt: Instant,
    val title: String?,
    val artist: String?,
    val remix: String?,
    val file: String,
)
@Serializable
data class SimpleTrack(
    val position: Int,
    @Serializable(with=DurationSerializer::class)
    val time: Duration,
    @Serializable(with=InstantSerializer::class)
    val startAt: Instant,
    val artist: String?,
    val title: String,
)