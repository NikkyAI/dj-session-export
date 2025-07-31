@file:OptIn(ExperimentalTime::class)

import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
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
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

//val xmlLenient = XML {
//    recommended {
//        ignoreUnknownChildren()
//        pedantic = false
//    }
//}
//val xml = XML {
//    recommended {
//        pedantic = false
//    }
//}


//@Serializable
//data class ExtVDJ(
//    @XmlElement
//    val time: String? = null,
//    @XmlElement
//    val lastplaytime: Long,
//    @XmlElement
//    val filesize: Long? = null,
//    @XmlElement
//    val artist: String? = null,
//    @XmlElement
//    val title: String? = null,
//    @XmlElement
//    val remix: String? = null,
//    @XmlElement
//    val songlength: Double? = null,
//)

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

@OptIn(
    ExperimentalForeignApi::class,
//    ExperimentalXmlUtilApi::class,
)
fun main(vararg args: String): Unit = runBlocking {
    println("args: ${args.toList()}")


    val documents = Command("powershell.exe")
        .args(
            "-Command",
            "[Environment]::GetFolderPath('MyDocuments')"
        )
        .stdout(Stdio.Pipe)
        .spawn()
        .waitWithOutput()
        .stdout
        ?.trim()
        ?: error("failed to get documents folder location using powershell")
    val localAppdata = getenv("LOCALAPPDATA")?.toKString() ?: error("cannot lookup %LOCALAPPDATA%")
    val tracklistTxt = listOf(
        *args.filter { it.endsWith(".txt") }.map { it.toPath() }.toTypedArray(),
        documents.toPath() / "VirtualDJ" / "History" / "tracklist.txt",
        localAppdata.toPath() / "VirtualDJ" / "History" / "tracklist.txt",
    ).first {
        FileSystem.SYSTEM.exists(it)
    }

    val trackEndRegex ="\\(\\d+\\)$".toRegex()

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
                lastTimeStamp: Instant = startTime
            ): SimpleTrack {
                val timeString = line.take(5)
                val time = LocalTime.parse(timeString, timeFormat)
                val rest = line.drop(5).trim(' ', ':', '-')
                val title = rest.substringAfterLast(" - ")
                    .replace(trackEndRegex, "")
                    .trimStart(' ', ':', '-')
                val artist = rest.substringBefore(title).trim(' ', ':', '-')

                var instant = LocalDateTime(date, time).toInstant(TimeZone.currentSystemDefault())
                while(
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
                    if(i == 0) return@mapIndexed tracklist
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
//    println(historyFolder)
//
//    val historyM3Us = FileSystem.SYSTEM.listRecursively(historyFolder)
//        .filter { it.name.endsWith(".m3u") }
//    println(historyM3Us.toList())
//
//    historyM3Us.map { m3uPath ->
//        val relativePath = m3uPath.relativeTo(historyFolder)
//
//        println("parsing $m3uPath")
//        val extVDJTracks = FileSystem.SYSTEM.read(m3uPath) {
//            readUtf8()
//        }.split("#EXTVDJ:")
//            .drop(1)
//            .filter { it.isNotBlank() }
//            .map {
//                val lines = it.lines()
//                println("lines: $lines")
//                val xmlStr = lines[0].replace("&", "&amp;")
//                val file = lines[1]
//                println("parsing $xmlStr")
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
////            Template.write(
////                tracklists = it,
////                openFolders = listOf(getExportFolder() / "VirtualDJ")
////            ) {
////                val time = time.formatTimestamp()
////
////                if (artist != null && title != null) {
////                    "$time $artist - $title"
////                } else if (title != null) {
////                    "$time $title"
////                } else {
////                    "$time $file"
////                }
////            }
//            if (it.isNotEmpty()) {
//                exitProcess(0)
//            }
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
//                println("decoding $databaseXmlPath")
//                xml.decodeFromString(VirtualDJDatabase.serializer(), it)
//            }
//    } else {
//        println("database file $databaseXmlPath not found, some information may not be accurate")
//
//        VirtualDJDatabase(songs = emptyList())
//    }
////        .also {
////            println(it)
////        }
//
//    val cueFiles = if (args.isEmpty()) {
//
//        val folders = database.songs.mapNotNull {
//            it.filePath.toPath().parent?.safeToRealPath()
//        }.distinct()
//
//        val realFolders = folders.filter {
//            FileSystem.SYSTEM.exists(it)
//        }
//
//        println("folders: $folders")
//        println("realFolders: $realFolders")
//
//        realFolders
//            .run {
//                takeUnless { it.isEmpty() }
//                    ?: listOf(
////                    ".".toPath().safeToRealPath(),
//                        "..".toPath().safeToRealPath(),
//                    )
//            }
//            .filter {
//                FileSystem.SYSTEM.exists(it)
//            }
//            .flatMap { folder ->
//                println("scanning $folder")
//                try {
//                    FileSystem.SYSTEM.listRecursively(folder, followSymlinks = false)
//                        .filter { it.name.endsWith(".cue") }
//                        .map { it.safeToRealPath() }
//                        .toList()
//                } catch (e: FileNotFoundException) {
//                    println()
//                    e.printStackTrace()
//                    println()
//                    emptyList()
//                }
//            }.distinct()
//    } else {
//        val paths = args.map { it.toPath() }.filter {
//            FileSystem.SYSTEM.exists(it)
//        }
//        paths.filter { it.name.endsWith(".cue") } + paths
//            .filterNot { it.name.endsWith(".cue") }
//            .flatMap { folder ->
//                println("scanning $folder")
//                try {
//                    FileSystem.SYSTEM.listRecursively(folder, followSymlinks = false)
//                        .filter { it.name.endsWith(".cue") }
//                        .map { it.safeToRealPath() }
//                        .toList()
//                } catch (e: FileNotFoundException) {
//                    println()
//                    e.printStackTrace()
//                    println()
//                    emptyList()
//                }
//            }.distinct()
//    }
//
//    if (cueFiles.isEmpty()) {
//        println("no cue file locations passed or found in $databaseXmlPath ")
//        exitProcess(1)
//    }
//
//    println("cue files: $cueFiles")
//    val cues = cueFiles.map {
//        parseCue(it)
//    }
//
//    cues.flatMap { cueFile ->
//        println()
//        println("cue file: $cueFile")
//        val recording = database.songs.firstOrNull() { it.filePath.endsWith(cueFile.file) }
//
//        println("recording: ${recording?.filePath}")
//        if (recording == null) {
//            println("WARN: MISSING RECORDING for ${cueFile.file}")
//        }
//        val date = recording
//            ?.comment
//            ?.substringAfter("Recorded using VirtualDJ on ")
//            ?.let {
//                LocalDate.parse(it, LocalDate.Formats.ISO)
//            }
//        val tracklist = Tracklist(
//            title = date?.toString() ?: cueFile.file,
//            exportPath = getExportFolder() / "VirtualDJ",
//            tracks = cueFile.tracks.map { cueTrack ->
//                Track(
//                    position = cueTrack.trackNum,
//                    time = cueTrack.timestamp,
//                    title = cueTrack.title,
//                    artist = cueTrack.performer
//                )
//            }
//        )
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
//
//    }.let {
//        Exporter.write(
//            it,
//            Track.serializer(),
//        )
//    }
//
//
//    database.songs.filter {
//        it.comment?.startsWith("Recorded using VirtualDJ on ") ?: false
//    }.filter {
//        it.filePath.toPath().name !in cues.map { it.file }
//    }.mapNotNull { recording ->
//        val date = recording.comment!!.substringAfter("Recorded using VirtualDJ on ")
//            .let {
//                LocalDate.parse(it, LocalDate.Formats.ISO)
//            }
//        val tracks = recording.pois
//            .filter { it.type == "cue" }
//            .sortedBy { it.num ?: Int.MAX_VALUE }
//            .map { cuePoint ->
//                val name = cuePoint.name ?: error("missing name on $cuePoint")
////                val song = virtualDjDb.songs.firstOrNull { s ->
////                    val authors = s.tags.author?.replace("\\s+", " ")?.split(", ") ?: return@firstOrNull false
////                    val title = s.tags.title?.replace("\\s+", " ") ?: return@firstOrNull false
////                    val remix = s.tags.remix?.replace("\\s+", " ") ?: return@firstOrNull false
////                    val feat = authors.drop(1).takeUnless { it.isEmpty() }
////                    "${authors.first()} - $title ($remix)" == name
////                } ?: virtualDjDb.songs.firstOrNull { s ->
////                    val authors = s.tags.author?.replace("\\s+", " ")?.split(", ") ?: return@firstOrNull false
////                    val title = s.tags.title?.replace("\\s+", " ") ?: return@firstOrNull false
////                    val feat = authors.drop(1).takeUnless { it.isEmpty() }
////                    if(feat)
////                    "${authors.first()} - $title" == name
////                } ?: virtualDjDb.songs.firstOrNull { s->
////                    s.filePath.substringAfterLast("\\").substringBeforeLast(".")
////                        .replace(" ", "")
////                        .replace("&amp;", "") == name.replace(" ", "")
////                }
////                if(song == null) {
////                    println("MISSING TRACK for: $cuePoint")
////                }
////                Triple(cuePoint.pos.seconds, song, cuePoint.name)
//                SimpleTrack(
//                    position = cuePoint.num ?: 0,
//                    time = cuePoint.pos.seconds,
//                    title = name
//                )
//            }
//
//        val recordingDuration = recording.infos.songLength?.toDouble()?.seconds
//
////        val firstSeen = recording.infos.firstSeen.let { Instant.fromEpochSeconds(it) }
////        val lastModified = recording.infos.lastModified.let { Instant.fromEpochSeconds(it) }
//
////        println("recording on $date")
////        println("duration $recordingDuration")
//
////        tracks.forEach { (start, name) ->
////            println("${start.formatTimestamp()} $name")
////        }
//        val tracklist = Tracklist(
//            title = date.toString(),
//            exportPath = getExportFolder() / "VirtualDJ",
//            tracks = tracks,
//        )
//        if (tracklist.tracks.isNotEmpty()) {
//            tracklist.splitTracklists(
//                { it.time },
//                { track, diff ->
//                    track.copy(
//                        time = track.time - diff
//                    )
//                },
//            ) { a, b -> b.time - a.time }
//        } else {
//            println("tracklist was empty")
//            null
//        }
//    }.flatten()
//        .let {
//            Exporter.write(
//                it,
//                SimpleTrack.serializer(),
//                defaultTemplate = "{time} {title}",
//                templateKey = "template_simple"
//            )
//        }

    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()

    exitProcess(0)
}
