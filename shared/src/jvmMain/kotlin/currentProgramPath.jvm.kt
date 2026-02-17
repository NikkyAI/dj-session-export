import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.io.File

actual val currentProgramPath: Path
    get() {
//        TODO("Not yet implemented")

        return File(
            Folders::class.java
                .protectionDomain
                .codeSource
                .location
                .path
        ).toOkioPath(true)
    }