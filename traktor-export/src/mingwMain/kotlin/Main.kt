import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import nl.adaptivity.xmlutil.serialization.XML
import okio.FileNotFoundException
import okio.Path
import okio.FileSystem
import okio.buffer
import okio.use
import okio.Path.Companion.toPath
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

val xmlLenient = XML {
    recommended {
        ignoreUnknownChildren()
        pedantic = false
//            autoPolymorphic = true
    }
}
val xml = XML {
    recommended {
        pedantic = false
//            autoPolymorphic = true
    }
}

fun parseNml(nmlPath: Path) {
//    val module = SerializersModule {
//        polymorphic(History.Node::class, ) {
//            subclass(History.Node.FolderNode::class, History.Node.FolderNode.serializer())
//            subclass(History.Node.PlaylistNode::class, History.Node.PlaylistNode.serializer())
//        }
//    }

    val history = FileSystem.SYSTEM.source(nmlPath).buffer().use {
        it.readUtf8()
    }.let {
        xmlLenient.decodeFromString(History.serializer(), it)
    }

    println("collection: ${history.collection}")
    println("playlists: ${history.playlists}")

    history.playlists.folderNode.name.let { require(it == $$"$ROOT") }
    history.playlists.folderNode.subnodes.let { require(it.count == 1) }

    val entries =
        history.playlists
            .folderNode.subnodes
            .playlistNode.playlist.entries
            .filter { it.extendedData.playedPublic == 1 }
            .map { entry ->
                val start = LocalDateTime(
                    // just guessing a offset here
                    LocalDate.fromEpochDays(entry.extendedData.startDate - 132625397),
                    LocalTime.fromSecondOfDay(entry.extendedData.startTime)
                )
                    .toInstant(TimeZone.UTC)
                entry to start
            }


    val referenceInstant = entries.firstOrNull()?.second ?: Clock.System.now()

    val tracks = entries.map { (entry, start) ->
        val collectionEntry = history.collection.entry.first() {
            it.location.fullPath == entry.primaryKey.key
        }
//                println("epochdays: " +Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toEpochDays())

        println(start)
        println(collectionEntry.title)
        println(collectionEntry.artist)

        Track(
            time = start - referenceInstant,
            duration = entry.extendedData.duration.seconds,
            endTime = (start - referenceInstant) + entry.extendedData.duration.seconds,
            title = collectionEntry.title,
            artist = collectionEntry.artist,
            album = collectionEntry.album?.title,
            label = collectionEntry.info?.label,
            remixer = collectionEntry.info?.remixer,
            key = collectionEntry.info?.key,
            genre = collectionEntry.info?.genre,
            file = collectionEntry.location.file,
        )
    }

    val tracklist = Tracklist(
        title = nmlPath.name.substringBeforeLast(".nml"),
        tracks = tracks
    )

    Template.write(tracklist, Track.serializer())
//    genreBreakdown(tracklist) { genre }

//        .let {
//            println("decoding XML")
//            xmlLenient.decodeFromString(VirtualDJDatabase.serializer(), it)
//        }
//        .let {
//            println(it)
//        }


}

fun main(vararg args: String) {
    val documents = executeCommand("powershell.exe -Command [Environment]::GetFolderPath('MyDocuments')")
    println(documents)

    val nativeInstrumentsPath = documents.toPath() / "Native Instruments"

    val nmlFiles = if (args.isEmpty()) {
        FileSystem.SYSTEM.list(nativeInstrumentsPath)
            .filter {
                it.name.startsWith("Traktor")
            }
            .flatMap { traktorPath ->
                FileSystem.SYSTEM.list(traktorPath / "History")
            }
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
            }.distinct()
    }


    if(nmlFiles.isEmpty()) {
        println("no nml file locations passed or found in $nativeInstrumentsPath")
        exitProcess(1)
    }
    
    nmlFiles.forEach { nmlPath ->
        println("processing $nmlPath")
        parseNml(nmlPath)
    }
    
//    FileSystem.SYSTEM.list(documents.toPath() / "Native Instruments")
//        .filter {
//            it.name.startsWith("Traktor")
//        }
//        .flatMap { traktorPath ->
//            FileSystem.SYSTEM.list(traktorPath / "History")
//        }
//        .forEach { traktorPath ->
//            println("processing $traktorPath")
//
//            println("listing ${traktorPath / "History"}")
//            FileSystem.SYSTEM.list(traktorPath / "History")
//                .filter { it.name.endsWith(".nml") }
//                .forEach { nmlPath ->
//                    println("processing $nmlPath")
//                    parseNml(nmlPath)
//                }
//        }


//    val tracklist = parseHtmlFile(filePath.toPath())
//
//    if (tracklist != null) {
////        createTracklist(parsedHtml)
//        Template.write(tracklist, TrackData.serializer())
//        genreBreakdown(tracklist) { genre }
//    }

    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

