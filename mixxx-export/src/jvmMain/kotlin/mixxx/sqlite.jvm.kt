package mixxx

import io.github.smyrgeorge.sqlx4k.ConnectionPool
import io.github.smyrgeorge.sqlx4k.sqlite.ISQLite
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite

actual fun sqliteDB(url: String): ISQLite = SQLite(
    url = url,
    options = ConnectionPool.Options(
//        minConnections = null,
        maxConnections = 10
    )
)
