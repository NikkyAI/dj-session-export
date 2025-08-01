package serato

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalForeignApi::class)
fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    SeratoCommand(args.toList()).main(args.toList())
}
