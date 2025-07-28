import kotlinx.datetime.UtcOffset
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class Track(
//    @Serializable(with = InstantSerializer::class)
//    val playedAt: String,
    @Serializable(with = DurationSerializer::class)
    val time: Duration,
    @Serializable(with = DurationSerializer::class)
    val duration: Duration,
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
}