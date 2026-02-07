package com.example.devaudioreccordings.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity()
data class AudioTextDbData(

    var text: String,
    val audioFileName: String?,
    val imageTimestampList : String? = null,
    val subHeader: String? = null,
    val header: String = "Anonymous",
    val flowType: FlowType,
    val isApiCallRequired:Boolean,
    val imageCollection : String?,
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()
)



