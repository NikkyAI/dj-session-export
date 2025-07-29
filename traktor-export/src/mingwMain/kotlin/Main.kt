import com.saveourtool.okio.safeToRealPath
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import nl.adaptivity.xmlutil.serialization.XML
import okio.FileNotFoundException
import okio.Path
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

val xmlLenient = XML {
    recommended {
        ignoreUnknownChildren()
        pedantic = false
    }
}
//val xml = XML {
//    recommended {
//        pedantic = false
//    }
//}

fun parseNml(nmlPath: Path): Tracklist<Track>? {

    val dateTime = LocalDateTime.parse(
        nmlPath.name,
        LocalDateTime.Format {
            chars("history_")
            year()
            char('y')
            monthNumber()
            char('m')
            day()
            char('d')
            char('_')
            hour()
            char('h')
            minute()
            char('m')
            second()
            char('s')
            chars(".nml")
        }
    )


    val history = FileSystem.SYSTEM.read(nmlPath) {
        readUtf8()
    }.let {
        xmlLenient.decodeFromString(History.serializer(), it)
    }

//    println("collection: ${history.collection}")
//    println("playlists: ${history.playlists}")

    history.playlists.folderNode.name.let { require(it == $$"$ROOT") }
    history.playlists.folderNode.subnodes.let { require(it.count == 1) }

    val entries =
        history.playlists
            .folderNode.subnodes
            .playlistNode.playlist.entries
            .filter { it.extendedData.playedPublic == 1 }
            .filter { it.primaryKey.type == "TRACK" }
            .map { entry ->

                val startDate = entry.extendedData.startDate.let { startDate ->
                    val day = startDate % 256
                    val month = (startDate / 256) % 256
                    val year = (startDate / 256) / 256
                    LocalDate(year,month,day)
                }

                val start = LocalDateTime(
                    // just guessing a offset here
                    startDate,
                    LocalTime.fromSecondOfDay(entry.extendedData.startTime)
                )
                    .toInstant(TimeZone.currentSystemDefault())

                entry to start
            }


    val referenceInstant = entries.firstOrNull()?.second ?: Clock.System.now()

    val tracks = entries.map { (entry, start) ->
        val collectionEntry = history.collection.entry.firstOrNull {
            it.location.fullPath == entry.primaryKey.key
        } ?: error("failed to find track data for ${entry.primaryKey.key}")

//        println(start)
//        println(collectionEntry.title)
//        println(collectionEntry.artist)

        Track(
            time = start - referenceInstant,
            playDuration = entry.extendedData.duration.seconds,
            endTime = (start - referenceInstant) + entry.extendedData.duration.seconds,
            startAt = start,
            title = collectionEntry.title,
            artist = collectionEntry.artist?.deduplicateRepeating(),
            album = collectionEntry.album?.title,
            label = collectionEntry.info?.label,
            remixer = collectionEntry.info?.remixer,
            key = collectionEntry.info?.key,
            genre = collectionEntry.info?.genre,
            file = collectionEntry.location.file ?: collectionEntry.location.webAddress,
        )
    }

    val traktorFolderName = nmlPath.parent?.takeIf { it.name == "History" }
        ?.parent?.takeIf { it.name.startsWith("Traktor") }
        ?.name
        ?.replace(" ", "_")

    val tracklists = Tracklist(
        title = nmlPath.name.substringBeforeLast(".nml"),
        exportPath = traktorFolderName?.let { getExportFolder() / it }
//            ?: nmlPath.safeToRealPath().parent
            ?: ".".toPath(),
        tracks = tracks
    )

    return tracklists

//    genreBreakdown(tracklist) { genre }

}

fun main(vararg args: String) {
    val epochDays = LocalDate(2024, 11, 19).toEpochDays()
//    println(Int.MAX_VALUE)
    println(epochDays)
    val refEpoch = epochDays - 132647699
    println(refEpoch)
    println(refEpoch.days)
    val date = LocalDate.fromEpochDays(refEpoch)
    println(date)
//    exitProcess(0)

    val documents = executeCommandAndCaptureOutput(
        listOf(
            "powershell.exe",
            "-Command",
            "[Environment]::GetFolderPath('MyDocuments')"
        )
    )

    val nativeInstrumentsPath = documents.toPath() / "Native Instruments"

    val nmlFiles = if (args.isEmpty()) {
        println("searching in $nativeInstrumentsPath")

        FileSystem.SYSTEM.list(nativeInstrumentsPath)
            .filter {
                it.name.startsWith("Traktor")
            }
            .flatMap { traktorPath ->
                FileSystem.SYSTEM.list(traktorPath / "History")
            }
            .map {
                it.safeToRealPath()
            }
            .distinct()
    } else {
        val paths = args.map { it.toPath() }.filter {
            FileSystem.SYSTEM.exists(it)
        }
        paths.filter { it.name.endsWith(".nml") } + paths
            .filterNot { it.name.endsWith(".nml") }
            .flatMap { folder ->
                println("scanning $folder")
                try {
                    FileSystem.SYSTEM.listRecursively(folder, followSymlinks = false)
                        .filter { it.name.endsWith(".nml") }
//                        .map { it.safeToRealPath() }
                        .toList()
                } catch (e: FileNotFoundException) {
                    println()
                    e.printStackTrace()
                    println()
                    emptyList()
                }
            }
            .map {
                it.safeToRealPath()
            }
            .distinct()
    }

    if (nmlFiles.isEmpty()) {
        println("no nml file locations passed or found in $nativeInstrumentsPath")
        exitProcess(1)
    }

    nmlFiles.flatMap { nmlPath ->
        try {
            println("processing ${nmlPath.safeToRealPath()}")

            parseNml(nmlPath.safeToRealPath())
                ?.splitTracklists(
                    { it.time },
                    { track, diff ->
                        track.copy(
                            time = track.time - diff,
                            endTime = (track.time - diff) + track.playDuration
                        )
                    }
                ) { lastTrack, nextTrack -> nextTrack.endAt - lastTrack.startAt }
                .orEmpty()
        } catch (e: Exception) {
            println()
            e.printStackTrace()
            println()
            //emptyList()
            exitProcess(-1)
        }
    }.let {

        Template.write(it, Track.serializer())
    }

    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}
