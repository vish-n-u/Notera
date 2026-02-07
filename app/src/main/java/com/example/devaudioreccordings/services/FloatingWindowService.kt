package com.example.devaudioreccordings.services


import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.print.PrintAttributes.Margins
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.devaudioreccordings.MainActivity
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


enum class RecordingResult {
    success, failure, partial
}

enum class SaveRecordingAs {
    transcript, summary
}

class FloatingWindow : Service() {
    private lateinit var floatView: ViewGroup
    private lateinit var floatWindowLayoutParams: WindowManager.LayoutParams
    private var LAYOUT_TYPE: Int? = null
    private lateinit var windowManager: WindowManager
    private lateinit var startRecording: ImageButton
    private lateinit var editText: EditText
    private lateinit var captureScreen: ImageButton
    private lateinit var playPauseRecording: ImageButton
    private lateinit var parentLayout: LinearLayout
    private lateinit var headerEditText: EditText
    private lateinit var saveRecordingAsTextView: TextView
    private lateinit var switchContainer: LinearLayout
    private lateinit var switchToTranscriptButton: RadioButton
    private lateinit var switchToSummaryButton: RadioButton
    private lateinit var headerTextCount: TextView
    private lateinit var subHeaderEditText: EditText
    private lateinit var subHeaderTextCount: TextView
    private var job: Job? = null
    var isInRecordingPhase = false // is true if recording is started , is in play or pause state
    var isRecordingInPlayPhase = false // is true if recording is on and in play phase
    private var isTimerRunning = false
    private var timeLeft = 600
    private var timerText = "10:00"
    var auidoNotAvialableDialogBox = false
    lateinit var broadCastReciever: BroadcastReceiver
    lateinit var stopFloatingWindow: ImageButton
    lateinit var returnBackToApp: ImageButton
    lateinit var timerRecordingIndicator: LinearLayout
    lateinit var timerTextLayout: TextView
    lateinit var timerAndHeaderLayout: LinearLayout
    lateinit var timerHeaderHideButton: ImageButton
    lateinit var recordingIndicatorButton: ImageButton
    private var fadeAnimator: ObjectAnimator? = null
    lateinit var bottomHideBar: LinearLayout
    var headerText = ""
    var subHeaderText = ""
    var jobForHidingEditText: Job? = null
    var isTimerAndHeaderHidden = false
    var showToast = true
    var saveContentAsTranscriptOrSumary = SaveRecordingAs.transcript


    fun showTimerAndHeaderText() {
        CoroutineScope(Dispatchers.Main).launch {
            slideDown(timerAndHeaderLayout)

            timerAndHeaderLayout.visibility = View.VISIBLE
            stopFloatingWindow.isEnabled = false
            stopFloatingWindow.alpha = 0.2f
            returnBackToApp.isEnabled = false
            returnBackToApp.alpha = 0.2f
            captureScreen.visibility = View.VISIBLE
        }
    }

    fun hideTimerAndHeaderText(stopVideoRecording: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if (timerAndHeaderLayout.visibility != View.GONE) {

                timerAndHeaderLayout.visibility = View.INVISIBLE
                delay(150)
                slideUp(timerAndHeaderLayout)
            }

//            timerAndHeaderLayout.visibility = View.GONE
            if (stopVideoRecording) {
                stopFloatingWindow.isEnabled = true
                returnBackToApp.isEnabled = true
                stopFloatingWindow.alpha = 1f
                returnBackToApp.alpha = 1f
                captureScreen.visibility = View.GONE

            }
        }
        return
    }

    fun startRecordingIndicatorFadeAnimation() {
        if (fadeAnimator == null) {
            fadeAnimator = ObjectAnimator.ofFloat(recordingIndicatorButton, "alpha", 1f, 0.3f, 1f)
            if (fadeAnimator != null) {
                fadeAnimator!!.duration = 1500L  // 1 second for full fade in/out cycle
                fadeAnimator!!.repeatCount = ValueAnimator.INFINITE
                fadeAnimator!!.repeatMode = ValueAnimator.RESTART
                fadeAnimator!!.start()
            }
        }
    }

    fun stopRecordingIndicatorFadeAnimation() {
        recordingIndicatorButton.alpha = 1f
        if (fadeAnimator != null) {
            fadeAnimator?.cancel()
            fadeAnimator = null
            recordingIndicatorButton.alpha = 1f
        }
    }

    fun audioNotAvailableRecordingIndicatorFadeAnimation() {
        recordingIndicatorButton.alpha = 0.4f
        if (fadeAnimator != null) {
            fadeAnimator?.cancel()
            fadeAnimator = null
        }
    }

    fun startRecordingFun() {
        playPauseRecording?.isVisible = true
        bottomHideBar.visibility = View.VISIBLE
        timerTextLayout.visibility = View.VISIBLE
        timerRecordingIndicator.visibility = View.VISIBLE
        startRecordingIndicatorFadeAnimation()

        val background = playPauseRecording.background as RippleDrawable
        background.state = intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed)
        val widthInDp = returnFloatToDp(startRecording, 6f)
        startRecording?.setPadding(widthInDp, widthInDp, widthInDp, widthInDp)



        updateImageButtonWidth(playPauseRecording, 30f)

        startRecording.setImageResource(R.drawable.tick)
        isInRecordingPhase = true
        isRecordingInPlayPhase = true

        startAudioCapture()
        startTimer()
        showTimerAndHeaderText()
//        jobForHidingEditText = checkFloatingWindowInteractionJob()

    }

    fun stopRecordingFun(headerText: String?, subHeaderText: String?) {

        val widthInDp = returnFloatToDp(startRecording, 0f)
        startRecording?.setPadding(widthInDp, widthInDp, widthInDp, widthInDp)
        updateImageButtonWidth(playPauseRecording, 0f)
        bottomHideBar.visibility = View.GONE
        timerTextLayout.visibility = View.GONE
        timerRecordingIndicator.visibility = View.GONE
        isInRecordingPhase = false
        isRecordingInPlayPhase = false
        playPauseRecording.setImageResource(R.drawable.pause)
        playPauseRecording?.isVisible = false
        startRecording.setImageResource(R.drawable.start_recording)
        stopRecordingIndicatorFadeAnimation()
        stopAudioCapture(headerText ?: "Anonymous", subHeaderText ?: "")
        stopTimer()
        hideTimerAndHeaderText(true)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("showToast0", showToast.toString())
            delay(2000)
            Log.d("showToast1", showToast.toString())
//            Toast.makeText(applicationContext, "Saving note quietly in the background...", Toast.LENGTH_SHORT).show()
            if (showToast) {
                showGlobalToast("Saving note quietly in the background...", 4000L, baseContext)
            }
            showToast = true
        }

        val isInternetAvailable = checkForInternet(baseContext)
        if (!isInternetAvailable) {
            openNoInternetBox()
        }

    }



    private fun slideUp(view: View, onEnd: (() -> Unit)? = null) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_up)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                view.visibility = View.GONE
                onEnd?.invoke()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        view.startAnimation(animation)
    }

    private fun slideDown(view: View) {
        view.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_down)
        view.startAnimation(animation)
    }

    fun pauseRecordingFun() {
        isRecordingInPlayPhase = false
        playPauseRecording.setImageResource(R.drawable.play)
        stopRecordingIndicatorFadeAnimation()
        pauseTimer()
        pauseAudioCapture()

    }

    fun playRecordingFun() {
        isRecordingInPlayPhase = true
        startRecordingIndicatorFadeAnimation()
        playPauseRecording.setImageResource(R.drawable.pause)
        resumeAudioCapture()
        startTimer()
        val isInternetAvailable = checkForInternet(baseContext)
        if (!isInternetAvailable) {
            openNoInternetBox()
        }
    }


    suspend fun changeBackgroundOfParent(resultState: String) {
        withContext(Dispatchers.Main) {
            if (RecordingResult.success.name == resultState) {
                parentLayout.background = ContextCompat.getDrawable(
                    baseContext,
                    R.drawable.rounded_background_with_glow
                )

                playSuccessSound(baseContext)
            }
            if (RecordingResult.failure.name == resultState) {
                parentLayout.background = ContextCompat.getDrawable(
                    baseContext,
                    R.drawable.rounded_background_with_red_glow
                )
                playFailSound(baseContext)

            }
            if (RecordingResult.partial.name == resultState) {
                parentLayout.background = ContextCompat.getDrawable(
                    baseContext,
                    R.drawable.rounded_background_with_yellow_glow
                )
            }
            delay(5000)
            parentLayout.background =
                ContextCompat.getDrawable(baseContext, R.drawable.rounded_background)
        }
    }

    fun openDialogBox(context: Context) {

        // Use the provided context (should be an Activity context)
//        val dialog = Dialog(baseContext)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.Blue.toArgb()))

        // Set up the ComposeView with the same context
        var stopRecordingFunctionIsCalledAlready = false
        val dialog = AlertDialog.Builder(baseContext)
            .setTitle("Audio Not Captured")
            .setMessage(
                "${getString(R.string.app_name_inside_app)} was unable to capture the audio. Please ensure the media is playing and the app allows audio capture.\n\n" +
                        "Note: Some apps restrict audio recording due to system-level limitations.\n\n" +
                        "You can alternatively use your device’s screen recorder and import the video using the 'Add Media' feature in Notera."
            )
            .setPositiveButton("Stop Recording") { dialogInterface: DialogInterface, which ->
                if (!stopRecordingFunctionIsCalledAlready) {
                    stopRecordingFun(headerText + "-1WxaX", subHeaderText)
                    stopRecordingFunctionIsCalledAlready = true
                }
            }.setOnDismissListener(DialogInterface.OnDismissListener {
                if (!stopRecordingFunctionIsCalledAlready) {
                    stopRecordingFun(headerText + "-1WxaX", subHeaderText)
                    stopRecordingFunctionIsCalledAlready = true
                }
            })
            .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.setCancelable(true)
            dialog.setCanceledOnTouchOutside(true)

        }, 3000)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!stopRecordingFunctionIsCalledAlready) {
                stopRecordingFun(headerText + "-1WxaX", subHeaderText)
                stopRecordingFunctionIsCalledAlready = true
            }
        }, 7000)

        // Show the dialog
        dialog.show()
    }

    fun openNoInternetBox() {
        // Configure and create the dialog
        val dialog = AlertDialog.Builder(baseContext)
            .setTitle("No Internet Available , don't worry your audio will still be saved")
            .setPositiveButton(
                "Okay",
                DialogInterface.OnClickListener { dialogInterface: DialogInterface, which ->
                })
            .create()

        dialog.setOnShowListener {
            // Style the positive button like a filled button
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton?.apply {
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                background = ContextCompat.getDrawable(context, R.drawable.alert_dialog)
                Margins(2, 8, 2, 8)

            }

            // Style the negative button like an outlined button


            // Make the title bold
            try {
                val titleView = dialog.findViewById<TextView>(android.R.id.title)
                titleView?.setTypeface(titleView.typeface, Typeface.BOLD)
            } catch (e: Exception) {
                // Handle exception if needed
            }
        }
        // Set the dialog window type to TYPE_APPLICATION_OVERLAY
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

        // Show the dialog
        dialog.show()

    }


    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReciever);
        if (::windowManager.isInitialized && ::floatView.isInitialized) {
            windowManager.removeView(floatView)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {
            sendBroadcast(Intent(MainActivity.ACTION_ORIENTATION_CHANGE_TO_LANDSCAPE))
        } else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {
            sendBroadcast(Intent(MainActivity.ACTION_ORIENTATION_CHANGE_TO_POTRAIT))
        }
    }

    override fun onCreate() {
        super.onCreate()
        val metrics = applicationContext.resources.displayMetrics
        val inflater = baseContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater



        floatView = inflater.inflate(R.layout.floating_window, null) as ViewGroup

        startRecording = floatView.findViewById(R.id.start_recording)
        playPauseRecording = floatView.findViewById(R.id.playpause_recording)
        stopFloatingWindow = floatView.findViewById(R.id.stop_app)
        headerEditText = floatView.findViewById(R.id.header_edit_text)
        subHeaderEditText = floatView.findViewById(R.id.sub_header_edit_text)
        timerAndHeaderLayout = floatView.findViewById(R.id.timerAndHeaderLayout)
        timerTextLayout = floatView.findViewById(R.id.timer)
        saveRecordingAsTextView = floatView.findViewById(R.id.recording_mode_badge)
        switchContainer = floatView.findViewById(R.id.recording_mode_selector)
        switchToTranscriptButton = floatView.findViewById(R.id.radio_transcript)
        switchToSummaryButton = floatView.findViewById(R.id.radio_summary)
        timerRecordingIndicator = floatView.findViewById(R.id.timer_recording_indicator)
        timerHeaderHideButton = floatView.findViewById(R.id.timer_header_visibility)
        switchContainer = floatView.findViewById(R.id.recording_mode_selector)


        recordingIndicatorButton = floatView.findViewById(R.id.recording_indicator)
        headerTextCount = floatView.findViewById(R.id.header_count)
        subHeaderTextCount = floatView.findViewById(R.id.sub_header_count)
        parentLayout = floatView.findViewById(R.id.parent_layout)
        returnBackToApp = floatView.findViewById(R.id.back_to_app)
        bottomHideBar = floatView.findViewById(R.id.bottom_hide_bar)
        captureScreen = floatView.findViewById(R.id.capture_ss)

        headerEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // EditText lost focus, add back FLAG_NOT_FOCUSABLE
                floatWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(floatView, floatWindowLayoutParams)
            } else {
                floatWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(floatView, floatWindowLayoutParams)
            }
        }
        headerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // You can use this for real time changes that do not modify the edit text.
            }

            override fun afterTextChanged(s: Editable?) {
                headerText = s.toString().trim()
                if (headerText.length > 30) {
                    headerText = headerText.substring(0, 30) // Trim to 30 characters
                    headerEditText.setText(headerText)
                    headerEditText.setSelection(headerText.length) // Move cursor to end
                }
                headerTextCount.setText(headerText.length.toString() + "/" + 30)

            }
        })
        captureScreen.setOnClickListener { view ->
            CoroutineScope(Dispatchers.Main).launch {
                parentLayout.visibility = View.GONE
                delay(400)
                val intent = Intent(MainActivity.ACTION_TAKE_SCREENSHOT)
                intent.putExtra("timestamp",600-timeLeft)
                sendBroadcast(intent)
                CoroutineScope(Dispatchers.Main).launch {
                    playCameraShutter(baseContext)
                }
                delay(400)
                parentLayout.visibility = View.VISIBLE
                delay(1000)
                animateIconColor(captureScreen)

            }

        }

        fun showSelectedButton(button: Button) {
            button.setBackgroundResource(R.drawable.switch_button_selected)
            button.setTextColor(Color.WHITE)
        }

        fun showDeSelectedButton(button: Button) {

            button.setBackgroundResource(R.drawable.switch_button_unselected)
            button.setTextColor(Color.BLACK)
        }
        switchToTranscriptButton.setOnClickListener {
//            showSelectedButton(switchToTranscriptButton)
//            showDeSelectedButton(switchToSummaryButton)
            saveContentAsTranscriptOrSumary = SaveRecordingAs.transcript
            saveRecordingAsTextView.text = "Transcript"
            saveRecordingAsTextView.setBackgroundResource(R.drawable.mode_badge_background)
            timerTextLayout.setTextColor(getResources().getColor(R.color.timer_recording_indicator_text_color_Transcription))
            saveRecordingAsTextView.setTextColor(getResources().getColor(R.color.timer_recording_indicator_text_color_Transcription))
            timerRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.timer_recording_indicator_background_color_Transcription))
        }

        switchToSummaryButton.setOnClickListener {
//            showDeSelectedButton(switchToTranscriptButton)
//            showSelectedButton(switchToSummaryButton)
            saveContentAsTranscriptOrSumary = SaveRecordingAs.summary
            saveRecordingAsTextView.text = "Summary"
            saveRecordingAsTextView.setTextColor(getResources().getColor(R.color.timer_recording_indicator_text_color_Summary))
            saveRecordingAsTextView.setBackgroundResource(R.drawable.yellow_mode_badge_background)
            timerTextLayout.setTextColor(getResources().getColor(R.color.timer_recording_indicator_text_color_Summary))
            timerRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.timer_recording_indicator_background_color_Summary))
        }


        subHeaderEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // EditText lost focus, add back FLAG_NOT_FOCUSABLE
                floatWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(floatView, floatWindowLayoutParams)
            } else {
                floatWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                windowManager.updateViewLayout(floatView, floatWindowLayoutParams)
            }
        }

// Your existing click listener remains the same


        subHeaderEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // You can use this for real time changes that do not modify the edit text.
            }

            override fun afterTextChanged(s: Editable?) {
                subHeaderText = s.toString().trim()
                if (subHeaderText.length > 30) {
                    subHeaderText = subHeaderText.substring(0, 30) // Trim to 30 characters
                    subHeaderEditText.setText(subHeaderText)
                    subHeaderEditText.setSelection(subHeaderText.length) // Move cursor to end
                }
                subHeaderTextCount.setText(subHeaderText.length.toString() + "/" + 30)

            }
        })

        timerHeaderHideButton.setOnClickListener {
            if (isTimerAndHeaderHidden) {
                timerHeaderHideButton.setImageResource(R.drawable.up_arrow)
                showTimerAndHeaderText()
                isTimerAndHeaderHidden = false
            } else {
                timerHeaderHideButton.setImageResource(R.drawable.down_arrow)
                hideTimerAndHeaderText(false)
                isTimerAndHeaderHidden = true
            }

        }

        startRecording.setOnClickListener {
            if (!isInRecordingPhase) {
                startRecordingFun()
            } else {
                headerText = headerEditText.text.toString().trim()
                subHeaderText = subHeaderEditText.text.toString().trim()
                stopRecordingFun(headerText, subHeaderText)
                hideTimerAndHeaderText(false)
            }
        }

        playPauseRecording.setOnClickListener {
            if (!isRecordingInPlayPhase) {
                playRecordingFun()
            } else pauseRecordingFun()
        }
        stopFloatingWindow.setOnClickListener {
            if (isInRecordingPhase) {
                headerText = headerEditText.text.toString()
                subHeaderText = subHeaderEditText.text.toString()
                stopRecordingFun(headerText, subHeaderText)
                hideTimerAndHeaderText(true)
            }

            sendBroadcast(Intent(MainActivity.ACTION_STOP_MEDIACAPTURE_SERVICE))
            stopSelf()

            // Stop other services
            val servicesToStop = listOf(
                this::class.java
            )
            for (service in servicesToStop) {
                stopService(Intent(this, service))
            }

            // Kill the app process


        }

        returnBackToApp.setOnClickListener {
            val context = baseContext
            if (isInRecordingPhase) {
                headerText = headerEditText.text.toString().trim()
                subHeaderText = subHeaderEditText.text.toString().trim()
                stopRecordingFun(headerText, subHeaderText)
                hideTimerAndHeaderText(true)
            }
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }






        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun RegisterBroadcastReciever() {
            broadCastReciever = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {

                    when (intent?.action) {
                        MainActivity.ACTION_AUDIO_NOT_AVAILABLE -> {
                            openDialogBox(context!!)
                            // if(pauseRecording) pauseRecording=false
                            // startAudioCapturing()
                            // showDialogBox()
                        }

                        MainActivity.ACTION_DATA_SAVING_SUCCESS -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                val state = RecordingResult.success.name
                                showToast = true
                                changeBackgroundOfParent(state)
                            }
                        }

                        MainActivity.ACTION_DATA_SAVING_FAILED -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                val state = RecordingResult.failure.name
                                showToast = false
                                changeBackgroundOfParent(state)
                            }
                        }

                        MainActivity.ACTION_DATA_SAVING_PARTIAL -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                val state = RecordingResult.partial.name
                                showToast = true
                                changeBackgroundOfParent(state)
                            }
                        }

                        MainActivity.ACTION_AUDIO_NOT_AVAILABLE_INDICATOR -> {
                            audioNotAvailableRecordingIndicatorFadeAnimation()
                        }

                        MainActivity.ACTION_AUDIO_AVAILABLE_INDICATOR -> {
                            startRecordingIndicatorFadeAnimation()

                        }
                    }
                }
            }

            val intentFilter = IntentFilter().apply {
                addAction(MainActivity.ACTION_AUDIO_NOT_AVAILABLE)
                addAction(MainActivity.ACTION_DATA_SAVING_PARTIAL)
                addAction(MainActivity.ACTION_DATA_SAVING_SUCCESS)
                addAction(MainActivity.ACTION_DATA_SAVING_FAILED)
                addAction(MainActivity.ACTION_AUDIO_NOT_AVAILABLE_INDICATOR)
                addAction(MainActivity.ACTION_AUDIO_AVAILABLE_INDICATOR)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(broadCastReciever, intentFilter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(broadCastReciever, intentFilter)
            }
        }

        RegisterBroadcastReciever()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_TOAST
        } else {
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        floatWindowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_TYPE!!,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // Add this flag
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSPARENT

        )
        floatWindowLayoutParams.gravity = Gravity.LEFT
        floatWindowLayoutParams.x = 0
        floatWindowLayoutParams.y = 0


        floatWindowLayoutParams.gravity = Gravity.LEFT
        floatWindowLayoutParams.x = 0
        floatWindowLayoutParams.y = 0

        windowManager.addView(floatView, floatWindowLayoutParams)

        floatView.setOnTouchListener(object : View.OnTouchListener {
            val updatedFloatingWindowLayoutParams = floatWindowLayoutParams
            var x = 0.0
            var y = 0.0
            var newX = 0.0
            var newY = 0.0
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event!!.action == MotionEvent.ACTION_OUTSIDE) {
                    floatWindowLayoutParams.flags =
                        floatWindowLayoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowManager.updateViewLayout(floatView, floatWindowLayoutParams)

                }

                if (event!!.action == MotionEvent.ACTION_DOWN) {
                    floatWindowLayoutParams.flags =
                        floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    windowManager.updateViewLayout(floatView, floatWindowLayoutParams)
                }

                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = updatedFloatingWindowLayoutParams.x.toDouble()
                        y = updatedFloatingWindowLayoutParams.y.toDouble()
                        newX = event.rawX.toDouble()
                        newY = event.rawY.toDouble()

                    }

                    MotionEvent.ACTION_MOVE -> {
                        updatedFloatingWindowLayoutParams.x =
                            (x + event.rawX.toDouble() - newX).toInt()
                        updatedFloatingWindowLayoutParams.y =
                            (y + event.rawY.toDouble() - newY).toInt()
                        windowManager.updateViewLayout(floatView, updatedFloatingWindowLayoutParams)

                    }
                }
                return false
            }

        })


    }

    private fun formatTime(timeInSeconds: Int): String {
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    suspend fun updateText() {
        timerText = formatTime(timeLeft)
        withContext(Dispatchers.Main) {
            timerTextLayout.text = timerText

        }
    }

    private fun startTimer() {
        isTimerRunning = true
        job = CoroutineScope(Dispatchers.Default).launch {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
                updateText()
            }
            CoroutineScope(Dispatchers.Main).launch {
                stopRecordingFun(headerText, subHeaderText)
            }

        }

    }

    private fun stopTimer() {
        job!!.cancel()
        isTimerRunning = false
        timeLeft = 600
        CoroutineScope(Dispatchers.Default).launch {
            updateText()
        }
    }

    private fun pauseTimer() {
        isTimerRunning = false
        job!!.cancel()

    }


    fun startAudioCapture() {
        applicationContext.sendBroadcast(Intent(MainActivity.ACTION_START_AUDIOCAPTURE))
    }

    fun stopAudioCapture(headerText: String = "Anonymous", subHeaderText: String?) {
        val intent = Intent(MainActivity.ACTION_STOP_AUDIOCAPTURE)
        intent.putExtra("header", headerText)
        intent.putExtra("subHeader", subHeaderText)
        intent.putExtra("contentType", saveContentAsTranscriptOrSumary.name)

        applicationContext.sendBroadcast(intent)
    }

    fun pauseAudioCapture() {
        applicationContext.sendBroadcast(Intent(MainActivity.ACTION_PAUSE_AUDIOCAPTURE))
    }

    fun resumeAudioCapture() {
        applicationContext.sendBroadcast(Intent(MainActivity.ACTION_RESUME_AUDIOCAPTURE))
    }

    fun stopApp() {
        applicationContext.sendBroadcast(Intent(MainActivity.ACTION_STOP_APP))
    }


}

private fun checkForInternet(context: Context): Boolean {

    // register activity with the connectivity manager service
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // if the android version is equal to M
    // or greater we need to use the
    // NetworkCapabilities to check what type of
    // network has the internet connection

    // Returns a Network object corresponding to
    // the currently active default data network.
    val network = connectivityManager.activeNetwork ?: return false

    // Representation of the capabilities of an active network.
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

    return when {
        // Indicates this network uses a Wi-Fi transport,
        // or WiFi has network connectivity
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

        // Indicates this network uses a Cellular transport. or
        // Cellular has network connectivity
        activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

        // else return false
        else -> false
    }
}

private fun updateImageButtonWidth(imageButton: ImageButton, widthInDp: Float) {
    // Convert dp to pixels
    val widthInPx = returnFloatToDp(imageButton, widthInDp)

    // Update the layoutParams
    val layoutParams = imageButton.layoutParams
    layoutParams.width = widthInPx
    layoutParams.height = widthInPx
    imageButton.layoutParams = layoutParams
}


private fun returnFloatToDp(imageButton: ImageButton, widthInDp: Float): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        widthInDp,
        imageButton.context.resources.displayMetrics
    ).toInt()

}


suspend fun playSuccessSound(context: Context) {
    try {
        CoroutineScope(Dispatchers.IO).launch {
            var mediaPlayer = MediaPlayer.create(context, R.raw.success_bell).apply {
                setVolume(1f, 1f)
            }
            mediaPlayer.start()
            delay(3000)
            mediaPlayer.release()

        }
    } catch (e: Throwable) {
        Log.d("e==>", e.message.toString())
    }
}

suspend fun playFailSound(context: Context) {
    try {
        CoroutineScope(Dispatchers.IO).launch {
            var mediaPlayer = MediaPlayer.create(context, R.raw.spin_fail).apply {
                setVolume(0.3f, 0.3f)
            }
            mediaPlayer.start()
            delay(3000)
            mediaPlayer.release()

        }
    } catch (e: Throwable) {
        Log.d("e==>", e.message.toString())
    }
}

suspend fun playCameraShutter(context: Context) {
    try {
        CoroutineScope(Dispatchers.IO).launch {
            var mediaPlayer = MediaPlayer.create(context, R.raw.camera_capture2).apply {
                setVolume(1f, 1f)
            }
            mediaPlayer.start()
            delay(3000)
            mediaPlayer.release()

        }
    } catch (e: Throwable) {
        Log.d("e==>", e.message.toString())
    }
}


private fun animateIconColor(imageButton: ImageButton) {
    val fromColor = Color.BLACK
    val toColor = Color.GREEN

    val colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 600  // time to go from black to green
        repeatMode = ValueAnimator.REVERSE
        repeatCount = 1

        addUpdateListener { animator ->
            val fraction = animator.animatedFraction
            val blendedColor = ArgbEvaluator().evaluate(fraction, fromColor, toColor) as Int

            imageButton.drawable.setTint(blendedColor)
        }
    }

    colorAnimator.start()
}


fun showGlobalToast(message: String, duration: Long = 6000L, baseContext: Context) {
    val density = baseContext.resources.displayMetrics.density

    val toastLayout = LinearLayout(baseContext).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(
            (24 * density).toInt(), // 24dp
            (16 * density).toInt(), // 16dp
            (24 * density).toInt(), // 24dp
            (16 * density).toInt()  // 16dp
        )
        setBackgroundResource(R.drawable.toast_background)
        alpha = 0f
        gravity = Gravity.CENTER_VERTICAL
        minimumWidth = (150 * density).toInt() // 200dp minimum width
        minimumHeight = (48 * density).toInt() // 48dp minimum height
        elevation = (4 * density) // 4dp elevation for shadow
    }

    val icon = ImageView(baseContext).apply {
        setImageResource(R.mipmap.ic_launcher)
        layoutParams = LinearLayout.LayoutParams(
            (28 * density).toInt(), // 24dp width
            (28 * density).toInt()  // 24dp height
        ).apply {
            rightMargin = (16 * density).toInt() // 16dp margin
            gravity = Gravity.CENTER_VERTICAL
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val text = TextView(baseContext).apply {
        this.text = message
        setTextColor(Color.WHITE)
        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        textSize = 14f // 14sp
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Ensure SP units
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        // Add some line spacing for better readability
        setLineSpacing(2f, 1.1f)
    }

    toastLayout.addView(icon)
    toastLayout.addView(text)

    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    params.y = (120 * density).toInt() // Convert to dp

    val wm = baseContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.addView(toastLayout, params)

    // Fade in animation
    toastLayout.animate()
        .alpha(1f)
        .setDuration(300)
        .setInterpolator(android.view.animation.DecelerateInterpolator())
        .start()

    // Auto dismiss with fade out animation
    Handler(Looper.getMainLooper()).postDelayed({
        toastLayout.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                try {
                    wm.removeView(toastLayout)
                } catch (_: Exception) {
                    // Handle case where view was already removed
                }
            }.start()
    }, duration)
}

