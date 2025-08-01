import com.github.ajalt.clikt.command.main
import io.github.oshai.kotlinlogging.Formatter
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import kotlinx.coroutines.runBlocking

fun main(vararg args: String) : Unit = runBlocking {
    configureLogging()

    MainCommand(args.toList()).main(args.toList())

//    logger.info { "Hello World ${args.joinToString(" ")}" }
//
//
//    if(args.firstOrNull() != "--updated") {
//
//        val copyPath = currentProgramPath.parent!! / (currentProgramPath.name + ".copy.exe")
//        FileSystem.SYSTEM.copy(currentProgramPath, copyPath)
//        logger.info { "copied $currentProgramPath to $copyPath" }
//
//        overrideSelfAndLaunch(
//            copyPath,
//            "--updated", *args
//        )
//    }
//
//    FileSystem.SYSTEM.write("sucess.txt".toPath()) {
//        writeUtf8("blub\n")
//    }

}