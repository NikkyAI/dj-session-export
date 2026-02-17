package djay

data class HistorySessionItem(
    val mysteryByte: Byte,
    val uuid: String,
    val sessionUUID: String?,
//    val rowId: Int,
//    val key: String,
    val title: String,
    val artist: String?,
    val duration: String?,
    val isrc: String?,
    val deckNumber: Long,
    val startTime: String?,
    val originSourceID: String?,
)

