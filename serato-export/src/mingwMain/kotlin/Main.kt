import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

fun trackListFrom(session: Session): Tracklist<HistoryTrack>? {
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

        val dateTimeExportFormat = LocalDateTime.Format {
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

        Tracklist(
            title = referenceInstant.toLocalDateTime(
                TimeZone.currentSystemDefault()
            ).format(dateTimeExportFormat),
            exportPath = getExportFolder() / "Serato",
            tracks = tracks
        )
    } catch (error: Exception) {
        println("Error reading file: \n$error")
        error.printStackTrace()
        null
    }
}

fun main(vararg args: String) {
//    val documents = executeCommand("powershell.exe -Command [Environment]::GetFolderPath('MyDocuments')")
//    println(documents)

    val sessions = getSeratoHistory()

    sessions.flatMap { session ->

        println()
        println("converting $session")

        val tracklist = trackListFrom(session)
        println(tracklist)

        tracklist?.splitTracklists(
            { it.time },
            { track, diff ->
                track.copy(
                    time = track.time - diff,
                )
            },
        ) ?: emptyList()
    }.let {
        Exporter.write(
            it,
            HistoryTrack.serializer(),
        )
    }
    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

