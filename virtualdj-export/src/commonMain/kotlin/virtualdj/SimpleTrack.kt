package virtualdj

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

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