import okio.Path

data class Tracklist<T>(
    val title: String,
    val exportPath: Path,
    val tracks: List<T>,
)
