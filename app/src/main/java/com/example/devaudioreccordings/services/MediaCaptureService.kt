package com.example.devaudioreccordings.services


import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.ImageReader
import android.media.MediaPlayer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Display
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.MainActivity
import com.example.devaudioreccordings.RecordTranscription
import com.example.devaudioreccordings.TranscriptionState
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.database.AudioTextDatabase
import com.example.devaudioreccordings.database.FlowType
import com.example.devaudioreccordings.network.RetrofitInstance
import com.example.devaudioreccordings.user
import com.example.devaudioreccordings.viewModals.convertAudioTextToAudioTextDbData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.experimental.and


class MediaCaptureService : Service() {
    private lateinit var mediaProjection: MediaProjection
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var audioRecord: AudioRecord? = null
    private var audioCaptureThread: Thread? = null


    //    privat var VideoRe
    private var sampleRate: Int? = null
    private var channels: Int? = null
    private var bitDepth: Int? = null
    private val BUFFER_SIZE_IN_BYTES = 1024 * 2
    private lateinit var database: AudioTextDatabase
    private lateinit var broadCastReciever: BroadcastReceiver

    private var stopAppJob: Job? = null
    private var stopAppTimer = 0
    val RecordTranscription = com.example.devaudioreccordings.RecordTranscription
    var imagePathList: MutableList<String> = mutableListOf()

    var metrics = Resources.getSystem().getDisplayMetrics()

    var imageReader = ImageReader.newInstance(
        metrics.widthPixels,
        metrics.heightPixels,
        PixelFormat.RGBA_8888,
        2
    )

    var timestamps = mutableListOf<String?>(null)


    var virtualDisplay: VirtualDisplay? = null

    @Volatile
    var isRecordingAudioAvailable = true

    @Volatile
    var pauseRecording = false

    @Volatile
    var headerText = "Anonymous"

    @Volatile
    var subHeaderText: String? = null


    @Volatile
    var isNewSSTaken = false

    @Volatile
    var newSSFileName = ""

    var saveContentAs = "transcript"

    fun recreateVirtualDisplay(width: Int, height: Int) {
        try {
            // Step 1: Create a new ImageReader with updated dimensions
            imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2
            )
            val newSurface = imageReader.surface

            // Step 2: Resize the existing VirtualDisplay
            virtualDisplay?.resize(width, height, metrics.densityDpi)

            // Step 3: Set the new Surface
            virtualDisplay?.setSurface(newSurface)


        } catch (e: SecurityException) {
            Log.e(
                "MediaCaptureService",
                "MediaProjection has become invalid. Cannot recreate virtual display."
            )
            // Optionally handle: stop the service or re-request permission
        } catch (e: IllegalStateException) {
            Log.e(
                "MediaCaptureService",
                "Illegal state. Possibly due to missing MediaProjection.Callback registration."
            )
        }
    }


    override fun onCreate() {

        fun stopServiceFunction() {
            CoroutineScope(Dispatchers.Default).launch {
                while (true) {
                    if (com.example.devaudioreccordings.RecordTranscription.uploadDetails.value.count == 0) {
                        stopSelf()
                        break // stop the loop after stopping the service
                    }
                    delay(500) // wait for half a second before checking again
                }
            }
        }




        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startForeground(
            1,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
        super.onCreate()
        database = AudioTextDatabase.getDatabase(context = this)
        fun registerBroadcastFun() {
            broadCastReciever = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    headerText = intent?.getStringExtra("header") ?: "Anonymous"
                    subHeaderText = intent?.getStringExtra("subHeader")
                    saveContentAs = intent?.getStringExtra("contentType") ?: "transcript"

                    when (intent?.action) {
                        MainActivity.ACTION_START_AUDIOCAPTURE -> {
                            pauseRecording = false
                            startAudioCapturing()
                        }

                        MainActivity.ACTION_PAUSE_AUDIOCAPTURE -> {
                            pauseRecording = true
                        }

                        MainActivity.ACTION_STOP_MEDIACAPTURE_SERVICE -> {
                            stopServiceFunction()
                        }

                        MainActivity.ACTION_RESUME_AUDIOCAPTURE -> {
                            pauseRecording = false
                        }

                        MainActivity.ACTION_TAKE_SCREENSHOT -> {
                            val time = intent?.getIntExtra("timestamp", -1)
                            takeSS(time)
                        }

                        Intent.ACTION_SCREEN_ON -> {
                            stopAppJob?.cancel()
                            stopAppTimer = 0


                        }

                        Intent.ACTION_SCREEN_OFF -> {
                            stopAppJob = CoroutineScope(Dispatchers.Default).launch {
                                while (true) {
                                    stopAppTimer++
                                    delay(1000)

                                    if (stopAppTimer == 600) {
                                        stopService(Intent(context, FloatingWindow::class.java))
                                        stopServiceFunction()

                                    }
                                }
                            }
                        }

                        MainActivity.ACTION_ORIENTATION_CHANGE_TO_LANDSCAPE -> {
                            metrics = Resources.getSystem().getDisplayMetrics()
                            recreateVirtualDisplay(
                                metrics.widthPixels.toInt(),
                                metrics.heightPixels.toInt()
                            )


                        }

                        MainActivity.ACTION_ORIENTATION_CHANGE_TO_POTRAIT -> {
                            metrics = Resources.getSystem().getDisplayMetrics()
                            recreateVirtualDisplay(
                                metrics.widthPixels,
                                metrics.heightPixels
                            )
//                                virtualDisplay?.release()
//
//                                imageReader?.setOnImageAvailableListener(null, null)
//                                imageReader?.close()
//                                delay(2000)
//                                imageReader = ImageReader.newInstance(
//                                    metrics.widthPixels,
//                                    metrics.heightPixels,
//                                    PixelFormat.RGBA_8888,
//                                    2
//                                )
//                                virtualDisplay = mediaProjection.createVirtualDisplay(
//                                    "ScreenShot",
//                                    metrics.widthPixels,
//                                    metrics.heightPixels,
//                                    metrics.densityDpi,
//                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                                    imageReader.surface,
//                                    null,
//                                    null
//                                )
                        }

                        else -> {
                            pauseRecording = false
                            stopAudioCapturing(headerText)
                        }
                    }
                }
            }

            val actions = listOf(
                MainActivity.ACTION_START_AUDIOCAPTURE,
                MainActivity.ACTION_STOP_AUDIOCAPTURE,
                MainActivity.ACTION_RESUME_AUDIOCAPTURE,
                MainActivity.ACTION_STOP_MEDIACAPTURE_SERVICE,
                MainActivity.ACTION_PAUSE_AUDIOCAPTURE,
                MainActivity.ACTION_STOP_APP,
                MainActivity.ACTION_TAKE_SCREENSHOT,
                MainActivity.ACTION_ORIENTATION_CHANGE_TO_LANDSCAPE,
                MainActivity.ACTION_ORIENTATION_CHANGE_TO_POTRAIT,
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_SCREEN_ON
            )

            actions.forEach { action ->
                val intentFilter = IntentFilter(action)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(broadCastReciever, intentFilter, RECEIVER_EXPORTED)
                } else {
                    registerReceiver(broadCastReciever, intentFilter)
                }
            }
        }

        registerBroadcastFun()


    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        imagePathList.forEach { imagePath: String ->
            val file = File(
                File(applicationContext.getExternalFilesDir(null), "ImageDirectory"),
                imagePath
            )
            if (file.exists()) {
                file.delete()
            } else {
                false
            }

        }
        super.onDestroy()
        stopMediaService()
        unregisterReceiver(broadCastReciever)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MainActivity.ACTION_STOP_MEDIACAPTURE -> {
                stopMediaService()
                return Service.START_NOT_STICKY
            }

            else -> {
                intent?.getParcelableExtra<Intent>("data")?.let { data ->
                    mediaProjection =
                        mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data)

                    // ✅ Register callback BEFORE starting capture
                    var mediaProjectionCallback = object : MediaProjection.Callback() {
                        override fun onStop() {
                            super.onStop()
                            stopMediaService()
                        }
                    }
                    mediaProjection.registerCallback(
                        mediaProjectionCallback,
                        Handler(Looper.getMainLooper())
                    )

                    configureMediaCapturingService()
                }
            }
        }

        return START_NOT_STICKY
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun configureMediaCapturingService() {


        val audioConfigBuilder =
            AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA).build()
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenShot",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
        val record = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        } else {
            val audioFormat = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(44100).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()
            sampleRate = audioFormat.sampleRate
            channels = if (audioFormat.channelCount == 1) 1 else 2 // 1 (mono)
            bitDepth = when (audioFormat.encoding) {
                AudioFormat.ENCODING_PCM_8BIT -> 8
                AudioFormat.ENCODING_PCM_16BIT -> 16
                AudioFormat.ENCODING_PCM_FLOAT -> 32
                else -> 16 // default to 16 if unknown
            }
            audioRecord = AudioRecord.Builder().setAudioFormat(audioFormat)
                .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
                .setAudioPlaybackCaptureConfig(audioConfigBuilder).build()


        }
    }

    fun createAudioFile(): File {
        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs() // makes entire directory along with parent directories
//            audioCapturesDirectory.mkdirs() makes the directory only if the  parent directories already exists
        }
        val timeStamp = SimpleDateFormat("dd-mm-yyyy-hh-mm-ss", Locale.US).format(Date())
        val filename = "capture-${timeStamp}.pcm"
        return File(audioCapturesDirectory.absolutePath + "/" + filename)

    }

    fun createAudioFile2(): File {
        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs() // makes entire directory along with parent directories
//            audioCapturesDirectory.mkdirs() makes the directory only if the  parent directories already exists
        }
        val timeStamp = SimpleDateFormat("dd-mm-yyyy-hh-mm-ss", Locale.US).format(Date())
        val filename = "capture-${timeStamp}123.pcm"
        return File(audioCapturesDirectory.absolutePath + "/" + filename)

    }


    fun writeToAudioFile(fileWithoutInjection: File) {
        try {
            var hasTimerStarted = false
            var data: Job? = null;
            FileOutputStream(fileWithoutInjection).use { fileOutputStreamWithoutInjection ->

                Log.d("in here==?", "in here")

                val capturedAudioArr = ShortArray(1024)

                var byteRead = 0
                while ((audioCaptureThread != null && !audioCaptureThread!!.isInterrupted)) {
                    if (pauseRecording) {
                        data?.cancel()
                        continue
                    }
                    val bytesRead = audioRecord!!.read(capturedAudioArr, 0, 1024)
                    if (isNewSSTaken) {
                        Log.d("called in here==>", "newsstaken")

                        try {
                            var ssFilename = newSSFileName // Example: your dynamic number as String


                            isNewSSTaken = false
                            newSSFileName = ""

                        } catch (e: Exception) {
                            isNewSSTaken = false
                            newSSFileName = ""
                        }
                    }

                    if (bytesRead > 0) {
                        val hasSound = capturedAudioArr.any { it != 0.toShort() }
                        if (!hasSound) {
                            if (isRecordingAudioAvailable) {
                                applicationContext.sendBroadcast(Intent(MainActivity.ACTION_AUDIO_NOT_AVAILABLE_INDICATOR))
                                isRecordingAudioAvailable = false
                            }
                            if (!hasTimerStarted) {
                                data = CoroutineScope(Dispatchers.Default).launch {
                                    hasTimerStarted = true
                                    delay(9000)
                                    applicationContext.sendBroadcast(Intent(MainActivity.ACTION_AUDIO_NOT_AVAILABLE))
                                }
                            }
                        } else {
                            Log.d("in here==>", "audio available")
                            if (data != null) data!!.cancel()
                            hasTimerStarted = false
                            if (!isRecordingAudioAvailable) {
                                applicationContext.sendBroadcast(Intent(MainActivity.ACTION_AUDIO_AVAILABLE_INDICATOR))
                                isRecordingAudioAvailable = true
                            }
                        }


                        val byteArray = capturedAudioArr.toByteArray()

                        fileOutputStreamWithoutInjection.write(
                            byteArray,
                            0,
                            bytesRead * 2
                        )


                        // *2 because short is 2 bytes

                        byteRead += bytesRead * 2
                    } else {
                        if (data != null) data!!.cancel()
                    }


                }
                if (data != null) data!!.cancel()
                CoroutineScope(Dispatchers.IO).launch {
                    try {

                        convertPcmToMp3(
                            fileWithoutInjection.name,
                            fileWithoutInjection.name.replace("pcm", "mp3"),
                            44100,
                            1,
                            context = applicationContext
                        )
                        val wavFileWithoutInjection = getAudioFile(
                            applicationContext,
                            fileWithoutInjection.name.replace("pcm", "mp3")
                        )
                        deletePCMAudioFile(applicationContext, fileWithoutInjection.name)
                        if (wavFileWithoutInjection != null) {

                            uploadFile(
                                headerText,
                                subHeaderText,
                                RecordTranscription,
                                wavFileWithoutInjection,
                                timestamps,saveContentAs
                            )
                        }

                    } catch (e: Exception) {
                        Log.e("AudioCaptureServicexxE", "Error occurred in file upload", e)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("AudioCaptureServicexxE", "Error in audio capture", e)
        }


    }


    fun startAudioCapturing() {
        audioRecord!!.startRecording()

        audioCaptureThread = thread(start = true) {
            val outputFile = createAudioFile()
            var outputFileForTranscription = createAudioFile2()

            writeToAudioFile(outputFile)

        }
    }

    fun stopAudioCapturing(header: String) {
        audioRecord!!.stop()
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }
        audioCaptureThread?.interrupt()
        audioCaptureThread?.join()
    }

    fun stopMediaService() {
        stopForeground(STOP_FOREGROUND_REMOVE) // Remove the notification
        stopSelf() // stops the media service
        virtualDisplay?.release()

    }

    fun takeSS(time: Int?) {
        try {
            val image = imageReader.acquireLatestImage()
            if (time != -1 && time != null) {
                timestamps.add(time.toString())
            }
            if (image != null) {

                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * metrics.widthPixels

                val bitmap = Bitmap.createBitmap(
                    metrics.widthPixels + rowPadding / pixelStride,
                    metrics.heightPixels,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Optional: Crop to actual screen width
//                val croppedBitmap =
//                    Bitmap.createBitmap(bitmap, 0, 0, metrics.widthPixels, metrics.heightPixels)
                val filename = System.currentTimeMillis().toString()
                val imageDirectory = File(baseContext.getExternalFilesDir(null), "ImageDirectory")
                if (!imageDirectory.exists()) {
                    imageDirectory.mkdirs()
                }
                val imageFile = File(imageDirectory, filename)


                // Save to file or return


                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                }
                isNewSSTaken = true
                newSSFileName = filename
                imagePathList.add(filename)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Log.e("Screenshot", "Failed to save image", e)
        }

    }


    private fun ShortArray.toByteArray(): ByteArray {
        val bytes = ByteArray(size * 2)
        for (i in 0 until size) {
            bytes[i * 2] = (this[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (this[i].toInt() shr 8).toByte()
            this[i] = 0
        }
        return bytes
    }

    suspend fun uploadFile(
        header: String, subHeader: String?, RecordTranscription:
        RecordTranscription, fileWithoutInjection: File?, timeStamps: MutableList<String?>,saveContentAs:String
    ) {

        var headerText = header
        var subHeaderText = subHeader
        if (header.contains("-1WxaX")) {

            if (fileWithoutInjection!!.exists()) {
                val duration = checkAudioDuration(fileWithoutInjection.absolutePath)
                if (duration < 19) {
                    fileWithoutInjection?.delete()
                    applicationContext.sendBroadcast(Intent(MainActivity.ACTION_DATA_SAVING_FAILED))
                    return
                }


            }
        }
        headerText = headerText.replace("-1WxaX", "")
        try {
            val prefs = applicationContext.user.data.first()
            val uidData = prefs[DataStoreKeys.USER_UID] ?: ""
            val sampleRatePart =
                MultipartBody.Part.createFormData("sampleRate", sampleRate.toString())
            val channelsPart = MultipartBody.Part.createFormData("channels", channels.toString())
            val bitDepthPart = MultipartBody.Part.createFormData("bitDepth", bitDepth.toString())
            val requestFile = fileWithoutInjection!!.asRequestBody("audio/pcm".toMediaTypeOrNull())

            val filePart =
                MultipartBody.Part.createFormData("file", fileWithoutInjection.name, requestFile)

            val uid = MultipartBody.Part.createFormData("uid", uidData)
            Log.d("timeStamps", timeStamps.joinToString(","))
            val saveContentAsPart =
                MultipartBody.Part.createFormData("saveContentAs", saveContentAs)
            val timestamps =
                MultipartBody.Part.createFormData("timestamps", timeStamps.joinToString(","))
            RecordTranscription.updateDetails(true, TranscriptionState.LOADING)
            var token = "123"
            val responseObj =
                RetrofitInstance.ApiServices.uploadFile(
                    appCheckToken = token,
                    file = filePart,
                    uid = uid,
                    timestamps = timestamps,
                    saveContentAs = saveContentAsPart
                )
            val response = responseObj.response
            val Used_Transcription_Duration =
                responseObj.Used_Transcription_Duration.toDouble().toLong()
            val total_Transcription_Duration =
                responseObj.Total_Transcription_Duration.toDouble().toLong()
            if (response != "") {
                Log.d("response==>", response)

                var responseText = response
                for (x in imagePathList) {
                    val imageDirectory =
                        File(applicationContext.getExternalFilesDir(null), "ImageDirectory")
                    val file = File(imageDirectory, x)
                    Log.d("file.absolutePath==>", file.absolutePath)
                    responseText = responseText.replaceFirst(
                        """<img src=""/>""",
                        """<img src="${file.absolutePath}"  />"""
                    )
                }
                Log.d("responseText==>", responseText)
                database.dao.instertAudioText(
                    convertAudioTextToAudioTextDbData(
                        AudioText(
                            text = responseText,
                            audioFileName = fileWithoutInjection!!.name,
                            imageTimestampList = timeStamps.joinToString(","),
                            header = headerText,
                            subHeader = subHeaderText,
                            flowType = FlowType.MediaCaptureService,
                            imageCollection = imagePathList,
                            isApiCallRequired = false
                        ),
                        true
                    )
                )

//                fileWithInjection.delete()
                applicationContext.user.edit {
                    it[DataStoreKeys.Used_Transcription_Duration] = Used_Transcription_Duration
                    it[DataStoreKeys.Total_Transcription_Duration] = total_Transcription_Duration
                }
                applicationContext.sendBroadcast(Intent(MainActivity.ACTION_DATA_SAVING_SUCCESS))
                RecordTranscription.updateDetails(false, TranscriptionState.SUCCESS)
            } else {
                RecordTranscription.updateDetails(false, TranscriptionState.PARTIALSUCCESS)
                database.dao.instertAudioText(
                    convertAudioTextToAudioTextDbData(
                        AudioText(
                            text = "Not Able To Create Text",
                            audioFileName = fileWithoutInjection!!.name,
                            header = headerText,
                            imageTimestampList = timeStamps.joinToString(","),
                            subHeader = subHeaderText,
                            flowType = FlowType.MediaCaptureService,
                            imageCollection = imagePathList,
                            isApiCallRequired = true
                        ), true
                    )
                )
                applicationContext.sendBroadcast(Intent(MainActivity.ACTION_DATA_SAVING_PARTIAL))
            }
        } catch (e: Throwable) {

            Log.d("e==>", e.stackTraceToString())

            RecordTranscription.updateDetails(false, TranscriptionState.FAILURE)
            database.dao.instertAudioText(
                convertAudioTextToAudioTextDbData(
                    AudioText(
                        text = "Not Able To Create Text",
                        audioFileName = fileWithoutInjection!!.name,
                        imageTimestampList = timeStamps.joinToString(","),
                        header = headerText,
                        subHeader = subHeaderText,
                        flowType = FlowType.MediaCaptureService,
                        imageCollection = imagePathList,
                        isApiCallRequired = true
                    ), true
                )
            )
            applicationContext.sendBroadcast(Intent(MainActivity.ACTION_DATA_SAVING_PARTIAL))
        } finally {
            imagePathList.clear()
            timeStamps.clear()
            timestamps.clear()
        }

    }


    private fun createNotification(): Notification {
        val channelId = "MediaCaptureService"
        val channelName = "Media Capture Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Media Capture Service")
            .setContentText("Recording system audio")
            .build()
    }

}

fun convertPcmToWav(
    pcmFileName: String,
    wavFileName: String,
    sampleRate: Int,
    channels: Int,
    bitDepth: Int,
    context: Context,

    ) {
    val pcmFile = getAudioFile(context, pcmFileName)
    val audioCapturesDirectory = File(context.getExternalFilesDir(null), "/AudioCaptures")
    val filename = wavFileName
    val wavFile = File(audioCapturesDirectory.absolutePath + "/" + filename)

    val pcmData = pcmFile!!.readBytes()
    val header = generateWavHeader(pcmData.size, sampleRate, channels, bitDepth)
    wavFile.outputStream().use { output ->
        output.write(header)
        output.write(pcmData)
    }
}

private fun generateWavHeader(
    dataSize: Int,
    sampleRate: Int,
    channels: Int,
    bitDepth: Int
): ByteArray {
    val byteRate = sampleRate * channels * (bitDepth / 8)
    val headerSize = 44
    val totalDataLen = dataSize + headerSize - 8
    val header = ByteArray(headerSize)

    header[0] = 'R'.code.toByte() // RIFF/WAVE header
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (totalDataLen and 0xff).toByte() // file-size (32-bit)
    header[5] = ((totalDataLen shr 8) and 0xff).toByte()
    header[6] = ((totalDataLen shr 16) and 0xff).toByte()
    header[7] = ((totalDataLen shr 24) and 0xff).toByte()
    header[8] = 'W'.code.toByte() // WAVE
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    header[12] = 'f'.code.toByte() // fmt chunk
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    header[16] = 16 // Sub-chunk size, 16 for PCM
    header[17] = 0
    header[18] = 0
    header[19] = 0
    header[20] = 1 // AudioFormat, 1 for PCM
    header[21] = 0
    header[22] = channels.toByte()
    header[23] = 0
    header[24] = (sampleRate and 0xff).toByte()
    header[25] = ((sampleRate shr 8) and 0xff).toByte()
    header[26] = ((sampleRate shr 16) and 0xff).toByte()
    header[27] = ((sampleRate shr 24) and 0xff).toByte()
    header[28] = (byteRate and 0xff).toByte()
    header[29] = ((byteRate shr 8) and 0xff).toByte()
    header[30] = ((byteRate shr 16) and 0xff).toByte()
    header[31] = ((byteRate shr 24) and 0xff).toByte()
    header[32] = (channels * (bitDepth / 8)).toByte() // block align
    header[33] = 0
    header[34] = bitDepth.toByte() // bits per sample
    header[35] = 0
    header[36] = 'd'.code.toByte() // data sub-chunk
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = (dataSize and 0xff).toByte()
    header[41] = ((dataSize shr 8) and 0xff).toByte()
    header[42] = ((dataSize shr 16) and 0xff).toByte()
    header[43] = ((dataSize shr 24) and 0xff).toByte()

    return header
}

fun getAudioFile(context: Context, fileName: String): File? {
    // Define the AudioCaptures directory within the external files directory
    val audioCapturesDirectory = File(context.getExternalFilesDir(null), "AudioCaptures")

    // Create the directory if it doesn't exist
    if (!audioCapturesDirectory.exists()) {
        return null
    }

    // Create the file path for the specified file name
    val file = File(audioCapturesDirectory, fileName)

    // Return the file if it exists, or null if it doesn't
    return if (file.exists()) file else null
}

fun deletePCMAudioFile(context: Context, fileName: String) {
    val audioCapturesDirectory = File(context.getExternalFilesDir(null), "AudioCaptures")

    val file = File(audioCapturesDirectory, fileName)
    if (file.exists()) {
        file.delete()
    }
}


fun videoNotAvailable(applicationContext: Context) {
    applicationContext.sendBroadcast(Intent(MainActivity.ACTION_AUDIO_NOT_AVAILABLE))
}


fun checkAudioDuration(filePath: String): Int {
    var mediaPlayer: MediaPlayer? = null
    fun getAudioDuration(filePath: String): Int {
        var durationInSec: Int = 0
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                val durationMs = duration // Get duration in milliseconds
                durationInSec = durationMs / 1000
            } catch (e: IOException) {
                Log.e("AudioService", "Error: ${e.message}")
            } finally {
                release() // Release MediaPlayer to free resources
                return durationInSec

            }
        }

    }
    return getAudioDuration(filePath)
}


fun checkIfScreenIsOff(display: DisplayManager, context: Context) {
    var timeInSecond = 0
    CoroutineScope(Dispatchers.Default).launch {
        while (true) {
            if (display.getDisplay(0).state == Display.STATE_OFF) {
                timeInSecond++
            } else {
                timeInSecond = 0
            }
            if (timeInSecond == 900) {
                val servicesToStop = listOf(
                    context::class.java,
                    FloatingWindow::class.java
                )
                for (service in servicesToStop) {
                    context.stopService(Intent(context, service))
                }
            }
            delay(1000)

        }
    }

}

fun convertPcmToMp3(
    pcmFileName: String,
    mp3FileName: String,
    sampleRate: Int,
    channels: Int,
    context: Context
) {
    val pcmFile = getAudioFile(context, pcmFileName)  // Your helper to get PCM file
    val audioCapturesDirectory = File(context.getExternalFilesDir(null), "/AudioCaptures")
    val mp3File = File(audioCapturesDirectory, mp3FileName)

    val command = arrayOf(
        "-f", "s16le",
        "-ar", sampleRate.toString(),
        "-ac", channels.toString(),
        "-i", pcmFile!!.absolutePath,
        "-codec:a", "libmp3lame",
        "-qscale:a", "2",
        mp3File.absolutePath
    )

    FFmpegKit.execute(command.joinToString(" "))
}