package traktor

import traktor.TrackData
import Tracklist
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.select.Elements
import com.saveourtool.okio.safeToRealPath
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val durationFormat = LocalTime.Companion.Format {
    hour(padding = Padding.NONE)
    char('h')
    minute(padding = Padding.ZERO)
    char(':')
    second(padding = Padding.ZERO)
}

fun parseHtmlFile(filePath: Path): Tracklist<TrackData>? {
    val logger = KotlinLogging.logger("traktor.parseHtmlFile")
    return try {
        val data = FileSystem.Companion.SYSTEM.read(filePath) {
            readUtf8()
        }

        val document = Ksoup.parse(data)
        val h1 = document.selectFirst("h1")
        if (h1 == null) {
            logger.info { "Title (h1) not found" }
            return null
        }
        val title = h1.text().trim().substringAfter("Track List: ")
        val table = document.selectFirst("table.border")
        if (table == null) {
            logger.info { "Table not found" }
            return null
        }

        val tracks = mutableListOf<TrackData>()
        val rows = table.select("tr")

        val headerCells = rows[0].select("th").map { it.text() }

        val missingFields = mutableSetOf<String>()
        val missingOptionalFields = mutableSetOf<String>()
        fun findField(fieldName: String): Elements.() -> String {
            val index = headerCells.indexOf(fieldName) // .takeUnless { it < 0 }

            if (index < 0) {
                missingFields += fieldName
//                    logger.info { "missing field $fieldName" }
//                        error("missing field '$fieldName' \navailable fields: $headerCells")

            }

            return {
                this[index].text().trim()
            }
        }

        fun findFieldOptional(fieldName: String): Elements.() -> String? {
            val index = headerCells.indexOf(fieldName) // .takeUnless { it < 0 }

            if (index < 0) {
                missingOptionalFields += fieldName
                return { null }
//                    logger.info { "missing field $fieldName" }
//                        error("missing field '$fieldName' \navailable fields: $headerCells")

            }

            return {
                this[index].text().trim()
            }
        }

        val trackNumField = findFieldOptional("Num.")
        val titleField = findField("Title")
        val artistField = findField("Artist")
        val genreField = findFieldOptional("Genre")
        val startTimeField = findField("Start Time")
        val durationField = findField("Duration")
        val deckField = findFieldOptional("Deck")
        val keyField = findFieldOptional("Key")


        if (missingOptionalFields.isNotEmpty()) {
            logger.info { "Missing optional fields:" }
            logger.info { missingOptionalFields.joinToString { "'$it'" } }
        }
        if (missingFields.isNotEmpty()) {
            logger.info { "Missing required fields:" }
            logger.info { missingFields.joinToString { "'$it'" } }
        }
        if (missingFields.isNotEmpty() || missingOptionalFields.isNotEmpty()) {
            logger.info { "Available Fields:" }
            logger.info { headerCells.joinToString { "'$it'" } }

        }

        if (missingFields.isNotEmpty()) {
            return null
        }

        val firstRow = rows[1].select("td")
        val referenceTimestamp = parseInstant(firstRow.startTimeField())
        for (i in 1 until rows.size) { // Skip the first row (header row)
            val cells = rows[i].select("td")
//            logger.info { "parsing row: $cells" }
            if (cells.size >= 10) {
                val timestamp = parseInstant(cells.startTimeField())
                tracks.add(
                    TrackData(
                        position = cells.trackNumField()?.toInt() ?: i,
                        time = timestamp - referenceTimestamp,
                        title = cells.titleField(),
                        artist = cells.artistField(),
                        genre = cells.genreField(),
                        startAt = timestamp,
                        duration = run {
                            val duration = cells.durationField()

                            logger.info { "parsing duration: $duration" }

                            val components = duration
                                .split(":")
                                .map { it.toInt() }
                                .reversed()
                            val d =components[0].seconds +
                                    components[1].minutes +
                                    (components.getOrNull(2)?.hours ?: Duration.ZERO)+
                                    (components.getOrNull(3)?.days ?: Duration.ZERO)
                            d
                           // val localTime = LocalTime.Companion.parse("0h" + duration, traktor.durationFormat)
//                            val localTime = LocalTime.parse(duration, traktor.durationFormat)
                           // localTime.toSecondOfDay().seconds
                        },
                        deck = cells.deckField(),
                        key = cells.keyField()
                    )
//                        .also {
//                            logger.info { it }
//                        }
                )
            }
        }

        Tracklist(
            title = title,
            exportPath = filePath.safeToRealPath().parent ?: ".".toPath(),
            tracks = tracks.sortedBy { it.startAt }
        )
        //.sortedBy { it.trackNum }

    } catch (error: Exception) {
        logger.info { "Error reading file: \n$error" }
//        error.printStackTrace()
        null
    }
}