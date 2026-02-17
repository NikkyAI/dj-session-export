@file:OptIn(ExperimentalSerializationApi::class)

import app.softwork.serialization.csv.CSVFormat
import com.github.ajalt.mordant.rendering.TextColors
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import com.saveourtool.okio.pathString
import com.saveourtool.okio.safeToRealPath
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import okio.SYSTEM


object Exporter {
    private val logger = KotlinLogging.logger("Exporter.kt")

    val default = """
        {time} {artist} - {title}
    """.trimIndent().trim()

    fun openFolder(path: Path) {
        logger.info { "ii ${TextColors.blue(path.pathString)}" }
        try {
            Command("powershell.exe")
                .args(
                    "-Command",
                    "Invoke-Item",
                    "'$path'",
                )
                .stdout(Stdio.Inherit)
                .spawn()
                .wait()
        } catch (e: Exception) {
            logger.info { e.message }
        }
    }

    private fun loadFormatter(
        dir: Path,
        defaultTemplate: String = default,
        templateKey: String = "template",
    ): (JsonObject) -> String {
        val templatePath = dir / "$templateKey.txt"
        val exists = FileSystem.SYSTEM.exists(templatePath)
        if (!exists) {
            FileSystem.SYSTEM.write(templatePath) {
                writeUtf8(
                    defaultTemplate
                )
            }
        }
        val templateString = FileSystem.SYSTEM.read(templatePath) {
            readUtf8().trim()
        }

        return { song: JsonObject ->
            song.entries.fold(templateString) { t, (key, value) ->
                t.replace("{$key}", value.jsonPrimitive.contentOrNull ?: "???")
            }
        }
    }

//    suspend fun <E> write(
//        tracklist: Tracklist<E>,
//        serializer: KSerializer<E>,
////        defaultTemplate: String = default,
//    ) {
//        if (tracklist.tracks.isEmpty()) {
//            logger.info { "tracklist ${tracklist.title} was empty" }
//            return
//        }
//        logger.info { "writing ${tracklist.title}" }
//        if (!FileSystem.SYSTEM.exists(tracklist.exportPath.safeToRealPath())) {
//            logger.info { "creating ${tracklist.exportPath}" }
//            FileSystem.SYSTEM.createDirectories(
//                tracklist.exportPath.safeToRealPath()
//            )
//        }
//
//        val template = KorteTemplate(
//            "{{time}} {{artist }} - {{title ?: file}}",
//            config = KorteTemplateConfig(
//                autoEscapeMode = KorteAutoEscapeMode.RAW,
//
//            ),
//        )
//        val formatter = template.invoke(
//
//        )
//        val encodedSongs =
//            json.encodeToJsonElement(ListSerializer(elementSerializer = serializer), value = tracklist.tracks)
//                .jsonArray.toList().map {
//                    it.jsonObject
//                }
//        val txt = encodedSongs
//            .map { songObj ->
//                template.invoke(
//                    songObj.entries.associate { (key, value) ->
//                        key to value.jsonPrimitive.contentOrNull
//                    }
//                )
//            }
//            .joinToString("\n")
//        val txtPath = tracklist.exportPath.safeToRealPath() / "${tracklist.title}.new.txt"
//        logger.info { "writing to $txtPath" }
//        FileSystem.SYSTEM.write(txtPath) {
//            writeUtf8(txt)
//        }
//    }

    fun <E> write(
        tracklist: Tracklist<E>,
        serializer: KSerializer<E>,
        templateFolder: Path = tracklist.exportPath,
        defaultTemplate: String = default,
        templateKey: String = "template",
    ) {
        if (tracklist.tracks.isEmpty()) {
            logger.info { "tracklist ${TextColors.brightGreen(tracklist.title)} was empty" }
            return
        }
        logger.info { "writing ${TextColors.brightGreen(tracklist.title)}" }
        if (!FileSystem.SYSTEM.exists(tracklist.exportPath.safeToRealPath())) {
            logger.info { "creating ${tracklist.exportPath}" }
            FileSystem.SYSTEM.createDirectories(
                tracklist.exportPath.safeToRealPath()
            )
        }
        val formatter = loadFormatter(
            dir = templateFolder,
            defaultTemplate = defaultTemplate,
            templateKey = templateKey
        )
        val encodedSongs =
            json.encodeToJsonElement(ListSerializer(elementSerializer = serializer), value = tracklist.tracks)
                .jsonArray.toList().map {
                    it.jsonObject
                }
//        val jsonString = json.encodeToString(ListSerializer(elementSerializer = serializer), value = tracklist.tracks)
//        logger.info { jsonString }
//        val encodedSongs = json.decodeFromString(
//            ListSerializer(JsonObject.serializer()),
//            jsonString
//        )
        //        val encodedSongs = .json.encodeToJsonElement(ListSerializer(serializer), songs)
//            .jsonArray
        val txt = encodedSongs
            .joinToString("\n") {
                formatter(it)
            }
        val txtPath = tracklist.exportPath.safeToRealPath() / "${tracklist.title}.txt"
        logger.info { "writing to ${TextColors.blue(txtPath.pathString)}" }
        FileSystem.SYSTEM.write(txtPath) {
            writeUtf8(txt)
        }
//        val debugPath = ".out".toPath()
//        FileSystem.SYSTEM.createDirectories(debugPath)
//        val jsonPath = debugPath / ("${basename}.json").toPath()
//        logger.info { "writing to $jsonPath" }
//        FileSystem.SYSTEM.write(jsonPath) {
//            writeUtf8(jsonString)
//        }
//        val keys = encodedSongs.first().keys
//        val values = encodedSongs.map {
//            it.entries.associate {
//                it.key to it.value.jsonPrimitive.contentOrNull
//            }
//        }
//        val widths = keys.associateWith { key ->
//            max(
//                key.length,
//            values.maxOf { it[key]?.length ?: 0 }
//            )
//        }

//        val md =
//            keys.joinToString(" | ", "| ", " | \n") { it.padEnd(widths[it] ?: 0) } +
//            keys.joinToString("-|-", "|-", "-| \n") { "-".repeat(widths[it] ?: 0) } +
//            values.joinToString("\n") { obj ->
//                obj.entries.joinToString(" | ","| ", " |") { (key, value) ->
//                    value.orEmpty().padEnd(widths[key] ?: 0)
//                }
//            }
//
//        val mdPath = "${basename}.md".toPath()
//        logger.info { "writing to $mdPath" }
//        FileSystem.SYSTEM.write(mdPath) {
//            writeUtf8(md)
//        }

        val csv = CSVFormat {
            includeHeader = true
            separator = ','
            lineSeparator = "\n"
            numberFormat = CSVFormat.NumberFormat.Dot
//            alwaysEmitQuotes = true
        }.encodeToString(
            ListSerializer(serializer), tracklist.tracks
        )

        val csvPath = tracklist.exportPath.safeToRealPath() / "${tracklist.title}.csv"
        logger.info { "writing to ${TextColors.blue(csvPath.pathString)}" }
        FileSystem.SYSTEM.write(csvPath) {
            writeUtf8(csv)
        }
        println()
    }


    fun <E> write(
        tracklists: List<Tracklist<E>>,
        serializer: KSerializer<E>,
        openFolders: List<Path> = tracklists.map { it.exportPath },
        defaultTemplate: String = default,
        templateKey: String = "template",
    ) {
        tracklists.forEach { trackList ->
            write(
                tracklist = trackList,
                serializer = serializer,
                defaultTemplate = defaultTemplate,
                templateKey = templateKey
            )
        }
        openFolders
            .distinct()
            .forEach { path ->
                openFolder(path)
            }
    }
    fun <E> write(
        tracklists: List<Tracklist<E>>,
        serializer: KSerializer<E>,
        templateFolder: Path,
        openFolders: List<Path> = tracklists.map { it.exportPath },
        defaultTemplate: String = default,
        templateKey: String = "template",
    ) {
        tracklists.forEach { trackList ->
            write(
                tracklist = trackList,
                serializer = serializer,
                templateFolder = templateFolder,
                defaultTemplate = defaultTemplate,
                templateKey = templateKey
            )
        }
        openFolders
            .distinct()
            .forEach { path ->
                openFolder(path)
            }
    }

//    suspend fun <E> write(
//        tracklists: List<Tracklist<E>>,
//        serializer: KSerializer<E>,
//        openFolders: List<Path> = tracklists.map { it.exportPath },
//
//    ) {
//        tracklists.forEach { trackList ->
//            write(
//                tracklist = trackList,
//                serializer = serializer
////                formatter = formatter,
//            )
//        }
//        openFolders
//            .distinct()
//            .forEach { path ->
//                openFolder(path)
//            }
//    }
}

