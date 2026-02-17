package serato

import SuspendingCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path.Companion.toPath
import update.UpdateArgs
import currentProgramPath
import exitProcess
import okio.SYSTEM

class SeratoCommand(
    val rawArgs: List<String>
): SuspendingCommand(currentProgramPath.name) {
    private val logger = KotlinLogging.logger("SeratoCommand.kt")
    val update by UpdateArgs()

    override fun help(context: Context): String = """
        exports history from Serator, test on version 3.2.3
        for Serato 4 you can use the builtin history export until support is implemented in this tool
    """.trimIndent()

    val seratorPath by option(
        "--serato",
        help ="serato library path, expects path to `_Serato_` folder, env: `%SERATO_LIBRARY%`",
        envvar = "SERATO_LIBRARY"
    )
        .convert {
            it.toPath(true)
        }
        .default(defaultSeratoPath)

    val args by argument().convert {
        it.toPath().takeIf { FileSystem.SYSTEM.exists(it) } ?: fail("path $it does not exist")
    }.multiple()

//    init {
//        context {
//            helpFormatter = {
//                MordantHelpFormatter(
//                    context = it,
//                    requiredOptionMarker = "*",
//                    showDefaultValues = true,
//                    showRequiredTag = true,
//                )
//            }
//        }
//    }

    override suspend fun run() {
        update.doUpdate(rawArgs)

        logger.info { "starting serato export" }
        exportSerato(seratorPath)

        terminal.println(TextColors.brightWhite("PRESS ENTER TO CLOSE"))
        terminal.readLineOrNull(hideInput = true)
        exitProcess(0)
    }
}