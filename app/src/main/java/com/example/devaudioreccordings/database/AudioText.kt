package com.example.devaudioreccordings.database

import androidx.room.ColumnInfo
import androidx.room.Entity


enum class FlowType{
    MediaCaptureService,AddMedia,AddText,AddAudio,RecordAudio
}


data class AudioText(

    var text: String,
    val audioFileName: String?,
    val imageTimestampList: String? = null,
    val header: String = "Anonymous",
    val subHeader: String? = null,
    val flowType: FlowType,
    val isApiCallRequired:Boolean,
    val imageCollection: MutableList<String>?,
    val id: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),

    )


@Entity()
data class HeaderAndCreatedAt(
    val header : String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)


