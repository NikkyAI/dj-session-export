package serato

import Tracklist
import getExportFolder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

val dateTimeExportFormat = LocalDateTime.Companion.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char(' ')
    hour()
    char('-')
    minute()
}

fun seratoSessionToTracklist(session: Session): Tracklist<HistoryTrack>? {
    val logger = KotlinLogging.logger("seratoSessionToTracklist.kt")
    return try {
        val referenceInstant = session.songs.first().playedAt

        val tracks = session.songs.map { it ->
            HistoryTrack(
                time = it.playedAt - referenceInstant,
                playedAt = it.playedAt,
                title = it.title,
                artist = it.artist,
                filePath = it.filePath,
                bpm = it.bpm,
            )
        }

        Tracklist(
            title = referenceInstant.toLocalDateTime(
                TimeZone.Companion.currentSystemDefault()
            ).format(dateTimeExportFormat),
            exportPath = getExportFolder() / "Serato",
            tracks = tracks
        )
    } catch (error: Exception) {
        logger.info { "Error reading file: \n$error" }
        error.printStackTrace()
        null
    }
}