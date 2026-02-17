package djay

data class HistorySession(
    val mysteryByte: Byte,
    val uuid: String,
    val deviceName: String,
    val deviceType: String,
    val startDate: String,
    val endDate: String,
    val itemUUIDs: List<String>,
)