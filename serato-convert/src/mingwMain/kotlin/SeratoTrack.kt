import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Serializable
data class SeratoTrack(
    @Serializable(with=DurationSerializer::class)
    val time: Duration,
    val title: String,
    @Serializable(with = InstantSerializer::class)
    val startAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val endAt: Instant,
    @Serializable(with = DurationSerializer::class)
    val playDuration: Duration,
    val deck: Int,
    val notes: String,
)