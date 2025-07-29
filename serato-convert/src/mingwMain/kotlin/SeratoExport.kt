import app.softwork.serialization.flf.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SeratoExport(
    @FixedLength(84)
    val name: String,
    @FixedLength(25)
    @SerialName("start time")
    val startTime: String,
    @FixedLength(25)
    @SerialName("end time")
    val endTime: String,
    @FixedLength(13)
    @Serializable(with=DurationSerializer::class)
    val playtime: Duration,
    @FixedLength(9)
    val deck: String,
    val notes: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SeratoExportFLF(
    @FixedLength(84)
    val name: String,
    @FixedLength(25)
    @SerialName("start time")
    val startTime: String,
    @FixedLength(25)
    @SerialName("end time")
    val endTime: String,
    @FixedLength(13)
    @Serializable(with=DurationSerializer::class)
    val playtime: Duration,
    @FixedLength(9)
    val deck: String,
)

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
