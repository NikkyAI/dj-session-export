import kotlin.time.Instant

sealed interface Chunk {
    val length: Int
    val tag: String

    data class StringData(
        override val length: Int,
        override val tag: String,
        val data: String
    ) : Chunk

    data class Date(
        override val length: Int,
        override val tag: String,
        val data: Instant
    ) : Chunk

    data class Chunks(
        override val length: Int,
        override val tag: String,
        val data: List<Chunk>
    ) : Chunk

    data class IntData(
        override val length: Int,
        override val tag: String,
        val data: Int
    ) : Chunk

}