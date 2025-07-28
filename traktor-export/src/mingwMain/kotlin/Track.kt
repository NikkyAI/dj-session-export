import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
class Track(
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
    val file: String,
) {
}