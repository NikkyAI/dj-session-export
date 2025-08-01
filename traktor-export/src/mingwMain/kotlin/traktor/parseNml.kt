package traktor

import Tracklist
import getExportFolder
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import nl.adaptivity.xmlutil.serialization.XML
import okio.Path
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.time.Clock
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

//val filenameFormat = LocalDateTime.Format {
//    chars("history_")
//    year()
//    char('-')
//    monthNumber()
//    char('-')
//    day()
//    char('_')
//    hour()
//    char('-')
//    minute()
//    char('-')
//    second()
//}

fun nmlToTracklist(nmlPath: Path): Tracklist<Track>? {
    val history = FileSystem.SYSTEM.read(nmlPath) {
        readUtf8()
    }.let {
        xmlLenient.decodeFromString(History.serializer(), it)
    }

//    logger.info { "collection: ${history.collection}" }
//    logger.info { "playlists: ${history.playlists}" }

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

//        logger.info { start }
//        logger.info { collectionEntry.title }
//        logger.info { collectionEntry.artist }

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
//         ?.replace(" ", "_")

    val tracklists = Tracklist(
        title = nmlPath.name.substringBeforeLast(".nml"),
        exportPath = getExportFolder() / (traktorFolderName ?: "Traktor"),
        tracks = tracks
    )

    return tracklists

//    genreBreakdown(tracklist) { genre }

}

