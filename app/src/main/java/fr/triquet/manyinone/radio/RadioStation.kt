package fr.triquet.manyinone.radio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "radio_stations")
data class RadioStation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val streamUrl: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
)

val DEFAULT_STATIONS = listOf(
    RadioStation(
        name = "France Info",
        streamUrl = "http://direct.franceinfo.fr/live/franceinfo-midfi.mp3",
        description = "L'info en continu — MP3 128kbps",
        sortOrder = 0,
    ),
    RadioStation(
        name = "Ibiza Organica",
        streamUrl = "https://stream.aiir.com/ilduibssvzbtv",
        description = "Organic house & downtempo — Ibiza vibes",
        sortOrder = 1,
    ),
    RadioStation(
        name = "Ibiza Global Radio",
        streamUrl = "https://listenssl.ibizaglobalradio.com:8024/;",
        description = "The soundtrack of Ibiza — Dance & House — MP3 128kbps",
        sortOrder = 2,
    ),
)
