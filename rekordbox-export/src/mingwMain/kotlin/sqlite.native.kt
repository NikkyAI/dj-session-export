import io.github.smyrgeorge.sqlx4k.sqlite.ISQLite
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite

actual fun sqliteDB(url: String): ISQLite = SQLite(
    url = url,
)