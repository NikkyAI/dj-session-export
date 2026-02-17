import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.installMordant
import com.github.ajalt.clikt.output.MordantMarkdownHelpFormatter

abstract class SuspendingCommand(name: String): SuspendingCliktCommand(
    name = name,
) {

    init {
        installMordant(force = true)
        context {
            helpFormatter = {
                MordantMarkdownHelpFormatter(
                    context = it,
                    requiredOptionMarker = "*",
                    showDefaultValues = true,
                    showRequiredTag = true,
                )
            }
        }
    }
}