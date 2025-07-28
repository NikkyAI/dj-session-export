import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

@Serializable
data class History(
    @XmlElement
    val collection: Collection,
    @XmlElement
    val playlists: Playlists,
) {
    @Serializable
    @SerialName("COLLECTION")
    data class Collection(
        @SerialName("ENTRIES")
        val entries: Int,
        val entry: List<Entry>,
    ) {
        @Serializable
        @SerialName("ENTRY")
        data class Entry(
//            @SerialName("MODIFIED_DATE")
//            val modifiedData: String,
//            @SerialName("MODIFIED_TIME")
//            val modifiedTime: String,
//            @SerialName("AUDIO_ID")
//            val audioId: String,
            @SerialName("TITLE")
            val title: String,
            @SerialName("ARTIST")
            val artist: String? = null,
            @XmlElement
            val location: Location,
            @XmlElement
            val album: Album? = null,
            @XmlElement
            val info: Info? = null,
            @XmlElement
            val tempo: Tempo? = null,
        ) {
            @Serializable
            @SerialName("LOCATION")
            data class Location(
                @SerialName("DIR")
                val dir: String,
                @SerialName("FILE")
                val file: String,
                @SerialName("VOLUME")
                val volume: String,
            ) {
                val fullPath get() = volume + dir + file
            }

            @Serializable
            @SerialName("ALBUM")
            data class Album(
                @SerialName("TRACK")
                val track: Int? = null,
                @SerialName("TITLE")
                val title: String? = null,
            )

            @Serializable
            @SerialName("INFO")
            data class Info(
                @SerialName("BITRATE")
                val bitrate: Float,
                @SerialName("GENRE")
                val genre: String? = null,
                @SerialName("LABEL")
                val label: String? = null,
                @SerialName("COMMENT")
                val comment: String? = null,
                @SerialName("REMIXER")
                val remixer: String? = null,
//                @SerialName("COVERARTID")
//                val coverArtId: String? = null,
                @SerialName("KEY")
                val key: String? = null,
                @SerialName("PLAYCOUNT")
                val playCount: Int? = null,
            )

            @Serializable
            @SerialName("TEMPO")
            data class Tempo(
                @SerialName("BPM")
                val bpm: Double,
                @SerialName("BPM_QUALITY")
                val bpmQuality: Double,
            )
        }
    }

    @Serializable
    @SerialName("PLAYLISTS")
    data class Playlists(
//        @SerialName("NODE")
        @XmlElement
        val folderNode: FolderNode
    )

    @Serializable
    @SerialName("NODE")
    data class FolderNode(
        @SerialName("NAME")
        val name: String,
        @XmlElement
        val subnodes: SubNodes
    ) {
        @Serializable
        @SerialName("SUBNODES")
        data class SubNodes(
            @SerialName("COUNT")
            val count: Int,
            val playlistNode: PlaylistNode,
        )
    }


    @Serializable
    @SerialName("NODE")
    data class PlaylistNode(
        @SerialName("NAME")
        val name: String,
        @XmlElement
        val playlist: Playlist
    ) {
        @Serializable
        @SerialName("PLAYLIST")
        data class Playlist(
            @SerialName("ENTRIES")
            val entryCount: Int,
            @XmlElement
            val entries: List<Entry>,
        ) {
            @Serializable
            @SerialName("ENTRY")
            data class Entry(
                @XmlElement
                val primaryKey: PrimaryKey,
                @XmlElement
                val extendedData: ExtendedData,
            ) {
                @Serializable
                @SerialName("PRIMARYKEY")
                data class PrimaryKey(
                    @SerialName("TYPE")
                    val type: String,
                    @SerialName("KEY")
                    val key: String,
                )

                @Serializable
                @SerialName("EXTENDEDDATA")
                data class ExtendedData(
                    @SerialName("DECK")
                    val deck: Int,
                    @SerialName("DURATION")
                    val duration: Double,
                    @SerialName("EXTENDEDTYPE")
                    val extendedType: String,
                    @SerialName("PLAYEDPUBLIC")
                    val playedPublic: Int,
                    @SerialName("STARTDATE")
                    val startDate: Long,
                    @SerialName("STARTTIME")
                    val startTime: Int,
                )
            }
        }
    }

}