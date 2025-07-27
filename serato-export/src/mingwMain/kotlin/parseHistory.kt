import com.fleeksoft.charset.Charsets
import com.fleeksoft.io.ByteBuffer
import com.fleeksoft.io.ByteBufferFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import platform.posix.getenv
import kotlin.time.Instant

fun parseChunk(buffer: ByteBuffer, index: Int): Pair<Chunk, Int> {
    val tag = run {
        val arr = ByteArray(4)
        buffer
            .position(index)
            .get(arr, 0, 4)
//        println(arr.toHexString()
        arr.map {
            it.toInt().toChar()
        }.joinToString("")
    }
    val length = buffer.getInt(index + 4)
    println("length: $length")
    val data: Chunk = when (tag) {
        "oses", "oent", "otrk", "adat" -> {
            Chunk.Chunks(
                length = length,
                tag = tag,
                data = parseChunkArray(buffer, index + 8, index + 8 + length)
            )
        }

        "\u0000\u0000\u0000\u0001", "\u0000\u0000\u0000\u000f" -> {
            Chunk.IntData(
                length = length,
                tag = tag,
                data = buffer.getInt(index + 8)
            )
        }

        "\u0000\u0000\u00005" -> {
            val secondsSince1970 = buffer.getLong(index + 8)
            Chunk.Date(
                length = length,
                tag = tag,
                data = Instant.fromEpochSeconds(secondsSince1970)
            )
        }

        else -> {
            val bytes = ByteArray(length)
            buffer.position(index + 8)
            buffer.get(bytes, 0, length)
            Chunk.StringData(
                length, tag,
                data = Charsets.ISO_8859_1.decode(ByteBufferFactory.wrap(bytes))
                    .toString()
                    .replace("\u0000", "")
            )
        }
    }
    return data to (index + length + 8)
}


fun parseChunkArray(buffer: ByteBuffer, start: Int, end: Int): List<Chunk> {
//    println(buffer.array().toHexString())
//    println("start: $start")
//    println("end: $end")
    val chunks = mutableListOf<Chunk>()
    var cursor = start
    while (cursor < end) {
//        println("cursor: $cursor")
        val (chunk, newIndex) = parseChunk(buffer, cursor)
        cursor = newIndex
        chunks.add(chunk)
    }
    return chunks
}

fun getSessions(path: Path): Map<Int, String> {
    println("getSessions")
    val sessions = mutableMapOf<Int, String>()
    val buffer = FileSystem.SYSTEM.source(path).buffer().use {
        ByteBufferFactory.wrap(it.readByteArray())
    }
    val chunks = parseChunkArray(buffer, 0, buffer.limit())

    chunks.forEach { chunk ->
        if (chunk.tag == "oses" && chunk is Chunk.Chunks) {
            val adatChunk = chunk.data[0] as Chunk.Chunks
            if (adatChunk.tag == "adat") {
                var date = ""
                var index = -1
                adatChunk.data.forEach { subChunk ->
                    when (subChunk.tag) {
                        "\u0000\u0000\u0000\u0001" -> index = (subChunk as Chunk.IntData).data as Int
                        "\u0000\u0000\u0000)" -> date = (subChunk as Chunk.StringData).data
                    }
                }
                println("SESSION $date $index")
                sessions[index] = date
            }
        }
    }
    return sessions
}

fun getSessionSongs(path: Path): List<Session.HistorySong> {
    println("getSessionSongs")
    val buffer = FileSystem.SYSTEM.source(path).buffer().use {
        ByteBufferFactory.wrap(it.readByteArray())
    }
    val chunks = parseChunkArray(buffer, 0, buffer.limit())

    val songs = mutableListOf<Session.HistorySong>()

    chunks.forEach { chunk ->

        if (chunk is Chunk.Chunks && chunk.tag == "oent") {
            val adatChunk = chunk.data[0]
            if (adatChunk.tag == "adat" && adatChunk is Chunk.Chunks) {
                var title = ""
                var artist = ""
                var bpm: Int? = null
                var filePath = ""
                var timePlayed: Instant? = null

                (adatChunk.data).forEach { subChunk ->
                    when (subChunk.tag) {
                        "\u0000\u0000\u0000\u0006" -> title = (subChunk as Chunk.StringData).data
                        "\u0000\u0000\u0000\u0007" -> artist = (subChunk as Chunk.StringData).data
                        "\u0000\u0000\u0000\u000f" -> bpm = (subChunk as Chunk.IntData).data as Int
                        "pfil" -> filePath = (subChunk as Chunk.StringData).data
                        "\u0000\u0000\u00005" -> timePlayed = (subChunk as Chunk.Date).data
                    }
                }
                songs.add(
                    Session.HistorySong(
                        timePlayed = timePlayed!!,
                        title = title,
                        artist = artist,
                        filePath = filePath,
                        bpm = bpm
                    )
                )
            }
        }
    }
    return songs
}

fun getSeratoHistory(seratoPath: Path = defaultSeratoPath): List<Session> {
    val sessions = getSessions(seratoPath / "History/history.database")
    val result = mutableListOf<Session>()

    for ((sessionIndex, key) in sessions) {
        val songlist = getSessionSongs(seratoPath / "History/Sessions/$sessionIndex.session")
        result += Session(date = key, songs = songlist.sortedBy { it.timePlayed })
    }
    return result;
}

@OptIn(ExperimentalForeignApi::class)
val defaultSeratoPath: Path
    get() {
        val homepath = getenv("HOMEPATH")?.toKString() ?: error("failed to get %HOMEPATH%")
        return homepath.toPath() / "Music/_Serato_/"
    }

fun ByteBuffer.getInt(index: Int): Int {
    val byteArray = ByteArray(4)
//    println("remaining: ${remaining()}")
    position(index).get(byteArray, 0, 4)
    return byteArrayToInt(byteArray).also {
//        println("parsed int: $it")
    }
}

fun ByteBuffer.getLong(index: Int): Long {
    val byteArray = ByteArray(8)
//    println("remaining: ${remaining()}")
    position(index).get(byteArray, 0, 8)
    return byteArrayToLong(byteArray).also {
//        println("parsed int: $it")
    }
}

fun byteArrayToInt(byteArray: ByteArray): Int {
    var result = 0
    println(byteArray.toHexString())
    for (i in byteArray.indices) {
        result = result or (byteArray[i].toInt() and 0xFF shl (8 * (byteArray.size - 1 - i)))
    }
    return result
}
fun byteArrayToLong(byteArray: ByteArray): Long {
    var result = 0L
    println(byteArray.toHexString())
    for (i in byteArray.indices) {
        result = result or (byteArray[i].toLong() and 0xFF shl (8 * (byteArray.size - 1 - i)))
    }
    return result
}