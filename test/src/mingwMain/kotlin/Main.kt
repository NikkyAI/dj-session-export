import com.kgit2.kommand.process.Command
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.sqlite.SQLite
import kotlin.system.exitProcess

//val TMP = FileSystem.SYSTEM_TEMPORARY_DIRECTORY
//val FS = FileSystem.SYSTEM

fun main(vararg args: String) {

    Command(
        "start"
    )
        .args(
            listOf(
                "."
            )
        )
        .spawn()
        .wait()

    val options = Driver.Pool.Options.builder()
        .maxConnections(10)
        .build()

    exitProcess(0)

    /**
     * The following urls are supported:
     * `sqlite::memory:`            | Open an in-memory database.
     * `sqlite:data.db`             | Open the file `data.db` in the current directory.
     * `sqlite://data.db`           | Open the file `data.db` in the current directory.
     * `sqlite:///data.db`          | Open the file `data.db` from the root (`/`) directory.
     * `sqlite://data.db?mode=ro`   | Open the file `data.db` for read-only access.
     */
    val db = SQLite(
        url = "sqlite://sqlite.db", // If the `test.db` file is not found, a new db will be created.
        options = options
    )

//    val sqlCipherPath = "" //TMP / "rekordbox-history-converter" / "sqlcipher.exe"
//
////    val appdata = getenv("APPDATA")?.toKString() ?: error("cannot lookup %APPDATA%")
//    val encryptedPath = "" // appdata.toPath(true) / "Pioneer" / "rekordbox" / "master.db"
//
//    println(sqlCipherPath)
//
////    // downloading sqlcipher
////    runBlocking {
////        if (FS.exists(sqlCipherPath)) {
////            println("sqlcipher already downloaded")
////            return@runBlocking
////        }
////        println("downloading sqlcipher")
////
////        httpClient.prepareGet(urlString = "https://github.com/Katecca/sqlcipher-static-binary/raw/refs/heads/master/windows/x86_64/sqlcipher.exe")
////            .execute { httpResponse ->
////                val channel: ByteReadChannel = httpResponse.body()
////                var count = 0L
////                FS.createDirectories(sqlCipherPath.parent!!, mustCreate = false)
////                FS.write(sqlCipherPath, mustCreate = false) {
////                    val rawSink = asKotlinxIoRawSink()
////                    while (!channel.exhausted()) {
////                        val chunk = channel.readRemaining()
////                        count += chunk.remaining
////
////                        chunk.transferTo(rawSink)
////                        println("Received $count bytes from ${httpResponse.contentLength()}")
////                    }
////                }
////            }
////        println("downloaded sqlcipher")
////    }
//
//    val dbPath = "" // TMP / "rekordbox-history-converter" / "plaintext.db"
////    FS.delete(dbPath, mustExist = false)
//
//    // decoding master.db
////    runBlocking {
//
//        val sqlLines = """
//            PRAGMA key='402fd482c38817c35ffa8ffb8c7d93143b749e7d315df7a81732a1ff43608497';
//            ATTACH DATABASE '$dbPath' AS plaintext KEY '';
//            SELECT sqlcipher_export('plaintext');
//            DETACH DATABASE plaintext;
//        """.trimIndent()
//            .lines()
//        val sqlQuoted = sqlLines
//            .joinToString(" ", "\"", "\"")
//        val sql = sqlLines
//            .joinToString(" ")
//
//        Command(
//            sqlCipherPath.toString()
//        )
//            .args(
//                listOf(
//                    encryptedPath.toString(),
//                    sqlQuoted
//                )
//            )
//            .spawn()
//            .wait()
////        val response = executeCommandAndCaptureOutput(
////            listOf(
////                sqlCipherPath.toString(),
////                encryptedPath.toString(),
////                sql
////            )
////        )
////        println(response)
////    }
}
