@file:OptIn(ExperimentalTime::class)

package mixxx

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime

fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    MixxxCommand(args.toList()).main(args.toList())
}
