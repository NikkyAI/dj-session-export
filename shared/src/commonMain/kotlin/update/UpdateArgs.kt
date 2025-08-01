package update

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class UpdateArgs: OptionGroup("update") {

    val skipSelfUpdate by option("--noupdate")
        .flag()

    suspend fun doUpdate(rawArgs: List<String>) {
        if(skipSelfUpdate) return

        val file = Updater.getUpdatedBinary(
            assetName = currentProgramPath.name,
        )

        if(file != null) {
            Updater.overrideSelfAndLaunch(
                file,
                "--noupdate", *rawArgs.toTypedArray()
            )
        }
    }
}