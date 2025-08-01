package rekordbox

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalForeignApi::class)
fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    RekordBoxCommand(args.toList()).main(args.toList())
}
