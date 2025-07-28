fun String.deduplicate(): String {
    (2..length - 1).forEach { i ->
        val candidate = substring(0, i)

//        println("candiate: '$candidate'")

        val shouldBeAllSpaces = split(candidate)
//        println(shouldBeAllSpaces)

        val match = shouldBeAllSpaces.all { it == " " || it == "" }
        if (match) {
            return candidate
        }
    }

    return this
}