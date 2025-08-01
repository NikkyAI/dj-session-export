import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
object Folders {
    val TEMP = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "dj-session-export"
    val APPDATA = getenv("APPDATA")?.toKString()?.toPath(true) ?: error("cannot lookup %APPDATA%")
    val LOCAL_APPDATA = getenv("LOCALAPPDATA")?.toKString()?.toPath(true) ?: error("cannot lookup %APPDATA%")
    val USERPROFILE = getenv("USERPROFILE")?.toKString()?.toPath(true) ?: error("cannot lookup %APPDATA%")

    val userDocuments by lazy {
        Command("powershell.exe")
            .args(
                "-Command",
                "[Environment]::GetFolderPath('MyDocuments')"
            )
            .stdout(Stdio.Pipe)
            .spawn()
            .waitWithOutput()
            .stdout
            ?.trim()
            ?.toPath()
            ?: error("failed to get documents folder location using powershell")
    }
}