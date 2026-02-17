@file:OptIn(ExperimentalTime::class)

package virtualdj

import Tracklist
import getExportFolder
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import splitTracklists
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val dateFormat = LocalDate.Format {
    year()
    char('/')
    monthNumber()
    char('/')
    day()
}
val timeFormat = LocalTime.Format {
    hour()
    char(':')
    minute()
}

val tracklistDocuments by lazy {
    Folders.userDocuments / "VirtualDJ" / "History" / "tracklist.txt"
}
val tracklistLocalAppdata by lazy {
    Folders.LOCAL_APPDATA / "VirtualDJ" / "History" / "tracklist.txt"
}

fun getPathsFiltered(paths: List<Path>): List<Path> {
    val varArgPath = paths.filter { it.name.endsWith("tracklist.txt") }
    return varArgPath.ifEmpty {
        listOf(
            tracklistDocuments,
            tracklistLocalAppdata,
        )
    }
}

fun canExportVirtualDJ(paths: List<Path>): Boolean {
    return getPathsFiltered(paths).any {
        FileSystem.SYSTEM.exists(it)
    }
//    return listOf(
//        *paths.filter { it.name.endsWith("tracklist.txt") }.toTypedArray(),
//        tracklistDocuments,
//        tracklistLocalAppdata,
//    ).any {
//        FileSystem.SYSTEM.exists(it)
//    }
}

fun exportVirtualDJ(paths: List<Path>): Unit = runBlocking {
//    logger.info { "args: $paths" }

    val tracklistTxt = getPathsFiltered(paths)
        .first {
            FileSystem.SYSTEM.exists(it)
        }
//    val tracklistTxt = listOf(
//        *paths.filter { it.name.endsWith("tracklist.txt") }.toTypedArray(),
//        tracklistDocuments,
//        tracklistLocalAppdata,
//    ).first {
//        FileSystem.SYSTEM.exists(it)
//    }

    val trackEndRegex = "\\(\\d+\\)$".toRegex()

    FileSystem.SYSTEM.read(tracklistTxt) {
        readUtf8()
    }
        .split("VirtualDJ History ")
        .drop(1)
        .map { txt ->
            val lines = txt.lines()
            val date = LocalDate.parse(lines[0], dateFormat)
            val trackLines = lines.drop(2)
                .filter {
                    it.isNotBlank()
                }

            val startTime = LocalDateTime(
                date,
                LocalTime.parse(
                    trackLines.first().take(5),
                    timeFormat
                )
            ).toInstant(
                TimeZone.currentSystemDefault()
            )

            fun convertTrack(
                i: Int,
                line: String,
                lastTimeStamp: Instant = startTime,
            ): SimpleTrack {
                val timeString = line.take(5)
                val time = LocalTime.parse(timeString, timeFormat)
                val rest = line.drop(5).trim(' ', ':', '-')
                val title = rest.substringAfterLast(" - ")
                    .replace(trackEndRegex, "")
                    .trimStart(' ', ':', '-')
                val artist = rest.substringBefore(title).trim(' ', ':', '-')

                var instant = LocalDateTime(date, time).toInstant(TimeZone.currentSystemDefault())
                while (
                    instant < lastTimeStamp
                ) {
                    instant += 1.days
                }
                return SimpleTrack(
                    position = i + 1,
                    time = instant - startTime,
                    startAt = instant,
                    title = title,
                    artist = artist.takeIf { it.isNotBlank() }
                )
            }

            val tracks = trackLines.drop(1).runningFoldIndexed(
                convertTrack(0, trackLines.first())
            ) { i, lastTrack, line ->
                convertTrack(i, line, lastTrack.startAt)
            }
            val title = date.format(LocalDate.Formats.ISO)
            Tracklist(
                title = title,
                exportPath = getExportFolder() / "VirtualDJ" / "${date.year}",
                tracks = tracks,
            )
        }.flatMap { tracklist ->
            tracklist.splitTracklists(
                { it.time },
                { track, diff ->
                    track.copy(
                        time = track.time - diff
                    )
                },
            ) { a, b -> b.time - a.time }
                .mapIndexed { i, tracklist ->
                    if (i == 0) return@mapIndexed tracklist
                    tracklist.copy(
                        title = tracklist.tracks.first().startAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            .format(
                                LocalDateTime.Format {
                                    date(LocalDate.Formats.ISO)
                                    char(' ')
                                    hour()
                                    char('-')
                                    minute()
                                }
                            ),
                        tracks = tracklist.tracks.mapIndexed { i, track ->
                            track.copy(position = i + 1)
                        }
                    )
                }
        }
        .toList()
        .let {
            Exporter.write(
                tracklists = it,
                serializer = SimpleTrack.serializer(),
                templateFolder = getExportFolder() / "VirtualDJ",
                openFolders = listOf(getExportFolder() / "VirtualDJ"),
            )
        }

////    val vdjFolder = databaseXmlPath.parent ?: error("failed to find parent path")
//    val historyFolder = tracklistTxt.parent ?: error("failed to find parent path")
//    logger.info { historyFolder }
//
//    val historyM3Us = FileSystem.SYSTEM.listRecursively(historyFolder)
//        .filter { it.name.endsWith(".m3u") }
//    logger.info { historyM3Us.toList() }
//
//    historyM3Us.map { m3uPath ->
//        val relativePath = m3uPath.relativeTo(historyFolder)
//
//        logger.info { "parsing $m3uPath" }
//        val extVDJTracks = FileSystem.SYSTEM.read(m3uPath) {
//            readUtf8()
//        }.split("#EXTVDJ:")
//            .drop(1)
//            .filter { it.isNotBlank() }
//            .map {
//                val lines = it.lines()
//                logger.info { "lines: $lines" }
//                val xmlStr = lines[0].replace("&", "&amp;")
//                val file = lines[1]
//                logger.info { "parsing $xmlStr" }
//                val extVDJ = xml.decodeFromString(ExtVDJ.serializer(), "<ExtVDJ>$xmlStr</ExtVDJ>")
//
//                file to extVDJ
//            }
//
//        val sessionStartTime = extVDJTracks.first().second.lastplaytime.let {
//            Instant.fromEpochSeconds(it)
//        }
//
//        val tracks = extVDJTracks.mapIndexed { i, (file, extVDJ) ->
//            val instant = Instant.fromEpochSeconds(extVDJ.lastplaytime)
//            M3UTrack(
//                position = i + 1,
//                time = instant - sessionStartTime,
//                startAt = instant,
//                artist = extVDJ.artist?.takeIf { it.isNotBlank() },
//                title = extVDJ.title?.takeIf { it.isNotBlank() }
//                    ?: file.toPath().name.substringBeforeLast("."),
//                remix = extVDJ.remix?.takeIf { it.isNotBlank() },
//                file = file.toPath().name
//            )
//        }
//
//        Tracklist(
//            title = m3uPath.name.substringBeforeLast(".m3u"),
//            exportPath = getExportFolder() / "VirtualDJ" / relativePath.parent!!,
//            tracks = tracks,
//        )
//    }.flatMap { tracklist ->
//        tracklist.splitTracklists(
//            { it.time },
//            { track, diff ->
//                track.copy(
//                    time = track.time - diff
//                )
//            },
//        ) { a, b -> b.time - a.time }
//            .map { tracklist ->
//                tracklist.copy(
//                    tracks = tracklist.tracks.mapIndexed { i, track ->
//                        track.copy(position = i + 1)
//                    }
//                )
//            }
//    }
//        .toList()
//        .let {
//            Exporter.write(
//                tracklists = it,
//                serializer = M3UTrack.serializer(),
//                openFolders = listOf(getExportFolder() / "VirtualDJ")
//            )
//        }
//
//    val databaseXmlPath = listOf(
//        documents.toPath() / "VirtualDJ" / "database.xml",
//        localAppdata.toPath() / "VirtualDJ" / "database.xml",
//    ).first {
//        FileSystem.SYSTEM.exists(it)
//    }
//
//    val database = if (FileSystem.SYSTEM.exists(databaseXmlPath)) {
//        FileSystem.SYSTEM.read(databaseXmlPath) {
//            readUtf8()
//        }
//            .let {
//                logger.info { "decoding $databaseXmlPath" }
//                xml.decodeFromString(VirtualDJDatabase.serializer(), it)
//            }
//    } else {
//        logger.info { "database file $databaseXmlPath not found, some information may not be accurate" }
//
//        VirtualDJDatabase(songs = emptyList())
//    }
////        .also {
////            logger.info { it }
////        }
}
