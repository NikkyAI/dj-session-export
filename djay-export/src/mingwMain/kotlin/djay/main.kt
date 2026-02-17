@file:OptIn(ExperimentalTime::class)

package djay

import com.github.ajalt.clikt.command.main
import configureLogging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalForeignApi::class)
fun main(vararg args: String): Unit = runBlocking {
    configureLogging()
    DJayCommand(args.toList()).main(args.toList())
}
