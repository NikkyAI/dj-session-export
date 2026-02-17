import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.mordant.rendering.TextColors
import io.github.oshai.kotlinlogging.KotlinLogging
import mixxx.canExportMixxx
import mixxx.exportMixxx
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import serato.canExportSerato
import serato.exportSerato
import traktor.canExportTraktor
import traktor.exportTraktor
import update.UpdateArgs
import virtualdj.canExportVirtualDJ
import virtualdj.exportVirtualDJ

class MainCommand(
    val rawArgs: List<String>
): SuspendingCommand(currentProgramPath.name) {
    private val logger = KotlinLogging.logger("MainCommand.kt")
    val update by UpdateArgs()

    val args by argument().convert {
        it.toPath().takeIf { FileSystem.SYSTEM.exists(it) } ?: fail("path $it does not exist")
    }.multiple()

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, requiredOptionMarker = "*") }
        }
    }

    override suspend fun run() {
        update.doUpdate(rawArgs)

        if(canExportMixxx()) {
            exportMixxx()
        }
        if (rekordbox.canExportRekordbox()) {
            rekordbox.exportRekordbox()
        }
        if(canExportSerato()) {
            exportSerato()
        }
        if(canExportTraktor(args)) {
            exportTraktor(args)
        }
        if(canExportVirtualDJ(args)) {
            exportVirtualDJ(args)
        }

        terminal.println(TextColors.brightWhite("PRESS ENTER TO CLOSE"))
        terminal.readLineOrNull(hideInput = true)
    }
}