import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun <E : Any> Tracklist<E>.splitTracklists(
    timestampSelector: (E) -> Duration,
    subtractTime: (E, Duration) -> E,
    minDuration: Duration = 25.minutes,
//    generateTitle: (Tracklist<E>, List<E>, Int) -> String = { original, newTracks, playlistCount -> original.title + "_$playlistCount" },
    calculateGap: (E, E) -> Duration = { a, b -> timestampSelector(b) - timestampSelector(a) },
): List<Tracklist<E>> {
    if(this.tracks.isEmpty()) return emptyList()

    val tracklists = mutableListOf(this)

//    var subtractTime = Duration.ZERO
    var last: Duration = Duration.ZERO
    tracks.forEachIndexed { i, track ->
        val start = timestampSelector(track) // - subtractTime
        if (start - last > 15.minutes) {
            if (tracklists.size == 1) {
                val newTracks = (
                        listOf(tracks.first()) + tracks.zipWithNext()
                            .takeWhile { (track, nextTrack) ->
//                                val trackStart = timestampSelector(track)
//                                val nextStart = timestampSelector(nextTrack)

                                calculateGap(track, nextTrack) <= minDuration
                            }.map { it.second })
//                    .map {
//                        subtractTime(it, start)
//                    }

                tracklists.add(
                    Tracklist(
                        title = title + "_${tracklists.size}",
                        exportPath = exportPath,
                        tracks = newTracks,
                    )
                )
            }

            val newTracks = (listOf(track) + tracks.drop(i).zipWithNext()
                .takeWhile { (track, nextTrack) ->
//                    val trackStart = timestampSelector(track)
//                    val nextStart = timestampSelector(nextTrack)

                    calculateGap(track, nextTrack) <= minDuration
                }.map { it.second })
                .map {
                    subtractTime(it, start)
                }
//                .also {
//                    println()
//                    it.forEach {
//                        println(it)
//                    }
//                    println()
//                }

            tracklists.add(
                Tracklist(
                    title = title + "_${tracklists.size}",
                    exportPath = exportPath,
                    tracks = newTracks
                )
            )
        }
        last = start
    }

    return tracklists
}
