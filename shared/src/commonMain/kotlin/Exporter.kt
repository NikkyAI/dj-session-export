@file:OptIn(ExperimentalSerializationApi::class)

import app.softwork.serialization.csv.CSVFormat
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import com.saveourtool.okio.safeToRealPath
//import korlibs.template.KorteAutoEscapeMode
//import korlibs.template.KorteTemplate
//import korlibs.template.KorteTemplateConfig
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

fun openFolder(path: Path) {
    println("opening $path")
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
        println(e.message)
    }
}

object Exporter {
    val default = """
        {time} {artist} - {title}
    """.trimIndent().trim()

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
//            println("tracklist ${tracklist.title} was empty")
//            return
//        }
//        println("writing ${tracklist.title}")
//        if (!FileSystem.SYSTEM.exists(tracklist.exportPath.safeToRealPath())) {
//            println("creating ${tracklist.exportPath}")
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
//        println("writing to $txtPath")
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
            println("tracklist ${tracklist.title} was empty")
            return
        }
        println("writing ${tracklist.title}")
        if (!FileSystem.SYSTEM.exists(tracklist.exportPath.safeToRealPath())) {
            println("creating ${tracklist.exportPath}")
            FileSystem.SYSTEM.createDirectories(
                tracklist.exportPath.safeToRealPath()
            )
        }
//        tracklist.tracks.forEach {
//            println(it)
//        }
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
//        println(jsonString)
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
        println("writing to $txtPath")
        FileSystem.SYSTEM.write(txtPath) {
            writeUtf8(txt)
        }
//        val debugPath = ".out".toPath()
//        FileSystem.SYSTEM.createDirectories(debugPath)
//        val jsonPath = debugPath / ("${basename}.json").toPath()
//        println("writing to $jsonPath")
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
//        println("writing to $mdPath")
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
        println("writing to $csvPath")
        FileSystem.SYSTEM.write(csvPath) {
            writeUtf8(csv)
        }
        println("\n")
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

