package serato

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.coroutines.runBlocking

fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    SeratoCommand(args.toList()).main(args.toList())
}
