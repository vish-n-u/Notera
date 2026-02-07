package com.example.devaudioreccordings.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.devaudioreccordings.MainActivity
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.database.AudioTextDao
import com.example.devaudioreccordings.database.AudioTextDatabase
import com.example.devaudioreccordings.database.FlowType
import com.example.devaudioreccordings.viewModals.convertAudioTextToAudioTextDbData
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingTextWindowService : Service() {
    lateinit var database : AudioTextDatabase
    lateinit var dao:AudioTextDao
    lateinit var windowManager: WindowManager
    private lateinit var floatView: ViewGroup
    private var LAYOUT_TYPE: Int? = null
    private lateinit var floatWindowLayoutParams: WindowManager.LayoutParams
    private lateinit var parentLayout: LinearLayout
    private lateinit var headerEditText: EditText
    private lateinit var subHeaderEditText: EditText
    private lateinit var bodyEditText: EditText
    private lateinit var headerTextCount: TextView
    private lateinit var subHeaderTextCount: TextView
    lateinit var stopFloatingWindow: ImageButton
    lateinit var movableWindow: LinearLayout
    lateinit var returnBackToApp: ImageButton
    lateinit var bottomHideBar: LinearLayout
    lateinit var bottomCloseButton: ImageButton
    lateinit var timerAndHeaderLayout: LinearLayout
    lateinit var saveTextButton: Button
    lateinit var clearTextButton: ImageButton
    var header = ""
    var subHeaderTitle = ""
    var bodyText = ""
    var isAboveLayoutHidden = false
    var isReziseButtonClicked = false
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized && ::floatView.isInitialized) {
            windowManager.removeView(floatView)
        }
    }

    override fun onCreate() {
        startForeground(1, createNotification())

        super.onCreate()
        database = AudioTextDatabase.getDatabase(context = this)
        dao = database.dao
        val metrics = applicationContext.resources.displayMetrics
        val inflater = baseContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager



        floatView = inflater.inflate(R.layout.floating_text_window, null) as ViewGroup
        parentLayout = floatView.findViewById(R.id.parent_layout)
        returnBackToApp = floatView.findViewById(R.id.back_to_app)
        movableWindow = floatView.findViewById(R.id.movable_layout)
        stopFloatingWindow = floatView.findViewById(R.id.stop_app)
        headerEditText = floatView.findViewById(R.id.header_edit_text)
        subHeaderEditText = floatView.findViewById(R.id.sub_header_edit_text)
        bottomHideBar = floatView.findViewById(R.id.bottom_hide_bar)
        bottomCloseButton = floatView.findViewById(R.id.bottom_close_button)
        headerTextCount = floatView.findViewById(R.id.header_count)
        subHeaderTextCount = floatView.findViewById(R.id.sub_header_count)
        bodyEditText = floatView.findViewById(R.id.body_edit_text)
        saveTextButton = floatView.findViewById(R.id.save_button)
        clearTextButton = floatView.findViewById(R.id.clear_text_button)
        timerAndHeaderLayout = floatView.findViewById(R.id.timerAndHeaderLayout)




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




        bodyEditText.setOnFocusChangeListener { _, hasFocus ->
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
                if (floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0) {
                    floatWindowLayoutParams.flags =
                        floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
//                update()
                }
                // Show soft keyboard
            }
        }

        bodyEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                bodyText = s.toString()
            }
        })

// Your existing click listener remains the same

        headerEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // You can use this for real time changes that do not modify the edit text.
            }

            override fun afterTextChanged(s: Editable?) {
                header = s.toString().trim()
                if (header.length > 30) {
                    header = header.substring(0, 30) // Trim to 30 characters
                    headerEditText.setText(header)
                    headerEditText.setSelection(header.length) // Move cursor to end
                }
                headerTextCount.setText(header.length.toString() + "/" + 30)

            }
        })

        subHeaderEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // You can use this for real time changes that do not modify the edit text.
            }

            override fun afterTextChanged(s: Editable?) {
                subHeaderTitle = s.toString().trim()
                if (subHeaderTitle.length > 50) {
                    subHeaderTitle = subHeaderTitle.substring(0, 50) // Trim to 30 characters
                    subHeaderEditText.setText(subHeaderTitle)
                    subHeaderEditText.setSelection(subHeaderTitle.length) // Move cursor to end
                }
                subHeaderTextCount.setText(subHeaderTitle.length.toString() + "/" + 50)

            }
        })
        saveTextButton.setOnClickListener {
            showGlobalToast("Saving note quietly in the background...",4000L , baseContext)
            CoroutineScope(Dispatchers.IO).launch {
                dao.instertAudioText(
                    convertAudioTextToAudioTextDbData(
                        AudioText(
                            text = bodyText,
                            header = if(header.length>0)header else "Floating Clipboard Header",
                            subHeader = subHeaderTitle,
                            audioFileName = null,
                            flowType = FlowType.AddText,
                            imageCollection = null,
                            isApiCallRequired = false,


                            ), true
                    )
                )
                CoroutineScope(Dispatchers.Main).launch {
                    parentLayout.background = ContextCompat.getDrawable(
                        baseContext,
                        R.drawable.rounded_background_with_glow
                    )
                    playSuccessSound(baseContext)
                    delay(4000)
                    bodyEditText.setText("")
                    bodyText = ""
                    parentLayout.background =
                        ContextCompat.getDrawable(baseContext, R.drawable.rounded_background)

                }
            }


        }
        clearTextButton.setOnClickListener {
            bodyEditText.setText("")
            bodyText = ""
        }
        returnBackToApp.setOnClickListener {
            val context = baseContext
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        stopFloatingWindow.setOnClickListener {
            stopSelf()
        }
        bottomCloseButton.setOnClickListener {
            if (isAboveLayoutHidden) {
                movableWindow.visibility = View.VISIBLE
                headerEditText.visibility = View.VISIBLE
                headerTextCount.visibility = View.VISIBLE
                subHeaderTextCount.visibility = View.VISIBLE
                subHeaderEditText.visibility = View.VISIBLE
                saveTextButton.visibility = View.VISIBLE
                isAboveLayoutHidden = false
                bottomCloseButton.setImageResource(R.drawable.up_arrow)
            } else {
                movableWindow.visibility = View.INVISIBLE
                headerEditText.visibility = View.GONE
                headerTextCount.visibility = View.GONE
                saveTextButton.visibility = View.GONE
                subHeaderTextCount.visibility = View.GONE
                subHeaderEditText.visibility = View.GONE
                isAboveLayoutHidden = true
                bottomCloseButton.setImageResource(R.drawable.down_arrow)
            }
        }

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
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSPARENT

        )
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
                    bodyEditText.clearFocus()
                    subHeaderEditText.clearFocus()
                    headerEditText.clearFocus()

                    windowManager.updateViewLayout(floatView, floatWindowLayoutParams)

                }

                if (event!!.action == MotionEvent.ACTION_DOWN) {
                    floatWindowLayoutParams.flags =
                        floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
//                update()
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

        fun enableKeyboard() {
            if (floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE != 0) {
                floatWindowLayoutParams.flags =
                    floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
//                update()
            }
        }

        fun disableKeyboard() {
            if (floatWindowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE == 0) {
                floatWindowLayoutParams.flags =
                    floatWindowLayoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                update()
            }
        }


    }

    private fun createNotification(): Notification {
        val channelId = "FloatingTextWindowServiceChannel" // More descriptive ID
        val channelName = "Floating Text Window Notifications" // More descriptive name
        val ACTION_STOP_SERVICE = "com.myapp.ridescribe"
        // Importance level: IMPORTANCE_LOW is suitable for ongoing background tasks
        // that the user is aware of but don't require immediate attention.
        // Consider IMPORTANCE_DEFAULT or IMPORTANCE_HIGH if the floating window is highly interactive or prominent.
        val importance = NotificationManager.IMPORTANCE_LOW
        val channelDescription = "Notifications for the floating text window service." // Add a description

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                // Optional: Customize notification behavior further
                // enableLights(false)
                // enableVibration(false)
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch when the user taps the notification.
        // Replace YourMainActivity::class.java with the activity you want to open.
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java), // Replace with your main activity
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )

        // Intent for the Stop action
        val stopServiceIntent = Intent(this, FloatingTextWindowService::class.java).apply { // Replace with your BroadcastReceiver or Service
            action = ACTION_STOP_SERVICE
        }

        val stopServicePendingIntent = PendingIntent.getBroadcast( // Use getService if sending to a Service
            this,
            0,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
        )

        // Create the Stop action
        val stopAction = NotificationCompat.Action.Builder(
            // Use a relevant icon for the stop action, e.g., android.R.drawable.ic_delete
            android.R.drawable.ic_delete, // Placeholder icon
            "Stop Service", // Text for the action button
            stopServicePendingIntent
        ).build()

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Text Window Active") // More informative title
            .setContentText("Tap to open window settings or stop the service.") // More informative text
            .setSmallIcon(R.drawable.app_logo) // **Required:** Add a small icon
            .setContentIntent(contentIntent) // Set the intent for tapping the notification
            .addAction(stopAction) // Add the stop action
            .setOngoing(true) // Indicates an ongoing background task
            .setAutoCancel(false) // Prevent notification from being dismissed when tapped
            // Set priority for devices running Android 7.1 and lower (API < 26)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Corresponds to IMPORTANCE_LOW
            .build()
    }

}

