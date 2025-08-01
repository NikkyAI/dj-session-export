package update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GithubRelease(
    val url: String,
//    val assets_url: String,
//    val upload_url: String,
    @SerialName("html_url")
    val htmlUrl: String,
//    val id: Int,
//    val author: Author
    val assets: List<Asset>,
) {
    //    data class Author()
    @Serializable
    data class Asset(
        val url: String,
//        val id: Int,
        val name: String,
        val digest: String,
        @SerialName("browser_download_url")
        val browserDownloadUrl: String,
        @SerialName("created_at")
        @Serializable(with = InstantSerializer::class)
        val createdAt: Instant,
        @SerialName("updated_at")
        @Serializable(with = InstantSerializer::class)
        val updatedAt: Instant,
    )
}