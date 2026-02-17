import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import okio.FileSystem
import okio.Path.Companion.toPath

object Folders {
    val TEMP = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "dj-session-export"
    val APPDATA by lazy{
        getPathFromEnv("APPDATA") ?: error("cannot lookup %APPDATA%")
    }
    val LOCAL_APPDATA by lazy {
        getPathFromEnv("LOCALAPPDATA") ?: error("cannot lookup %LOCALAPPDATA%")
    }
    val USERPROFILE by lazy {
        getPathFromEnv("USERPROFILE") ?: error("cannot lookup %USERPROFILE%")
    }

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

    val userMusic by lazy {
        Command("powershell.exe")
            .args(
                "-Command",
                "[Environment]::GetFolderPath('MyMusic')"
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