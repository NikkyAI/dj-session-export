package update

import com.github.ajalt.mordant.rendering.TextColors.*
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import com.saveourtool.okio.pathString
import com.saveourtool.okio.safeToRealPath
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.winhttp.WinHttp
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.io.okio.asKotlinxIoRawSink
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes

object Updater {
    private val logger = KotlinLogging.logger("Updater.kt")

    fun getCurrentBinaryDigest(): String {
        val digest = SHA256()
        FileSystem.SYSTEM.read(currentProgramPath) {
            digest.update(readByteArray())
        }
        return "sha256:" + digest.digest().toHexString()
    }


    fun overrideSelfAndLaunch(
        newFile: Path,
        vararg args: String,
    ) {
        val folder = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "dj-session-exporter"
        FileSystem.SYSTEM.createDirectories(folder)
        val scriptPath = folder / "update.ps1"
        val arglist = args.joinToString(",") { arg ->
            "\"$arg\""
        }
        FileSystem.SYSTEM.write(scriptPath) {
            // language=PowerShell
            writeUtf8(
                $$"""
            $ErrorActionPreferenceBak = $ErrorActionPreference
            $ErrorActionPreference    = 'Stop'

            $sec = 0 
            
            While($True){
                try{
                    Write-Host -ForegroundColor Green "$sec Sec"
                    
                    Copy-Item "$$newFile" -Destination "$$currentProgramPath"
                    
                    Write-Host -ForegroundColor Green "copied file $$newFile to $$currentProgramPath"
                    
                    break
                }
                catch{
                    Write-Output "Something failed"
                    Start-Sleep -Seconds 1 # wait for a seconds before next attempt.
                    $sec++    
                }
                finally{
                    #Reset the erroracton preference
                    $ErrorActionPreference = $ErrorActionPreferenceBak
                }
            }
            
            Start-Process -FilePath "$$currentProgramPath" -ArgumentList $$arglist
            
            Remove-Item $$newFile
            
            Start-Sleep -Seconds 10
        """.trimIndent()
            )
        }


        Command("powershell.exe")
            .args(
                "-Command",
                "Start-Process",
                "powershell.exe",
                scriptPath.safeToRealPath().pathString
            )
            .stdout(Stdio.Inherit)
            .spawn()
            .wait()

        exitProcess(0)
    }

    suspend fun getUpdatedBinary(
        assetName: String,
        githubUser: String = "nikkyai",
        project: String = "dj-session-export",
        tag: String = "nightly",
        currentDigest: String = getCurrentBinaryDigest(),
    ): Path? {
        val httpClient = HttpClient(WinHttp) {}
        val json = Json {
            ignoreUnknownKeys = true
        }

        val responseString = httpClient.get(
            urlString = "https://api.github.com/repos/$githubUser/$project/releases/tags/$tag"
        ).bodyAsText()
        val githubRelease = try {
            json.decodeFromString(GithubRelease.serializer(), responseString)
        } catch (e: SerializationException) {
            logger.info { responseString.trim() }
            logger.error(e) { "failed to parse json" }
            exitProcess(2)
        }
//    logger.info { githubRelease }
        val asset = githubRelease.assets.firstOrNull() {
            it.name == assetName
        } ?: run {
            logger.info { "asset ${brightMagenta(assetName)} is missing from release" }
            return null
        }
        if (asset.digest == currentDigest) {
            logger.info { "digest matches" }
            return null
        }
        if (asset.updatedAt <= (Generated.buildTime + 10.minutes)) {
            logger.info { "release is older than binary" }
            return null
        }

        val folder = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "dj-session-export"
        FileSystem.SYSTEM.createDirectories(folder)
        val targetFile = folder / asset.name

        httpClient.prepareGet(urlString = asset.browserDownloadUrl)
            .execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                var count = 0L
                FileSystem.SYSTEM.createDirectories(targetFile.parent!!, mustCreate = false)
                FileSystem.SYSTEM.write(targetFile, mustCreate = false) {
                    val rawSink = asKotlinxIoRawSink()
                    while (!channel.exhausted()) {
                        val chunk = channel.readRemaining()
                        count += chunk.remaining

                        chunk.transferTo(rawSink)
                        logger.debug { "Received $count bytes from ${httpResponse.contentLength()}" }
                    }
                }
            }
        logger.info { "downloaded ${brightMagenta(assetName)}" }

        return targetFile
    }
}

