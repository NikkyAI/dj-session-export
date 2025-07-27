import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class HistoryTrack(
    @Serializable(with = DurationSerializer::class)
    val time: Duration,
    @Serializable(with = InstantSerializer::class)
    val timePlayed: Instant, // Date
    val title: String,
    val artist: String,
    val filePath: String,
    val bpm: Int? = null,
)
