fun trackListFrom(session: Session): Tracklist<HistoryTrack>? {
    return try {
        val referenceInstant = session.songs.first().timePlayed
        //exportStartTime.toInstant(TimeZone.currentSystemDefault())

        val tracks = session.songs.map { it ->
            HistoryTrack(
                time = it.timePlayed - referenceInstant,
                timePlayed = it.timePlayed,
                title = it.title,
                artist = it.artist,
                filePath = it.filePath,
                bpm = it.bpm,
            )
        }


        Tracklist(
            title = referenceInstant.toString()
                .replace(":", "-")
                .replace("T", " "),
            tracks = tracks
        )
    } catch (error: Exception) {
        println("Error reading file: \n$error")
        error.printStackTrace()
        null
    }
}

fun main(vararg args: String) {
//    val documents = executeCommand("powershell.exe -Command [Environment]::GetFolderPath('MyDocuments')")
//    println(documents)

    val sessions = getSeratoHistory()

    sessions.forEach { session ->

        println()
        println("converting $session")

        val tracklist = trackListFrom(session)
        println(tracklist)

        if (tracklist != null) {
            Template.write(
                tracklist,
                HistoryTrack.serializer(),
            )
        }
    }
    println("")
    println("PRESS ANY BUTTON TO CLOSE")
    readlnOrNull()
}

