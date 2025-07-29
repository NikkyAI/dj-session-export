import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import okio.FileSystem
import okio.Path.Companion.toPath

val dateFormat = LocalDateTime.Format {
    year()
    char('/')
    monthNumber(padding = Padding.NONE)
    char('/')
    day(padding = Padding.NONE)
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun parseInstant(startTimeString: String): Instant {
    return LocalDateTime.parse(
        startTimeString,
        dateFormat
    ).toInstant(TimeZone.currentSystemDefault())
}

fun main(vararg args: String) {
    val filePath = args.getOrNull(0)
        ?: "HISTORY.html".takeIf {
            FileSystem.SYSTEM.exists(it.toPath())
        }
        ?: run {
            println("Enter the path to the HTML file: ")
            print("> ")
            readlnOrNull()?.trim() ?: return
        }
    println()
    println("parsing $filePath")

    val tracklist = parseHtmlFile(filePath.toPath())

    if (tracklist != null) {
//        createTracklist(parsedHtml)
        Template.write(
            tracklist.splitTracklists(
                { it.time },
                { track, diff ->
                    track.copy(
                        time = track.time - diff
                    )
                },
            ) { lastTrack, nextTrack -> nextTrack.time - (lastTrack.time + lastTrack.duration) },
            TrackData.serializer()
        )
        genreBreakdown(tracklist) { genre }
    }

    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

