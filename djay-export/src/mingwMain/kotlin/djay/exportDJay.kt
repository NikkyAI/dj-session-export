@file:OptIn(ExperimentalTime::class)

package djay

import com.github.ajalt.mordant.rendering.TextColors.*
import com.saveourtool.okio.pathString
import com.saveourtool.okio.safeToRealPath
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite
import io.ktor.utils.io.core.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.coroutineScope
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.use
import useWith
import kotlin.time.ExperimentalTime

val dbPath = Folders.userMusic / "djay" / "djay Media Library" / "MediaLibrary.db"

fun canExportDJay(): Boolean {
    return FileSystem.SYSTEM.exists(dbPath)
}

data class Database2Row(
    val rowId: Int,
    val collection: String,
    val key: String,
    val data: String,
)

@OptIn(ExperimentalForeignApi::class)
suspend fun exportDJay() {
    val logger = KotlinLogging.logger("exportDJay.kt")

    logger.info { "opening ${blue(dbPath.pathString)}" }

    /**
     * The following urls are supported:
     * `sqlite::memory:`            | Open an in-memory database.
     * `sqlite:data.db`             | Open the file `data.db` in the current directory.
     * `sqlite://data.db`           | Open the file `data.db` in the current directory.
     * `sqlite:///data.db`          | Open the file `data.db` from the root (`/`) directory.
     * `sqlite://data.db?mode=ro`   | Open the file `data.db` for read-only access.
     */
    val db = SQLite(
        url = "sqlite://$dbPath",
//        url = "sqlite://$dbPath?mode=ro",
    )

    try {
        val database2 = db.fetchAll(
            """
                SELECT 
                d.rowId as rowId,
                d.collection AS collection,
                d.key AS key,
                hex(d.data) AS data
                FROM database2 AS d;
            """.trimIndent()
        ).map {
            it.metadata.let { metadata ->
                repeat(metadata.getColumnCount()) { i ->
                    println(
                        metadata.getColumn(i)
                    )
                }
            }
            it.rows.map { row ->
                Database2Row(
                    row.get("rowId").asInt(),
                    row.get("collection").asString(),
                    row.get("key").asString(),
                    row.get("data").asString(),
                )
            }
        }.getOrThrow()

        // "TSAF" 00
        // 03 00 (�)
        // 03 00 (�)
        // 00 00
        // 00 00
        // 00 00
        // 13 00 (�)
        // 00 00
        // 2B (+)
        // 08 "ADCHistorySessionItem" 00
        // 08 var historySessionItem 00
        // 08 "uuid" 00
        // 08 var uuid 00
        // 08 "sessionUUID" 00
        // 2B (+)
        // 08 var sessionUUID 00
        // 08 "ADCMediaItemTitleID" 00
        // 08 var mediaItemTitleID 00

        // 05 02 // NOTE: section marker ?
        // 08 var songTitle 00
        // 08 "title" 00
        // 08 var artistName 00
        // 08 "artist" 00
        // 14 var duration // NOTE: 13 bytes ?
        //     14 00 00 00 00 00 00 00 00 80 95 0B 73 40
        //     14 00 00 00 00 00 1A 73 40
        // 08 "duration" 00
        // 08 var isrc 00
        // 08 "isrc" 00 00
        // 08 "titleID" 00
        // 14 var titleId // NOTE: 13 bytes ?
        //      14 00 00 00 00 00 00 00 00 00 00 00 00 00 F0 3F
        //      14 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40
        // 08 "deckNumber" 00
        // 30 var ??? 41 (14 unknown bytes, start: 30 00 00 00, end: 41)
        //      30 00 00 00 F8 53 B3 BB E3 15 C7 41
        //      30 00 00 00 19 04 A6 D6 A9 D2 C5 41
        //      30 00 00 00 0E 2D 02 29 44 1E C7 41
        // 08 "startTime" 00
        // 08 var originSourceID 00
        // 08 "originSourceID" 00
        //     "apple-music"
        //     "beatport"
        // 00 (File terminator??)

        fun BufferedSource.expectBytes(bytes: ByteArray): Long {
            val b = readByteArray(bytes.size.toLong())
            require(b.toList() == bytes.toList()) {
                "${b.toHexString()} should have been ${bytes.toHexString()} data: ${
                    peek().readByteArray().toHexString()
                }"
            }
            return bytes.size.toLong()
        }

        //        fun BufferedSource.expectBytes(byte: Byte) = expectBytes(byteArrayOf(byte))
        fun BufferedSource.expectBytes(vararg byte: Byte) = expectBytes(byteArrayOf(*byte))
//        {
//            val b = readByte()
//            require(b == byte.toByte()) {
//                "${b.toHexString()} does not start with ${byte.toByte().toHexString()} data: ${
//                    peek().readByteArray().toHexString()
//                }"
//            }
//        }


        fun BufferedSource.expectString(value: String): Long {
            val bytes = byteArrayOf(0x08, *value.toByteArray(), 0x0)
            val b = readByteArray(bytes.size.toLong())
            require(b.toList() == bytes.toList()) {
                "${b.toHexString()} should have been ${bytes.toHexString()} ($value) data: ${
                    peek().readByteArray().toHexString()
                }"
            }
            return b.size.toLong()
        }

        fun BufferedSource.parseString(): Pair<String, Long> {
            val list = mutableListOf<Byte>()
            var byteCount = 0L
//            peek().useWith {
            expectBytes(0x08)
            byteCount++
            while (true) {
                val nextByte = readByte()
                byteCount++
                if (nextByte == 0x00.toByte()) {
                    break
                } else {
                    list += nextByte
                }
            }
//            }
//            skip(byteCount)

            return list.toByteArray().decodeToString() to byteCount
        }

        fun BufferedSource.readUntil(stopAt: ByteArray): ByteArray {
            val list = mutableListOf<Byte>()
            var listHex = ""
            val stopAtHex = stopAt.toHexString()

            peek().useWith {
                while (true) {
                    val nextByte = readByte()
                    list += nextByte
                    listHex += nextByte.toHexString()
                    if (listHex.endsWith(stopAtHex)) break
                }
            }
            val bytes = list.dropLast(stopAt.size).toByteArray()
            skip(bytes.size.toLong())
            return bytes
        }

        fun List<Byte>.toLong(): Long {
            val byteArray = this
            var result = 0L
            for (i in byteArray.indices) {
                result = result or (byteArray[i].toLong() and 0xFF shl (8 * (byteArray.size - 1 - i)))
            }
            return result
        }

//        database2
//            .map { row ->
//                row.collection
//            }
//            .distinct()
//            .forEach { collection ->
//                val folder = collection.toPath()
//                FileSystem.SYSTEM.createDirectories(folder)
//            }
//
//        database2
//            .forEach { row ->
//                val folder = row.collection.toPath()
//                val path = folder / "${row.key}.hex"
//                println("writing bytes to $path")
//                FileSystem.SYSTEM.write(path) {
//                    write(row.data.hexToByteArray())
//                }
//            }

        val CWD = ".".toPath().safeToRealPath()

        coroutineScope {
            val historySessions = database2
                .filter { it.collection == "historySessions" }
                .map { row ->
//                    async {
                    println()
                    logger.info { "rowId: ${row.rowId}" }
                    logger.info { "key: ${row.key}" }

                    val folder = row.collection.toPath()
                    val path = folder / "${row.key}.hex"

                    logger.info { CWD / path }

                    Buffer().use { buf ->
                        buf.write(row.data.hexToByteArray())
                        buf.flush()
                        with(buf) {
                            expectBytes(
                                *"TSAF".encodeToByteArray(),
                                0x03, 0x00,
                                0x03, 0x00,
                                0x04, 0x00,
                                0x00, 0x00,
                                0x00, 0x00,
                                0x00, 0x00,
                            )
                            val mysteryByte = readByte()
                            expectBytes(0x00, 0x00, 0x00)
//                        readUntil(byteArrayOf(0x2B))
                            expectBytes(0x2B)
                            expectString("ADCHistorySession")
                            val (uuid, _) = parseString()
                            logger.info { "uuid: $uuid" }
                            expectString("uuid")
                            val (deviceName, _) = parseString()
                            logger.info { "deviceName: $deviceName" }
                            expectString("deviceName")

                            val deviceTypeBytes: ByteArray
//                        var deviceTypeBytes: ByteArray? = null

                            peek().useWith {
//                            try {
                                val l = expectBytes(0x14)
                                val stringLiteral = byteArrayOf(0x08, *"deviceType".encodeToByteArray(), 0x00)
                                val bytes = readUntil(stringLiteral)
                                val staticLength = expectBytes(stringLiteral)
                                deviceTypeBytes = bytes

                                l + bytes.size + staticLength
//                            } catch (e: IllegalArgumentException) {
//                                logger.warn(e) { "failed to parse deviceType" }
//                                0
//                            }
                            }.let { skip ->
                                skip(skip)
                            }

                            logger.info { "deviceTypeBytes: ${deviceTypeBytes?.size} bytes" }


                            val startDateBytes: ByteArray
//                        var startDateBytes: ByteArray? = null

                            peek().useWith {
//                            try {
                                val l = expectBytes(0x30)
                                val stringLiteral = byteArrayOf(0x08, *"startDate".encodeToByteArray(), 0x00)
                                val bytes = readUntil(stringLiteral)
                                val staticLength = expectBytes(stringLiteral)
                                startDateBytes = bytes

                                l + bytes.size + staticLength
//                            } catch (e: IllegalArgumentException) {
//                                logger.warn(e) { "failed to parse startDate" }
//                                0
//                            }
                            }.let { skip ->
                                skip(skip)
                            }

                            logger.info { "startDateBytes: ${startDateBytes?.size} bytes" }

                            val endDateBytes: ByteArray
//                        var endDateBytes: ByteArray? = null

                            peek().useWith {
//                            try {
                                val l = expectBytes(0x30)
                                val stringLiteral = byteArrayOf(0x08, *"endDate".encodeToByteArray(), 0x00)
                                val bytes = readUntil(stringLiteral)
                                val staticLength = expectBytes(stringLiteral)
                                endDateBytes = bytes

                                l + bytes.size + staticLength
//                            } catch (e: IllegalArgumentException) {
//                                logger.warn(e) { "failed to parse endDate" }
//                                0
//                            }
                            }.let { skip ->
                                skip(skip)
                            }

                            logger.info { "endDateBytes: ${endDateBytes?.size} bytes" }

                            expectBytes(
                                0x1B, 0x00, 0x00,
                            )
                            val countByte = readByte().toUInt()
                            logger.info { "count: $countByte" }
                            expectBytes(
                                0x00, 0x00, 0x00
                            )

                            val itemUUIDs = mutableListOf<String>()
//                        val list = List(countByte.toInt()) {
//                            parseString().first
//                        }
                            while (true) {
                                val nextString = parseString().first
                                if (nextString == "itemUUIDs") break
                                itemUUIDs += nextString
                            }
                            logger.info { "found ${itemUUIDs.size} item UUIDs" }
//                        expectString("itemUUIDs")

                            HistorySession(
                                mysteryByte = mysteryByte,
                                uuid = uuid,
                                deviceName = deviceName,
                                deviceType = deviceTypeBytes.toHexString(),
                                startDate = startDateBytes.toHexString(),
                                endDate = endDateBytes.toHexString(),
                                itemUUIDs = itemUUIDs,
                            )
                        }
                    }
                }
//                }.awaitAll()
                .associateBy { it.uuid }

            val historySessionItems = database2
                .filter { it.collection == "historySessionItems" }
                .map { row ->
//                    async {
                    println()
                    logger.info { "rowId: ${row.rowId}" }
                    logger.info { "key: ${row.key}" }
//                println()
//                logger.info { "data: ${row.data}" }
//                logger.info { "data: ${row.data.hexToByteArray().decodeToString()}" }

                    val folder = row.collection.toPath()
                    val path = folder / "${row.key}.hex"

                    logger.info { CWD / path }

//                val header = row.data.substringBefore("0502")
//                logger.info { "before: ${header.length / 2} bytes" }

                    Buffer().useWith buf@{
                        var pos = 0L
                        val totalBytes = row.data.length.toLong()
                        write(row.data.hexToByteArray())
                        flush()
                        pos += expectBytes(
                            *"TSAF".encodeToByteArray(),
                            0x03, 0x00,
                            0x03, 0x00,
                            0x03, 0x00,
                            0x00, 0x00,
                            0x00, 0x00,
                            0x00, 0x00,
                        )
                        val mysteryByte = readByte()
                        pos++
                        pos += expectBytes(0x00, 0x00, 0x00)
//                        readUntil(byteArrayOf(0x2B))
                        pos += expectBytes(0x2B)
                        pos += expectString("ADCHistorySessionItem")
                        val uuid = parseString().let {
                            pos += it.second
                            it.first
                        }
                        pos += expectString("uuid")
                        logger.info { "uuid: $uuid" }

                        var sessionUUID: String? = null
                        peek().useWith {
                            try {
                                val (str, strLength) = parseString()
                                val staticLength = expectString("sessionUUID")
                                sessionUUID = str

                                strLength + staticLength
                            } catch (e: IllegalArgumentException) {
                                logger.warn(e) { "failed to parse sessionUUID" }
                                0
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
                        logger.info { "sessionUUID: $sessionUUID" }

                        pos += expectBytes(0x2B)
                        pos += expectString("ADCMediaItemTitleID")
                        val (ADCMediaItemTitleID, _) = parseString()

//                        val magicHeader = readUntil(byteArrayOf(0x05, 0x02))

                        pos += expectBytes(byteArrayOf(0x05, 0x02))
                        val title = parseString().let {
                            pos += it.second
                            it.first
                        }
                        logger.info { "title: $title" }
                        pos += expectString("title")
                        var artist: String? = null

                        peek().useWith {
                            try {
                                val (str, strLength) = parseString()
                                val staticLength = expectString("artist")
                                artist = str

                                strLength + staticLength
                            } catch (e: IllegalArgumentException) {
                                try {
                                    this@buf.peek().useWith {
                                        val length = expectBytes(0x05, 0x07)
                                        val staticLength = expectString("artist")
                                        length + staticLength
                                    }
                                } catch (e: IllegalArgumentException) {
                                    logger.warn(e) { "failed to parse artist" }
                                    0
                                }
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
                        logger.info { "artist: $artist" }


                        var durationBytes: ByteArray? = null

                        peek().useWith {
                            try {
                                val l = expectBytes(0x14)
                                val pos = totalBytes - size
                                logger.warn { "pos: $pos ${pos.toByte().toHexString(HexFormat.UpperCase)}" }
//                                val padding = run {
                                val padding = 8 - (pos % 8)
                                logger.warn { "padding: $padding" }
//                                    repeat(padding.toInt()) { p ->
//                                        expectBytes(0x00)
//                                    }
//                                    padding
//                                }
                                val stringLiteral = byteArrayOf(0x08, *"duration".encodeToByteArray(), 0x00)
                                val bytes = readUntil(stringLiteral)
                                val staticLength = expectString("duration")

                                val zeroBytes = bytes
                                    .takeWhile { it == 0x00.toByte() }
                                    .size
                                    .let { zeroBytes ->
                                        if (padding > zeroBytes) {
                                            logger.error { "incorrect padding in ${bytes.toHexString()}" }
                                            0
                                        } else {
                                            logger.warn { "zeros: $zeroBytes" }
                                            zeroBytes
                                        }
                                    }

                                durationBytes = bytes
                                    .drop(zeroBytes)
//                                    .dropWhile { it == 0x00.toByte() }
                                    .toByteArray()

                                l + bytes.size + staticLength
                            } catch (e: IllegalArgumentException) {
                                logger.warn(e) { "failed to parse duration" }
                                0
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
//                        val duration = durationBytes?.dropLast(1)?.toLong()?.toULong()

                        logger.info { "durationBytes: ${durationBytes?.size} bytes" }

                        var isrc: String? = null

                        peek().useWith {
                            try {
                                val (str, strLength) = parseString()
                                val staticLength = expectString("isrc")
                                isrc = str

                                strLength + staticLength
                            } catch (e: IllegalArgumentException) {
                                logger.warn(e) { "failed to parse isrc" }
                                0
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
                        logger.info { "isrc: $isrc" }

                        peek().useWith {
                            try {
                                val l = expectBytes(byteArrayOf(0x00))
                                val staticLength = expectString("titleID")

                                l + staticLength
                            } catch (e: IllegalArgumentException) {
                                0
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }

                        val deckNumberBytes: ByteArray
//                        var deckNumberBytes: ByteArray? = null

                        peek().useWith {
//                            try {
                            val l = expectBytes(0x14)
                            val bytes = readUntil(
                                byteArrayOf(0x08) + "deckNumber".encodeToByteArray() + byteArrayOf(0x00)
                            )
                            val staticLength = expectString("deckNumber")
                            deckNumberBytes = bytes

                            l + bytes.size + staticLength
//                            } catch (e: IllegalArgumentException) {
//                                logger.warn(e) { "failed to parse deckNumber" }
//                                0
//                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
                        val deckNumber = deckNumberBytes.dropLast(1).toLong()
                        logger.info { "deckNumber: $deckNumber" }


                        var startTimeBytes: ByteArray? = null

                        peek().useWith {
                            try {
                                val l = expectBytes(0x30)
                                val stringLiteral =
                                    byteArrayOf(0x08) + "startTime".encodeToByteArray() + byteArrayOf(0x00)
                                val bytes = readUntil(
                                    stringLiteral
                                )
                                val staticLength = expectBytes(stringLiteral)
                                startTimeBytes = bytes

                                l + bytes.size + staticLength
                            } catch (e: IllegalArgumentException) {
                                logger.warn(e) { "failed to parse startTime" }
                                0
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
//                        val startTime = startTimeBytes?.dropLast(1)?.toLong() // ?.toULong()

                        var originSourceID: String? = null

                        peek().useWith {
                            try {
                                val (str, strLength) = parseString()
                                val staticLength = expectString("originSourceID")
                                originSourceID = str

                                strLength + staticLength
                            } catch (e: IllegalArgumentException) {
                                logger.warn(e) { "failed to parse originSourceID" }
                                0
                            }
                        }.let { skip ->
                            skip(skip)
                            pos += skip
                        }
                        logger.info { "isrc: $isrc" }

                        HistorySessionItem(
                            mysteryByte = mysteryByte,
                            uuid = uuid,
                            sessionUUID = sessionUUID,
//                            rowId = row.rowId,
//                            key = row.key,
                            title = title,
                            artist = artist,
                            duration = durationBytes?.toHexString(),
                            isrc = isrc,
                            deckNumber = deckNumber,
                            startTime = startTimeBytes?.toHexString(),
                            originSourceID = originSourceID,
                        )
                    }
                }
//                }.awaitAll()
                .associateBy { it.uuid }

//        historySessionItems.forEach {
//            logger.info { it }
//        }

//        historySessionItems.mapNotNull { it.duration }
//            .map { it.takeLast(16) }
////            .map { it.chunked(2).reversed().joinToString("") }
////            .sortedBy { it.reversed() }
//            .sorted()
////            .map { it.reversed() }
//            .forEach { hex ->
////                val trimmed = it.trim('0').length
//
//                val byteArray = hex.hexToByteArray().dropLast(1)
//                var result = 0L
//
//                for (i in byteArray.indices) {
//                    result = result or (byteArray[i].toLong() and 0xFF shl (8 * (byteArray.size - 1 - i)))
//                }
//                logger.info { "duration: $hex $result" }
//            }
//            val maxDuration = historySessionItems.values.mapNotNull { it.duration }
//                .maxBy {
//                    it.chunked(2).dropWhile { it == "00" }.size
//                }
//            val maxStartTime = historySessionItems.values.mapNotNull { it.startTime }
//                .maxBy {
//                    it.chunked(2).dropWhile { it == "00" }.size
//                }
//        historySessionItems.mapNotNull { it.startTime }
//            .forEach {
//                logger.info { "startTime: $it" }
//            }
//
//        historySessionItems.values.filter { it.sessionUUID == null }.forEach {
//            logger.info { it }
//        }
//        historySessionItems.values.filter { it.sessionUUID != null }.groupBy { it.sessionUUID }
//            .forEach { (sessionUUID, sessionItems) ->
//                logger.info { "session $sessionUUID: ${sessionItems.size}" }
//            }
//
//        logger.info { "history session items: ${historySessionItems.size}" }
//        logger.info { "maxDurationBytes: $maxDuration" }
//        logger.info { "maxStartTimeBytes: $maxStartTime" }

//            historySessions.forEach { (uuid, session) ->
//                logger.info { "SESSION ${brightRed(uuid)} on ${brightMagenta(session.deviceName)}" }
//                val items = session.itemUUIDs.map {
//                    historySessionItems[it] ?: error("item uuid $it of ${session.uuid} was not found")
//                }
//
//                items.forEach { item ->
////                    logger.info { "(deck: ${item.deckNumber})" }
//                    logger.info { "title: ${brightWhite(item.title)} by ${brightCyan(item.artist ?: "???")} " }
//                    logger.info { "startTime: ${item.startTime}" }
//                    logger.info { "duration: ${item.duration}" }
//                }
//            }

//            historySessions.values.forEach {  session ->
//                logger.info { "SESSION ${brightRed(session.uuid)} on ${brightMagenta(session.deviceName)}" }
//                val items = session.itemUUIDs.map {
//                    historySessionItems[it] ?: error("item uuid $it of ${session.uuid} was not found")
//                }
//                items
////                    .mapNotNull { it.startTime }
//                    .forEach { item ->
//                        val timestamp =
//                            item.startTime
//                                    ?.hexToByteArray()
//                                    ?.dropLast(1)
//                                    ?.toLong()
//
//                            logger.info { "startTime: ${item.startTime} $timestamp" }
//                    }
//            }

            historySessions.values.forEach { session ->
                logger.info { "SESSION ${brightRed(session.uuid)} on ${brightMagenta(session.deviceName)}" }
                val items = session.itemUUIDs.map {
                    historySessionItems[it] ?: error("item uuid $it of ${session.uuid} was not found")
                }
                items
//                    .mapNotNull { it.duration }
                    .forEach { item ->
//                        val timestamp =
//                            item.duration
//                                    ?.hexToByteArray()
//                                    ?.dropLast(1)
//                                    ?.toLong()

                        val number = item.duration
                            ?.dropLast(2)?.takeUnless { it.isBlank() }
                            ?.toByteArray()?.toList()?.toLong()

                        logger.info { "${gray(item.uuid)} duration: $number ${item.duration}" }
                    }
            }

//            historySessionItems.values.mapNotNull { it.startTime }
//                .distinct()
//                .sorted()
//                .also { times ->
//                    val difference = times
//                        .map {
//                            it
//                                .hexToByteArray()
//                                .dropLast(1)
//                                .toLong() to it
//                        }
//                        .zipWithNext { (a, aHex), (b, bHex) ->
//
//                            a to b - a
//                        }
//                    difference.forEach { (startTime, diff) ->
//                        logger.info { "startTime: $startTime $diff" }
//
////                            val a = startTime % 256UL
////                            val b = startTime / 256UL % 256UL
////                            val c = startTime / 256UL/ 256UL % 256UL
////                            logger.info { "startTime: $a $b $c" }
//                    }
//                }


        }

    } catch (e: Exception) {
        logger.error(e) { "error while reading database: ${e.message}" }
    } finally {
        logger.info { "closing ${blue(dbPath.pathString)}" }
        db.close()
    }
}
