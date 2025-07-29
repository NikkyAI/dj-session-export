import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
expect fun executeCommand(
    command: String,
    trim: Boolean = true,
    redirectStderr: Boolean = true
): String

// https://github.com/jmfayard/kotlin-cli-starter/blob/main/src/commonMain/kotlin/io/CommonExpects.kt#L28
expect fun executeCommandAndCaptureOutput(
    command: List<String>,
    options: ExecuteCommandOptions = ExecuteCommandOptions(
        directory = ".",
        abortOnError = true,
        redirectStderr = true,
        trim = true
    )
): String

data class ExecuteCommandOptions(
    val directory: String,
    val abortOnError: Boolean,
    val redirectStderr: Boolean,
    val trim: Boolean
)