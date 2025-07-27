import kotlin.time.Instant

data class Session(
    val date: String,
    val songs: List<HistorySong>
) {
    data class HistorySong(
        val timePlayed: Instant, // Date
        val title: String,
        val artist: String,
        val filePath: String,
        val bpm: Int? = null,
    )
}