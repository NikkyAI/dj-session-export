package rekordbox

import SuspendingCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.mordant.rendering.TextColors
import currentProgramPath
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import update.UpdateArgs

class RekordBoxCommand(
    val rawArgs: List<String>
): SuspendingCommand(currentProgramPath.name) {
    private val logger = KotlinLogging.logger("rekordbox/RekordBoxCommand.ktCommand.kt")
    val update by UpdateArgs()

    val args by argument().convert {
        it.toPath().takeIf { FileSystem.SYSTEM.exists(it) } ?: fail("path $it does not exist")
    }.multiple()

    override suspend fun run() {
        update.doUpdate(rawArgs)

        exportRekordbox()

        terminal.println(TextColors.brightWhite("PRESS ENTER TO CLOSE"))
        terminal.readLineOrNull(hideInput = true)
    }
}