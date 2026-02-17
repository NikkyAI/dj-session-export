import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.Appender
import io.github.oshai.kotlinlogging.DirectLoggerFactory
import io.github.oshai.kotlinlogging.KLoggingEvent
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import sun.misc.Signal
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val logChannel: Channel<Pair<KLoggingEvent, String>> = Channel()
private val loggingScope = CoroutineScope(CoroutineName("log-writer"))

suspend fun configureLoggingOld() {

    KotlinLoggingConfiguration.loggerFactory = DirectLoggerFactory
    KotlinLoggingConfiguration.direct.logLevel = Level.DEBUG

    val now = Clock.System.now()
    val basename = now.toLocalDateTime(TimeZone.currentSystemDefault()).format(
        LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour()
            char('-')
            minute()
            char('-')
            second()
        }
    )
    val logFile = getExportFolder() / "logs" / "$basename.log"
    logFile.parent?.let {
        FileSystem.SYSTEM.createDirectories(it)
    }

    val terminalAppender = object : Appender {
        val term = Terminal()
        val includePrefix = true
        override fun log(loggingEvent: KLoggingEvent) {
            with(loggingEvent) {
                val formattedLogMessage = buildString {
                    append(prefix(level, loggerName))
                    marker?.getName()?.let {
                        append(it)
                        append(" ")
                    }
                    append(message)
                    cause
                        .throwableToString()
                        .takeUnless { it.isBlank() }
                        ?.let {
                            append(red(it))
                        }

                    appendLine()
                }
                term.rawPrint(formattedLogMessage)
            }
        }

        private fun prefix(level: Level, loggerName: String): String {
            return if (includePrefix) {
                val levelColor = when (level) {
                    Level.TRACE -> gray
                    Level.DEBUG -> cyan
                    Level.INFO -> white
                    Level.WARN -> brightYellow
                    Level.ERROR -> brightRed
                    Level.OFF -> black
                }
                "${levelColor(level.name)}: ${gray("[$loggerName]")} "
            } else {
                ""
            }
        }

        private fun Throwable?.throwableToString() = createThrowableMsg("", this)

        private tailrec fun createThrowableMsg(msg: String, throwable: Throwable?): String {
            return if (throwable == null || throwable.cause == throwable) {
                msg
            } else {
                createThrowableMsg("$msg, Caused by: '${throwable.message}'", throwable.cause)
            }
        }

    }

    val plainTerminal = Terminal(AnsiLevel.NONE)

    KotlinLoggingConfiguration.direct.appender = object : Appender {
        override fun log(loggingEvent: KLoggingEvent) {
            terminalAppender.log(
                loggingEvent = loggingEvent
            )
            val loggingEvent = loggingEvent.copy(
                message = plainTerminal.render(
                    message = loggingEvent.message,
                    whitespace = Whitespace.NOWRAP,
                    width = 150
                )
            )
            KotlinLoggingConfiguration.direct.formatter.formatMessage(loggingEvent).let {
                logChannel.trySendBlocking(loggingEvent to it)
//                logFormattedMessage(loggingEvent, it)
            }
        }

        fun logFormattedMessage(
            loggingEvent: KLoggingEvent,
            formattedMessage: String,
        ) {
            logChannel.trySendBlocking(loggingEvent to formattedMessage)

        }
    }

    loggingScope.launch {
        FileSystem.SYSTEM.write(logFile) {
            var lastFlush = Instant.DISTANT_PAST
            for ((event, msg) in logChannel) {
                writeUtf8(msg)
                writeUtf8("\n")
                val now = Clock.System.now()
                if (now > lastFlush + 10.seconds) {
                    flush()
                    lastFlush = now
                }
            }
        }
        println("logfile closed")
    }


    Runtime.getRuntime().addShutdownHook(
        Thread {
            logChannel.close()
            loggingScope.cancel()
            println("Exit!")
        }
    )
//    atexit(staticCFunction<Unit> {
//        logChannel.close()
////        .loggingScope.cancel()
//        println("Exit!")
//    })
//    signal(SIGINT, staticCFunction<Int, Unit> {
//        logChannel.close()
////        runBlocking {
////            delay(1.seconds)
////        }
//        loggingScope.cancel()
//        println("Exit!")
//    })
//    KotlinLoggingConfiguration.formatter = object : Formatter {
//        override fun formatMessage(loggingEvent: KLoggingEvent): String {
//            with(loggingEvent) {
//                return buildString {
//                    append(prefix(level, loggerName))
//                    marker?.getName()?.let {
//                        append(it)
//                        append(" ")
//                    }
//                    append(message)
//                    append(cause.throwableToString())
//                }
//            }
//        }
//        private fun prefix(level: Level, loggerName: String): String {
//            return if (includePrefix) {
//                "${level.name}: [$loggerName] "
//            } else {
//                ""
//            }
//        }
//
//    }

}

actual fun registerOnExit() {
    Runtime
        .getRuntime()
        .addShutdownHook(
            Thread {
                onExit()
            }
        )
}

actual fun registerInterrupt() {
    Signal.handle(
        Signal("INT"),  // SIGINT
        {
            onInterrupt()
        }
    )
}