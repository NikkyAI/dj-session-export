package traktor

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.coroutines.runBlocking

fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    TraktorCommand(args.toList()).main(args.toList())
}
