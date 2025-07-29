import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.FILE
import platform.posix.chdir
import platform.posix.execvp
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.pid_t
import platform.posix.popen

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual fun executeCommand(
    command: String,
    trim: Boolean,
    redirectStderr: Boolean
): String = memScoped {
    println("executing: $command")

    val commandToExecute = if (redirectStderr) "$command 2>&1" else command
    val fp = popen?.invoke(commandToExecute.cstr.ptr, "r".cstr.ptr) ?: error("Failed to run command: $command")

    val stdout = buildString {
        val buffer = ByteArray(4096)
        while (true) {
            val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
            append(input.toKString())
        }
    }

    val status = pclose?.invoke(fp)
    if (status != 0) {
        error("Command `$command` failed with status $status${if (redirectStderr) ": $stdout" else ""}")
    }

    if (trim) stdout.trim() else stdout
}


/**
 * https://stackoverflow.com/questions/57123836/kotlin-native-execute-command-and-get-the-output
 */
@OptIn(ExperimentalForeignApi::class)
actual fun executeCommandAndCaptureOutput(
    command: List<String>,
    options: ExecuteCommandOptions
): String {
    chdir(options.directory)
    val commandToExecute = command.joinToString(separator = " ") { arg ->
        if (arg.contains(" ") || arg.contains("%")) "\"$arg\"" else arg
    }
    println("executing: $commandToExecute")
    val redirect = if (options.redirectStderr) " 2>&1 " else ""
    val fp = _popen("$commandToExecute $redirect", "r") ?: error("Failed to run command: $command")

    val stdout = buildString {
        val buffer = ByteArray(4096)
        while (true) {
            val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
            append(input.toKString())
        }
    }

    val status = _pclose(fp)
    if (status != 0 && options.abortOnError) {
        println(stdout)
        println("failed to run: $commandToExecute")
        throw Exception("Command `$command` failed with status $status${if (options.redirectStderr) ": $stdout" else ""}")
    }

    return if (options.trim) stdout.trim() else stdout
}