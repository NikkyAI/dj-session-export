import app.softwork.serialization.csv.CSVFormat
import app.softwork.serialization.flf.FixedLengthFormat
import com.saveourtool.okio.safeToRealPath
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import okio.Path
import okio.FileSystem
import okio.Path.Companion.toPath

private val logger = KotlinLogging.logger("SeratoMain")

val dateFormat = LocalDate.Format {
    monthNumber(padding = Padding.NONE)
    char('/')
    day(padding = Padding.NONE)
    char('/')
    year()
}

val timeFormat = LocalTime.Format {
    amPmHour(padding = Padding.NONE)
    char(':')
    minute(padding = Padding.NONE)
    char(':')
    second(padding = Padding.NONE)
    char(' ')
    this.amPmMarker("AM", "PM")
}

val dateTimeFormat = LocalDateTime.Format {
    date(dateFormat)
    char(' ')
    time(timeFormat)
}

fun parseLocalDatetime(startTimeString: String): LocalDateTime {
    return LocalDateTime.parse(
        startTimeString,
        dateTimeFormat
    )
}

fun parseInstant(startTimeString: String): Instant {
    return parseLocalDatetime(startTimeString)
        .toInstant(TimeZone.currentSystemDefault())
}


@OptIn(ExperimentalSerializationApi::class)
fun parseFLF(filePath: Path): List<SeratoExport> {
    val data = FileSystem.SYSTEM
        .read(filePath) {
            readUtf8()
        }
        .let {
            it.lines().filterIndexed { i, s -> i != 0 && i != 1 && i != 3 }
        }
        .filter { it.isNotBlank() }
        .map { line ->
            logger.info { line }
            val flf = FixedLengthFormat.decodeFromString(SeratoExportFLF.serializer(), line.take(156))
            SeratoExport(
                name = flf.name.trim(),
                startTime = flf.startTime.trim(),
                endTime = flf.endTime.trim(),
                playtime = flf.playtime,
                deck = flf.deck.trim(),
                notes = line.drop(156).trim()
            ).also {
                logger.info { it }
            }
        }
    return data
}

@OptIn(ExperimentalSerializationApi::class)
fun parseCSV(filePath: Path): List<SeratoExport> {

    val data = FileSystem.SYSTEM.read(filePath) {
        readUtf8()
    }.let { csv ->
        CSVFormat {
            separator = ','
            alwaysEmitQuotes = true
        }.decodeFromString(ListSerializer(SeratoExport.serializer()), csv)
    }
    return data
}

fun trackListFrom(filePath: Path, data: List<SeratoExport>): Tracklist<SeratoTrack>? {
    return try {
        val exportData = data.first()
        val exportDate = LocalDate.parse(exportData.name, dateFormat)
        val exportStartTime = parseLocalDatetime(exportData.startTime)
        val exportEndTime = parseLocalDatetime(exportData.endTime)

        val exportDuration = exportData.playtime
        val trackData = data.drop(1).map {
            val startTime = LocalDateTime(
                exportStartTime.date,
                LocalTime.parse(it.startTime, timeFormat)
            ).toInstant(TimeZone.currentSystemDefault())
            val endTime = LocalDateTime(
                exportStartTime.date,
                LocalTime.parse(it.endTime, timeFormat)
            ).toInstant(TimeZone.currentSystemDefault())
            Triple(it, startTime, endTime)
//            .SeratoTrack(
//                time = (startTime - referenceInstant).formatTimestamp(),
//                title = it.name,
//                startTime = startTime,
//                endTime = endTime,
//                playTime = it.playtime,
//                deck = it.deck.toInt(),
//                notes = it.notes,
//            )
        }
        val referenceInstant = trackData.first().second
        //exportStartTime.toInstant(TimeZone.currentSystemDefault())

        val tracks = trackData.map { (it, startTime, endTime) ->

            SeratoTrack(
                time = startTime - referenceInstant,
                title = it.name,
                startAt = startTime,
                endAt = endTime,
                playDuration = it.playtime,
                deck = it.deck.toInt(),
                notes = it.notes,
            )
        }


        Tracklist(
            title = exportStartTime.toString()
                .replace(":", "-")
                .replace("T", " ") + "." + filePath.name.substringAfterLast("."),

            exportPath = filePath.safeToRealPath().parent ?: ".".toPath(),
            //getExportFolder() / "serato-convert",
            tracks = tracks
        )
    } catch (error: Exception) {
        logger.info { "Error reading file: \n$error" }
        error.printStackTrace()
        null
    }
}

fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
//    val documents = executeCommand("powershell.exe -Command [Environment]::GetFolderPath('MyDocuments')")
//    logger.info { documents }

    val args = args
        .toList()
        .takeUnless { it.isEmpty() }
        ?: run {
            listOf(
                "TEST.csv",
                "TEST.txt",
                "../TEST.csv",
                "../TEST.txt",
            )
                .filter {
                    FileSystem.SYSTEM.exists(it.toPath())
                }
                .takeUnless { it.isEmpty() }
        }
            ?.filter {
                FileSystem.SYSTEM.exists(it.toPath())
            }?.takeUnless { it.isEmpty() }
        ?: run {
            logger.info { "Enter the path to the csv file: " }
            print("> ")
            listOf(readlnOrNull()?.trim() ?: return@runBlocking)
        }
    logger.info { "parsing $args" }
    val tracklists = args.flatMap { filePath ->

        logger.info {  }
        logger.info { "parsing $filePath" }

        val data = if (filePath.endsWith(".csv")) {
            parseCSV(filePath.toPath())
        } else if (filePath.endsWith(".txt")) {
            parseFLF(filePath.toPath())
        } else {
            error("expected file extension $filePath")
        }

        val tracklist = trackListFrom(filePath.toPath(), data)


        tracklist?.splitTracklists(
            { it.time },
            { track, diff ->
                track.copy(
                    time = track.time - diff
                )
            }
        ) { lastTrack, nextTrack -> lastTrack.endAt - nextTrack.startAt }
            ?: emptyList()
    }
    Exporter.write(
        tracklists,
        SeratoTrack.serializer(),
        defaultTemplate = "{time} - {title}"
    )

    logger.info { "PRESS ANY BUTTON TO CLOSE" }
    readlnOrNull()
}

