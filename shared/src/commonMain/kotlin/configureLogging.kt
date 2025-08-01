import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.Appender
import io.github.oshai.kotlinlogging.ConsoleOutputAppender
import io.github.oshai.kotlinlogging.KLoggingEvent
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
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
import platform.posix.SIGINT
import platform.posix.atexit
import platform.posix.signal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.github.ajalt.mordant.rendering.TextColors.*

private val logChannel: Channel<Pair<KLoggingEvent, String>> = Channel()
private val loggingScope = CoroutineScope(CoroutineName("log-writer"))

@OptIn(ExperimentalForeignApi::class)
suspend fun configureLogging() {

    KotlinLoggingConfiguration.logLevel = Level.DEBUG

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

    KotlinLoggingConfiguration.appender = object : Appender {
        override fun log(loggingEvent: KLoggingEvent) {
            terminalAppender.log(
                loggingEvent = loggingEvent
            )
            KotlinLoggingConfiguration.formatter.formatMessage(loggingEvent).let {
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
                writeUtf8(msg.toString())
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

    atexit(staticCFunction<Unit> {
        logChannel.close()
//        loggingScope.cancel()
        println("Exit!")
    })
    signal(SIGINT, staticCFunction<Int, Unit> {
        logChannel.close()
//        runBlocking {
//            delay(1.seconds)
//        }
        loggingScope.cancel()
        println("Exit!")
    })
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