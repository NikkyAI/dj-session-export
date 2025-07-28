import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.max

fun <E> genreBreakdown(
    tracklist: Tracklist<E>,
    getGenre: E.() -> String?
) {
    if(tracklist.tracks.all { it.getGenre() == null }) return

    val genreCount = tracklist.tracks.groupingBy { it.getGenre() }.eachCount()
    println("")
    println(tracklist.title)
    println("GENRES: ")
    genreCount.entries.sortedByDescending { it.value }
        .forEach {
            println("${it.value} x ${it.key}")
        }

    val keys = setOf("Genre", "Tracks")
    val values = genreCount.entries.map { (genre, count) ->
        mapOf("Genre" to (genre ?: "???"), "Tracks" to count.toString())
    }

    val widths = keys.associateWith { key ->
        max(
            key.length,
            values.maxOf { it[key]?.length ?: 0 }
        )
    }

    val md =
        keys.joinToString(" | ", "| ", " | \n") { it.padEnd(widths[it] ?: 0) } +
                keys.joinToString("-|-", "|-", "-| \n") { "-".repeat(widths[it] ?: 0) } +
                values.joinToString("\n") { obj ->
                    obj.entries.joinToString(" | ","| ", " |") { (key, value) ->
                        value.padEnd(widths[key] ?: 0)
                    }
                }

    val mdPath = "${tracklist.title}.genres.md".toPath()
    println("writing to $mdPath")
    FileSystem.SYSTEM.write(mdPath) {
        writeUtf8(md)
    }
}