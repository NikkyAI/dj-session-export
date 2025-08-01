package traktor

import com.github.ajalt.mordant.rendering.TextColors.*
import com.saveourtool.okio.pathString
import com.saveourtool.okio.safeToRealPath
import getExportFolder
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import splitTracklists
import kotlin.system.exitProcess

val nativeInstrumentsPath = Folders.userDocuments / "Native Instruments"

fun canExportTraktor(paths: List<Path>): Boolean {
    return if (paths.isEmpty()) {
        paths.any { path -> path.name.endsWith(".nml") && FileSystem.SYSTEM.exists(path) } ||
        paths.any { path -> FileSystem.SYSTEM.metadataOrNull(path)?.isDirectory ?: false }
    } else {
        FileSystem.SYSTEM.exists(nativeInstrumentsPath)
    }
}

fun exportTraktor(paths: List<Path>) {
    val logger = KotlinLogging.logger("exportTraktor.kt")


    val nmlFiles = if (paths.isEmpty()) {
        logger.info { "searching for ${brightWhite("Traktor")} folders in ${blue(nativeInstrumentsPath.pathString)}" }

        FileSystem.Companion.SYSTEM.list(nativeInstrumentsPath)
            .filter {
                it.name.startsWith("Traktor")
            }
            .flatMap { traktorPath ->
                FileSystem.Companion.SYSTEM.list(traktorPath / "History")
            }
            .map {
                it.safeToRealPath()
            }
            .distinct()
    } else {
        val paths = paths.filter {
            FileSystem.Companion.SYSTEM.exists(it)
        }
        paths.filter { it.name.endsWith(".nml") } + paths
            .filterNot { it.name.endsWith(".nml") }
            .flatMap { folder ->
                logger.info { "scanning ${blue(folder.pathString)}" }
                try {
                    FileSystem.Companion.SYSTEM.listRecursively(folder, followSymlinks = false)
                        .filter { it.name.endsWith(".nml") }
//                        .map { it.safeToRealPath() }
                        .toList()
                } catch (e: FileNotFoundException) {
                    logger.info { }
                    e.printStackTrace()
                    logger.info { }
                    emptyList()
                }
            }
            .map {
                it.safeToRealPath()
            }
            .distinct()
    }

    if (nmlFiles.isEmpty()) {
        logger.info { "no nml file locations passed or found in ${blue(nativeInstrumentsPath.pathString)}" }
        exitProcess(1)
    }

    nmlFiles.flatMap { nmlPath ->
        try {
            logger.info { "processing ${blue(nmlPath.safeToRealPath().pathString)}" }

            nmlToTracklist(nmlPath.safeToRealPath())
                ?.splitTracklists(
                    { it.time },
                    { track, diff ->
                        track.copy(
                            time = track.time - diff,
                            endTime = (track.time - diff) + track.playDuration
                        )
                    },
                ) { lastTrack, nextTrack -> nextTrack.endAt - lastTrack.startAt }
                .orEmpty()
//                .map { tracklist ->
//                    logger.info { "remapping ${tracklist.title} with ${tracklist.tracks.size} tracks" }
//                    val firstTrack = tracklist.tracks.minBy { it.startAt }
//                    val lastTrack = tracklist.tracks.maxBy { it.endAt }
//                    val duration = lastTrack.endAt - firstTrack.startAt
//                    val durationString = duration.toComponents { days, hours, minutes, seconds, _ ->
//
//                       if(days > 0) {
//                            "${days}d${hours}h${minutes}m${seconds}"
//                        } else if(hours > 0) {
//                           "${hours}h${minutes}m${seconds}"
//                       }else  {
//                           "${minutes}m${seconds}"
//                       }
//                    }
//                    tracklist.copy(
//                        title = firstTrack.startAt.toLocalDateTime(TimeZone.currentSystemDefault()).format(filenameFormat) + "-$durationString"
//                    )
//                }
        } catch (e: Exception) {
            logger.info { }
            e.printStackTrace()
            logger.info { }
            //emptyList()
            exitProcess(-1)
        }
    }.let { tracklists ->
        Exporter.write(
            tracklists = tracklists,
            serializer = Track.serializer(),
            openFolders = listOf(getExportFolder())
        )
    }
}