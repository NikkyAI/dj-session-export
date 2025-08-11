package traktor

import genreBreakdown
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import okio.FileSystem
import okio.Path.Companion.toPath
import splitTracklists

fun parseInstant(startTimeString: String): Instant {
    return LocalDateTime.parse(
        startTimeString,
        LocalDateTime.Format {
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
    ).toInstant(TimeZone.currentSystemDefault())
}

fun main(vararg args: String) {
    val logger = KotlinLogging.logger("TraktoMain")
    val filePath = args.getOrNull(0)
        ?: "HISTORY.html".takeIf {
            FileSystem.SYSTEM.exists(it.toPath())
        }
        ?: run {
            logger.info { "Enter the path to the HTML file: " }
            print("> ")
            readlnOrNull()?.trim() ?: return
        }
    logger.info {  }
    logger.info { "parsing $filePath" }

    val tracklist = parseHtmlFile(filePath.toPath())

    if (tracklist != null) {
//        createTracklist(parsedHtml)
        Exporter.write(
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


    logger.info { "PRESS ANY BUTTON TO CLOSE" }
    readlnOrNull()
}

