import kotlin.time.Duration

fun Duration.formatTimestamp(): String {
    val localTime = toComponents() { days, hours, minutes, seconds, _ ->
        listOf(
            days,
            hours,
            minutes,
            seconds
        ).joinToString(":") {
            it.toString().padStart(2, '0')
        }
    }
    return localTime
//    val localTime = LocalTime.fromSecondOfDay(inWholeSeconds.toInt())
//    return localTime.format(durationTimestampFormat)
}