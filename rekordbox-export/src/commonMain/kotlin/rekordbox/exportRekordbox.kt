@file:OptIn(ExperimentalTime::class)

package rekordbox

import Tracklist
import com.github.ajalt.mordant.rendering.TextColors.*
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import com.saveourtool.okio.pathString
import genreBreakdown
import getExportFolder
import httpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.impl.extensions.asIntOrNull
import io.github.smyrgeorge.sqlx4k.impl.extensions.asLong
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.io.okio.asKotlinxIoRawSink
import okio.FileSystem
import okio.SYSTEM
import splitTracklists
import sqliteDB
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val httpClient = httpClient()

val FS = FileSystem.SYSTEM

val encryptedPath = Folders.APPDATA / "Pioneer" / "rekordbox" / "master.db"

fun canExportRekordbox(): Boolean {
    return FileSystem.SYSTEM.exists(encryptedPath)
}

suspend fun exportRekordbox() {
    val logger = KotlinLogging.logger("exportRekordbox.kt")

    val sqlCipherPath = Folders.TEMP / "sqlcipher.exe"


    logger.info { sqlCipherPath }

    // downloading sqlcipher
    run {
        if (FS.exists(sqlCipherPath)) {
            logger.info { "sqlcipher already downloaded" }
            return@run
        }
        logger.info { "downloading sqlcipher" }

        httpClient.prepareGet(urlString = "https://github.com/Katecca/sqlcipher-static-binary/raw/refs/heads/master/windows/x86_64/sqlcipher.exe")
            .execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                var count = 0L
                FS.createDirectories(sqlCipherPath.parent!!, mustCreate = false)
                FS.write(sqlCipherPath, mustCreate = false) {
                    val rawSink = asKotlinxIoRawSink()
                    while (!channel.exhausted()) {
                        val chunk = channel.readRemaining()
                        count += chunk.remaining

                        chunk.transferTo(rawSink)
                        logger.info { "Received $count bytes from ${httpResponse.contentLength()}" }
                    }
                }
            }
        logger.info { "downloaded sqlcipher" }
    }

    val dbPath = Folders.TEMP / "rekordbox.sqlite"
    FS.delete(dbPath, mustExist = false)
    dbPath.parent?.let {
        FileSystem.SYSTEM.createDirectories(it)
    }

    // decoding master.db
    run {

        val sqlLines = """
            PRAGMA key='402fd482c38817c35ffa8ffb8c7d93143b749e7d315df7a81732a1ff43608497';
            ATTACH DATABASE '$dbPath' AS plaintext KEY '';
            SELECT sqlcipher_export('plaintext');
            DETACH DATABASE plaintext;
        """.trimIndent()
            .lines()
            .filter { it.isNotBlank() }
        val sql = sqlLines
            .joinToString(" ")

        Command(
            sqlCipherPath.toString()
        )
            .args(
                encryptedPath.toString(),
                sql
            )
            .also {
                logger.info { it.debugString() }
            }
//            .stdin(Stdio.Pipe)
            .stdout(Stdio.Inherit)
            .spawn()
//            .apply {
//                bufferedStdin()?.let { writer ->
//                    sqlLines.forEach {
//                        logger.info { it }
//                        writer.writeLine(it)
//                    }
//                }
//            }
            .wait()
    }

    logger.info { "opening ${blue(dbPath.pathString)}" }

    /**
     * The following urls are supported:
     * `sqlite::memory:`            | Open an in-memory database.
     * `sqlite:data.db`             | Open the file `data.db` in the current directory.
     * `sqlite://data.db`           | Open the file `data.db` in the current directory.
     * `sqlite:///data.db`          | Open the file `data.db` from the root (`/`) directory.
     * `sqlite://data.db?mode=ro`   | Open the file `data.db` for read-only access.
     */
    val db = sqliteDB(
        url = "sqlite://$dbPath",
//        url = "sqlite://$dbPath?mode=ro",
    )

    try {
        logger.info { "getting tracklists" }
        val tracklists = db.fetchAll(
            """
                SELECT h.Name        AS HistoryName,
                       sh.TrackNo    AS TrackNo,
                       unixepoch(sh.updated_at) AS start,
                       c.Title       AS Title,
                       c.ReleaseYear AS year,
                       c.Length      AS Length,
                       c.BPM         AS BPM,
                       a.Name        AS Artist,
                       al.Name       AS Album,
                       g.Name        AS Genre,
                       l.Name        As Label,
                       k.ScaleName   AS ScaleName
                FROM djmdHistory h
                         JOIN
                     djmdSongHistory sh ON h.id = sh.HistoryID
                         LEFT JOIN
                     djmdContent c ON sh.ContentID = c.ID
                         LEFT JOIN
                     djmdArtist a ON c.ArtistID = a.ID
                         LEFT JOIN
                     djmdAlbum al ON c.AlbumID = al.ID
                         LEFT JOIN
                     djmdGenre g ON c.GenreID = g.ID
                         LEFT JOIN
                     djmdLabel l ON c.LabelID = l.ID
                         LEFT JOIN
                     djmdKey k ON c.KeyID = k.ID
                -- GROUP BY sh.HistoryID, sh.TrackNo
                ORDER BY sh.HistoryID, sh.TrackNo ASC;
            """.trimIndent()
        )
            .map {
                it.rows.groupBy {
                    it.get("HistoryName").asString()
                }.map { (historyName, rows) ->
                    val referenceTimestamp = rows.first().get("start").asLong().let {
                        Instant.fromEpochSeconds(it)
                    }
                    Tracklist(
                        title = historyName,
                        exportPath = getExportFolder() / "Rekordbox",
                        tracks = rows.map { row ->
                            val timestamp = row.get("start").asLong().let {
                                Instant.fromEpochSeconds(it)
                            }
                            Song(
                                position = row.get("TrackNo").asInt(),
                                time = (timestamp - referenceTimestamp),
                                startAt = timestamp,
//                                duration = row.get("Length").asInt().seconds,
                                title = row.get("Title").asString(),
                                artist = row.get("Artist").asStringOrNull(),
                                label = row.get("Label").asStringOrNull(),
                                album = row.get("Album").asStringOrNull(),
                                genre = row.get("Genre").asStringOrNull(),
                                year = row.get("year").asIntOrNull(),
                                bpm = row.get("BPM").asInt() / 100.0f,
                                scale = row.get("ScaleName").asStringOrNull(),
                            )
                        }
                    )
                }
            }.getOrThrow()

        tracklists.flatMap { tracklist ->
            genreBreakdown(tracklist) { genre }
            tracklist.splitTracklists(
                { it.time },
                { track, diff ->
                    track.copy(
                        time = track.time - diff
                    )
                },
            ) { lastTrack, nextTrack -> nextTrack.time - lastTrack.time }
        }.map { tracklist->
            tracklist.copy(
                tracks=tracklist.tracks.mapIndexed { i, track ->
                    track.copy(
                        position = i+1,
                    )
                }
            )
        }.let { tracklists ->
            Exporter.write(
                tracklists,
                Song.serializer(),
            )
        }
    } finally {
        logger.info { "closing ${blue(dbPath.pathString)}" }
        db.close()
    }
}
