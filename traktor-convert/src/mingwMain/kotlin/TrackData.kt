import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class TrackData(
    val position: Int,
    @Serializable(with = DurationSerializer::class)
    val time: Duration,
    @Serializable(with = InstantSerializer::class)
    val playedAt: Instant,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration,
    val title: String,
    val artist: String,
    val genre: String?,
    val deck: String?,
    val key: String?,
) {
    @Serializable(with = InstantSerializer::class)
    val endTime = playedAt + duration
}