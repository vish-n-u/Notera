package com.example.devaudioreccordings.viewModals

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devaudioreccordings.AppColorTheme
import com.example.devaudioreccordings.AppTheme
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.database.AudioTextDatabase
import com.example.devaudioreccordings.database.AudioTextDbData
import com.example.devaudioreccordings.database.FlowType
import com.example.devaudioreccordings.database.HeaderAndCreatedAt
import com.example.devaudioreccordings.network.IdRequest
import com.example.devaudioreccordings.network.RetrofitInstance
import com.example.devaudioreccordings.network.TextRequest
import com.example.devaudioreccordings.user
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class AppViewModel(application: Application) : AndroidViewModel(application) {
    val database = AudioTextDatabase.getDatabase(context = application)
    val dao = database.dao

    var token: String = "123"

    private val _data = MutableStateFlow<List<AudioText>>(emptyList())
    private val _headerData = MutableStateFlow<List<HeaderAndCreatedAt>>(emptyList())
    val fullData: StateFlow<List<AudioText>> get() = _data
    val fullHeaderData: StateFlow<List<HeaderAndCreatedAt>> get() = _headerData

    val audioText = mutableStateOf<String>("")

    var isNewTextCreated =
        mutableStateOf(false) // just a flag to know if New Create Text Flow has been started and whether the data has been saved, initially false on start of Create Text Flow it will be set to true and then false after first save

    var useAppTheme = mutableStateOf(true)
    var appTheme = mutableStateOf(AppTheme.SYSTEM.toString())
    var appColorTheme = mutableStateOf(AppColorTheme.GREEN.toString())
    var showSplashPreview = mutableStateOf(false)

    val context = application
    lateinit var dataStoreDataFlow: Flow<Preferences>

    val isFirstLaunch = mutableStateOf(true)
    val isReady = mutableStateOf(false)

    init {

        viewModelScope.launch {
            dao.getHeaderList().collect { it ->
                _headerData.value = it
            }
        }
        viewModelScope.launch {
            dataStoreDataFlow = context.user.data
            val dataStoreData = dataStoreDataFlow.collectLatest { it ->
                isFirstLaunch.value = it[DataStoreKeys.first_launch] ?: true
                appTheme.value = it[DataStoreKeys.App_Theme] ?: AppTheme.SYSTEM.toString()
                appColorTheme.value =
                    it[DataStoreKeys.Color_Scheme] ?: AppColorTheme.GREEN.toString()
                if (it[DataStoreKeys.Use_App_Theme] == false) {
                    useAppTheme.value = false
                }
                isReady.value = true
            }


        }
    }

    fun selectHeader(header: String) {
        viewModelScope.launch {
            dao.getDataByHeader(header).collect { it ->
                _data.value = convertAudioList(it)
            }
        }
    }

    fun deleteAudioFile(fileName: String): Boolean {
        val file = File(File(context.getExternalFilesDir(null), "/AudioCaptures"), fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun deleteImageFile(fileName: String): Boolean {
        val file = File(File(context.getExternalFilesDir(null), "ImageDirectory"), fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }


    suspend fun saveUserData(userId: String, timeDuration: Long) {
        context.user.edit { usrData ->
            usrData[DataStoreKeys.USER_UID] = userId
            usrData[DataStoreKeys.Used_Transcription_Duration] = timeDuration
        }

        // Display Data after save:
    }

    suspend fun firstLaunch() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.user.data.first()
                var isFirstLaunch: Boolean = prefs[DataStoreKeys.first_launch] ?: true
                var userUid: String = prefs[DataStoreKeys.USER_UID] ?: ""
                var usedTranscriptionTime = prefs[DataStoreKeys.Used_Transcription_Duration] ?: ""
                val android_device_id = "123"
                val task = FirebaseAuth.getInstance().signInAnonymously().await()
                var user = task.user?.uid ?: android_device_id



                if (isFirstLaunch) {
                    // Preserve theme/color already set during onboarding
                    val savedTheme = prefs[DataStoreKeys.App_Theme] ?: AppTheme.SYSTEM.toString()
                    val savedColor = prefs[DataStoreKeys.Color_Scheme] ?: AppColorTheme.GREEN.toString()

                    context.user.edit {
                        it[DataStoreKeys.first_launch] = false
                        it[DataStoreKeys.USER_UID] = user
                        it[DataStoreKeys.Used_Transcription_Duration] = 0
                        it[DataStoreKeys.Total_Transcription_Duration] = 10 * 60 * 1000
                        it[DataStoreKeys.Used_Linkedin_Text_Conversion_Count] = 0
                        it[DataStoreKeys.Total_Linkedin_Text_Conversion_Count] = 5
                        it[DataStoreKeys.Used_Enhance_Text_Count] = 0
                        it[DataStoreKeys.Total_Enhance_Text_Count] = 10
                        it[DataStoreKeys.Use_App_Theme] = true
                        it[DataStoreKeys.App_Theme] = savedTheme
                        it[DataStoreKeys.Color_Scheme] = savedColor
                    }
                    RetrofitInstance.ApiServices.createUser(token, IdRequest(user))


                }
            } catch (e: Throwable) {
                Log.d("error==>", e.message.toString())
                Log.d("error==>", e.stackTraceToString())
            }

        }
    }


    suspend fun getLinkedinShareableAiGeneratedText(text: String): String {
        val spanned = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
        try {

            val dataStoreData = context.user.data.first()
            var response = RetrofitInstance.ApiServices.createLinkedinShareableText(
                token,
                TextRequest(
                    dataStoreData[DataStoreKeys.USER_UID] ?: "", text
                )
            )

            if (response.response != "") {
                context.user.edit {
                    it[DataStoreKeys.Used_Linkedin_Text_Conversion_Count] =
                        response.linkedinTextConversionCount
                    it[DataStoreKeys.Total_Linkedin_Text_Conversion_Count] =
                        response.totalLinkedinTextConversionCount
                }
                return response.response
            }
            return spanned.toString()
        } catch (e: Throwable) {

            return spanned.toString()
        }
    }

    suspend fun increaseLimit(text: String) {
        try {
            val dataStoreData = context.user.data.first()
            val response = RetrofitInstance.ApiServices.increaseLimit(
                token, TextRequest(
                    dataStoreData[DataStoreKeys.USER_UID] ?: "", text
                )
            )

        } catch (e: Throwable) {
            Log.d("e==>", e.message.toString())

        }

    }

    suspend fun enhanceTextUsingAI(text: String): String {
        try {
            val dataStoreData = context.user.data.first()
            var response = RetrofitInstance.ApiServices.enhanceText(
                token,
                TextRequest(
                    dataStoreData[DataStoreKeys.USER_UID] ?: "", text
                )
            )

            if (response.response != "") {
                context.user.edit {
                    it[DataStoreKeys.Used_Enhance_Text_Count] = response.usedEnhanceTextCount
                    it[DataStoreKeys.Total_Enhance_Text_Count] = response.totalEnhanceTextCount
                }
                return response.response

            }
            return ""
        } catch (e: Throwable) {
            return ""
        }

    }


    suspend fun addInitialTextData(): Int {
        dao.instertAudioText(
            AudioTextDbData(
                "",
                null,
                null,
                flowType = FlowType.AddText,
                imageCollection = null,
                isApiCallRequired = false
            )
        )
        return getLatestCreatedId()
    }

    suspend fun getLatestCreatedId(): Int {
        val totalIdCount = dao.getLatestCreatedId()
        return totalIdCount
    }


    fun saveAudioFromRaw(resId: Int, outputFileName: String): String? {
        val file = File(context.filesDir, outputFileName)

        return try {
            context.resources.openRawResource(resId).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath // Return the path to the saved file
        } catch (e: IOException) {
            e.printStackTrace()
            null // Return null if there was an error
        }
    }

    fun returnDataBasedOnId(fullData: List<AudioText>, id: Int): AudioText? {
        var data: AudioText? = null

        CoroutineScope(Dispatchers.IO).launch {
            data = convertAudioTextDbDataToAudioText(dao.getDataById(id))
        }
        return data
    }


    suspend fun returnDataBasedOnId2(fullData: List<AudioText>, id: Int): AudioText? {
        var data: AudioText? = null

        data = convertAudioTextDbDataToAudioText(dao.getDataById(id))
        return data
    }

    fun filterHeaders(
        data: State<List<HeaderAndCreatedAt>>,
        searchData: String
    ): State<List<HeaderAndCreatedAt>> {
        val uniqueHeaders: MutableList<String> = arrayListOf()
        var uniqueAudioText: State<MutableList<HeaderAndCreatedAt>> =
            mutableStateOf(mutableListOf())
        data.value.forEach { it ->
            if (!uniqueHeaders.contains(it.header)) {
                uniqueHeaders.add(uniqueHeaders.size, it.header)
                if (it.header.contains(
                        searchData,
                        true
                    )
                ) uniqueAudioText.value.add(uniqueAudioText.value.size, it)

            } else {

            }
        }

        return uniqueAudioText
    }

    @SuppressLint("SuspiciousIndentation")
    fun filterRecordingList(
        fullData: State<List<AudioText>>,
        header: String
    ): State<List<AudioText>> {
        var headerIncludedAudioText: State<MutableList<AudioText>> = mutableStateOf(mutableListOf())

        fullData.value.forEach {
            if (it.header == header) {
                headerIncludedAudioText.value.add(headerIncludedAudioText.value.size, it)
            }
        }

        headerIncludedAudioText.value.sortBy { it -> it.updatedAt }
        headerIncludedAudioText.value.reverse()

        return headerIncludedAudioText
    }

    fun updateData(audioText: AudioText) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                dao.updateAudioText(convertAudioTextToAudioTextDbData(audioText, false))
                println("success==>")
            } catch (e: Throwable) {
                println("e==>" + e.message)
            }
        }
    }

    fun deleteData(audioText: AudioText) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deleteAudioFile(audioText.audioFileName.toString())

                audioText.imageCollection?.forEach { imgPath: String ->
                    deleteImageFile(imgPath)
                }

                dao.deleteAudioText(convertAudioTextToAudioTextDbData(audioText, false))

            } catch (e: Throwable) {
                println("e==>" + e.message)
            }
        }
    }
}


fun convertAudioTextToAudioTextDbData(
    audioText: AudioText,
    isCreatingNewData: Boolean?
): AudioTextDbData {
    var audioTextDbData: AudioTextDbData
    var header = audioText.header.trim()
    var subHeader = audioText.subHeader?.trim()
    if (header == "") {
        header = "Anonymous"
    }
    if (subHeader == "") {
        subHeader = null
    }

    if (isCreatingNewData == true) {
        audioTextDbData = AudioTextDbData(
            text = audioText.text,
            audioFileName = audioText.audioFileName,
            imageTimestampList = audioText.imageTimestampList,
            header = header,
            subHeader = subHeader,
            flowType = audioText.flowType,
            isApiCallRequired = audioText.isApiCallRequired,
            imageCollection = convertListCollectionToStringOrReturnNull(audioText.imageCollection),
        )
    } else {
        audioTextDbData = AudioTextDbData(
            id = audioText.id,
            text = audioText.text,
            audioFileName = audioText.audioFileName,
            imageTimestampList = audioText.imageTimestampList,
            header = header,
            subHeader = subHeader,
            flowType = audioText.flowType,
            isApiCallRequired = audioText.isApiCallRequired,
            imageCollection = convertListCollectionToStringOrReturnNull(audioText.imageCollection),
            createdAt = audioText.createdAt,
            updatedAt = audioText.updatedAt,
        )
    }
    return audioTextDbData

}

fun convertListCollectionToStringOrReturnNull(imageCollection: MutableList<String>?): String? {
    if (imageCollection == null) return null
    if (imageCollection.size == 0) return null
    else {
        val collection = imageCollection.joinToString(",")

        return collection
    }

}


fun convertAudioTextDbDataToAudioText(audioTextDbData: AudioTextDbData): AudioText {

    var audioText = AudioText(
        text = audioTextDbData.text,
        id = audioTextDbData.id,
        subHeader = audioTextDbData.subHeader,
        updatedAt = audioTextDbData.updatedAt,
        createdAt = audioTextDbData.createdAt,
        audioFileName = audioTextDbData.audioFileName,
        imageTimestampList = audioTextDbData.imageTimestampList,
        header = audioTextDbData.header,
        flowType = audioTextDbData.flowType,
        isApiCallRequired = audioTextDbData.isApiCallRequired,
        imageCollection = convertStringToListCollectionorReturnNull(audioTextDbData.imageCollection)
    )

    return audioText
}


fun convertStringToListCollectionorReturnNull(stringCollection: String?): MutableList<String>? {
    if (stringCollection == null) return null
    else {
        val list = stringCollection.trim().split(",").toMutableList()

        return list
    }
}


fun convertAudioList(data: List<AudioTextDbData>): List<AudioText> {
    val newList = data.map {
        convertAudioTextDbDataToAudioText(it)
    }
    return newList
}


suspend fun getAppCheckToken(): String {
    val result = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
    return result?.token ?: ""
}