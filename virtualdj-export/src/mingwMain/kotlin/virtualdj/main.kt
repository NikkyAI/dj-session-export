package virtualdj

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalForeignApi::class)
fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    VirtualDJCommand(args.toList()).main(args.toList())
}
