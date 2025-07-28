import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

//val dateFormat = LocalDate.Format {
//    monthNumber(padding = Padding.NONE)
//    char('/')
//    day(padding = Padding.NONE)
//    char('/')
//    year()
//}
//
//val timeFormat = LocalTime.Format {
//    amPmHour(padding = Padding.NONE)
//    char(':')
//    minute(padding = Padding.NONE)
//    char(':')
//    second(padding = Padding.NONE)
//    char(' ')
//    this.amPmMarker("AM", "PM")
//}

val dateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    time(LocalTime.Formats.ISO)
}
fun trackListFrom(session: Session): Tracklist<HistoryTrack>? {
    return try {
        val referenceInstant = session.songs.first().timePlayed
        //exportStartTime.toInstant(TimeZone.currentSystemDefault())

        val tracks = session.songs.map { it ->
            HistoryTrack(
                time = it.timePlayed - referenceInstant,
                timePlayed = it.timePlayed,
                title = it.title,
                artist = it.artist,
                filePath = it.filePath,
                bpm = it.bpm,
            )
        }


        Tracklist(
            title = referenceInstant.toLocalDateTime(
                TimeZone.currentSystemDefault()
            ).format(dateTimeFormat)

                .replace(":", "-")
                .replace("T", " "),
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
                    time = track.time - diff
                )
            },
        ) ?: emptyList()
    }.let {
        Template.write(
            it,
            HistoryTrack.serializer(),
        )
    }
    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

