fun String.deduplicateRepeating(separator: String = " "): String {
    (2..length - 1).forEach { i ->
        val candidate = substring(0, i)

        val shouldBeAllSpaces = split(candidate)

        val match = shouldBeAllSpaces.all { it == separator || it.isEmpty() }
        if (match) {
            return candidate
        }
    }

    return this
}