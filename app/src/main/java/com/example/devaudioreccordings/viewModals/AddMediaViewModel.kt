package com.example.devaudioreccordings.viewModals

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.database.AudioTextDatabase
import com.example.devaudioreccordings.database.FlowType
import com.example.devaudioreccordings.network.RetrofitInstance
import com.example.devaudioreccordings.user
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AddMediaViewModel : ViewModel() {
    val header = mutableStateOf("Media Header")
    val content = mutableStateOf("")
    val audioFileName = mutableStateOf("")
    val isTextGettingGenerated = mutableStateOf(false)
    var token: String = ""


    suspend fun createData(filename: String, database: AudioTextDatabase): Int {
        val totalDataLength = 0
        database.dao.instertAudioText(
            convertAudioTextToAudioTextDbData(
                AudioText(
                    id = totalDataLength,
                    text = "Not able to create Text",
                    audioFileName = filename,
                    header = "My Media Header",
                    flowType = FlowType.AddMedia,
                    imageCollection = null,
                    isApiCallRequired = false
                ), true
            )

        )
        val latestCreatedId = database.dao.getLatestCreatedId()
        return latestCreatedId
    }


    suspend fun uploadFile(file: File, database: AudioTextDatabase, context: Context) {

        val id = createData(filename = file.name, database)
        val prefs = context.user.data.first()
        val uidData = prefs[DataStoreKeys.USER_UID] ?: ""
        try {

            val requestFile = file.asRequestBody("audio/pcm".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val uid = MultipartBody.Part.createFormData("uid", uidData)
            val timestamps =
                MultipartBody.Part.createFormData(
                    "timestamps",
                    "null"
                )
            val saveContentAsPart =
                MultipartBody.Part.createFormData("saveContentAs", "transcript")

            isTextGettingGenerated.value = true
            val responseObj = withContext(Dispatchers.IO) {
                RetrofitInstance.ApiServices.uploadFile(
                    token,
                    filePart, uid,
                    saveContentAsPart,
                    timestamps
                )
            }
            val response = responseObj.response
            val used_Transcription_Duration =
                responseObj.Used_Transcription_Duration.toDouble().toLong()
            val total_Transcription_Duration =
                responseObj.Total_Transcription_Duration.toDouble().toLong()
            if (response != "") {
                database.dao.updateAudioText(
                    convertAudioTextToAudioTextDbData(
                        AudioText(
                            id = id,
                            text = response,
                            audioFileName = file.name,
                            header = header.value,
                            flowType = FlowType.AddMedia,
                            imageCollection = null,
                            isApiCallRequired = false
                        ), false
                    )
                )
                context.user.edit {
                    it[DataStoreKeys.Used_Transcription_Duration] = used_Transcription_Duration
                    it[DataStoreKeys.Total_Transcription_Duration] = total_Transcription_Duration
                }
                isTextGettingGenerated.value = false
                content.value = response

            } else {
                database.dao.updateAudioText(
                    convertAudioTextToAudioTextDbData(
                        AudioText(
                            id = id,
                            text = "Not Able To Create Text",
                            audioFileName = file.name,
                            header = header.value,
                            flowType = FlowType.AddMedia,
                            imageCollection = null,
                            isApiCallRequired = true

                        ), false
                    )
                )
                isTextGettingGenerated.value = false
                content.value = "Not Able To Create Text"
            }
        } catch (e: Throwable) {
            isTextGettingGenerated.value = false
            database.dao.updateAudioText(
                convertAudioTextToAudioTextDbData(
                    AudioText(
                        id = id,
                        text = "Not Able To Create Text",
                        audioFileName = file.name,
                        header = header.value,
                        flowType = FlowType.AddMedia,
                        imageCollection = null,
                        isApiCallRequired = true
                    ), false
                )
            )
        }

    }
}

