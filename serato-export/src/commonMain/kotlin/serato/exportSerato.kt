package serato

import com.github.ajalt.mordant.rendering.TextColors
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import splitTracklists

fun canExportSerato(): Boolean = FileSystem.SYSTEM.exists(defaultSeratoPath)

suspend fun exportSerato(seratoPath: Path = defaultSeratoPath) {
    val logger = KotlinLogging.logger("exportSerato.kt")
//    val documents = executeCommand("powershell.exe -Command [Environment]::GetFolderPath('MyDocuments')")
//    logger.info { documents }

    val sessions = getSeratoHistory(seratoPath)

    sessions.flatMap { session ->

        logger.info { "converting session ${TextColors.brightGreen(session.date)}" }
        logger.debug { session }

        val tracklist = seratoSessionToTracklist(session)
        logger.debug { "tracklist: $tracklist" }

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
}