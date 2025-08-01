package virtualdj

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

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