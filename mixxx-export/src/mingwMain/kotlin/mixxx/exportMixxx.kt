@file:OptIn(ExperimentalTime::class)

package mixxx

import Tracklist
import com.github.ajalt.mordant.rendering.TextColors.*
import com.saveourtool.okio.pathString
import genreBreakdown
import getExportFolder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.impl.extensions.asFloatOrNull
import io.github.smyrgeorge.sqlx4k.impl.extensions.asInt
import io.github.smyrgeorge.sqlx4k.impl.extensions.asLong
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import splitTracklists
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val dbPath = Folders.LOCAL_APPDATA / "Mixxx" / "mixxxdb.sqlite"

fun canExportMixxx(): Boolean {
    return FileSystem.SYSTEM.exists(dbPath)
}

@OptIn(ExperimentalForeignApi::class)
suspend fun exportMixxx() {
    val logger = KotlinLogging.logger("exportMixxx.kt")

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
        url = "sqlite://$dbPath?mode=ro",
    )

    try {

//        db.execute("PRAGMA key = '402fd482c38817c35ffa8ffb8c7d93143b749e7d315df7a81732a1ff43608497'")
        val tracklists = db.fetchAll(
            """
                SELECT 
                       p.Name                          AS playlistName,
                       pt.position                     AS position,
                       unixepoch(pt.pl_datetime_added) AS start,
                       l.title                         AS title,
                       l.artist                        AS artist,
                       l.album                         AS album,
                       l.year                          AS year,
                       l.bpm                           AS bpm,
                       l.key                           AS key,
                       l.genre                         AS genre
                     FROM Playlists p
                         JOIN
                     PlaylistTracks pt ON p.id = pt.playlist_id
                         LEFT JOIN
                     library l ON pt.track_id = l.id
                WHERE p.locked = 0
                -- GROUP BY p.Name, pt.position, p.position
                ORDER BY p.Name, pt.position ASC, p.position ASC;
            """.trimIndent()
        ).map { it ->
            it.rows.groupBy {
                it.get("playlistName").asString()
            }.map { (playlistName, rows) ->
                val referenceTimestamp = rows.first().get("start").asLong().let {
                    Instant.fromEpochSeconds(it)
                }
                Tracklist(
                    title = playlistName
                        .replace("/", "_")
                        .replace("\\", "_")
                        .replace(";", "-")
                        .replace(":", "-"),
                    exportPath = getExportFolder() / "Mixxx",
                    tracks = rows.map { songRow ->
                        val timestamp = songRow.get("start").asLong().let {
                            Instant.fromEpochSeconds(it)
                        }
                        Song(
                            position = songRow.get("position").asInt(),
                            time = (timestamp - referenceTimestamp),
                            startAt = timestamp,
                            title = songRow.get("title").asString(),
                            artist = songRow.get("artist").asStringOrNull(),
                            album = songRow.get("album").asStringOrNull(),
                            year = songRow.get("year").asStringOrNull(),
                            bpm = songRow.get("bpm").asFloatOrNull()
                                ?.takeUnless { it == 0.0f },
                            key = songRow.get("key").asStringOrNull()
                                ?.takeUnless(String::isBlank),
                            genre = songRow.get("genre").asStringOrNull(),
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
                }
            ) { lastTrack, nextTrack -> nextTrack.time - lastTrack.time }
        }.let {tracklists ->
            Exporter.write(
                tracklists,
                Song.serializer(),
            )
//            genreBreakdown(tracklist) { genre }
        }
    } finally {
        logger.info { "closing ${blue(dbPath.pathString)}" }
        db.close()
    }
}
