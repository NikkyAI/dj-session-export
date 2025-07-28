import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(markerClass = [ExperimentalForeignApi::class])
expect fun executeCommand(
    command: String,
    trim: Boolean = true,
    redirectStderr: Boolean = true
): String