package com.example.devaudioreccordings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.toMutableStateList
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.text.toHtml
import androidx.core.view.WindowCompat
import androidx.core.view.allViews
import androidx.lifecycle.lifecycleScope
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.glideloader.GlideImageLoader
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.example.devaudioreccordings.wordpress_comments.HiddenGutenbergPlugin
import com.myapp.ridescribe.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecAttributes
import org.wordpress.aztec.AztecExceptionHandler
import org.wordpress.aztec.AztecText
import org.wordpress.aztec.Html
import org.wordpress.aztec.IHistoryListener
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.plugins.BackgroundColorButton
import org.wordpress.aztec.plugins.CssBackgroundColorPlugin
import org.wordpress.aztec.plugins.CssUnderlinePlugin
import org.wordpress.aztec.plugins.IMediaToolbarButton
import org.wordpress.aztec.source.SourceViewEditText
import org.wordpress.aztec.toolbar.AztecToolbar
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener
import org.wordpress.aztec.toolbar.ToolbarAction
import org.wordpress.aztec.toolbar.ToolbarItems
import org.wordpress.aztec.util.AztecLog
import org.xml.sax.Attributes
import java.io.File
import java.io.FileOutputStream
import java.util.Random

open class AztecView : AppCompatActivity(),
    AztecText.OnImeBackListener,
    AztecText.OnImageTappedListener,
    AztecText.OnVideoTappedListener,
    AztecText.OnAudioTappedListener,
    AztecText.OnMediaDeletedListener,
    AztecText.OnVideoInfoRequestedListener,
    IAztecToolbarClickListener,
    IHistoryListener,
    OnRequestPermissionsResultCallback,
    PopupMenu.OnMenuItemClickListener,
    View.OnTouchListener {

    companion object {
        private val HEADING =
            "<h1>Heading A</h1>" +
                    "<h2>Heading 2</h2>" +
                    "<h3>Heading 3</h3>" +
                    "<h4>Heading 4</h4>" +
                    "<h5>Heading 5</h5>" +
                    "<h6>Heading 6</h6>"
        private val BOLD = "<b>Bold</b><br>"
        private val ITALIC = "<i style=\"color:darkred\">Italic</i><br>"
        private val UNDERLINE = "<u style=\"color:lime\">Underline</u><br>"
        private val BACKGROUND =
            "<span style=\"background-color:#005082\">BACK<b>GROUND</b></span><br>"
        private val STRIKETHROUGH =
            "<s style=\"color:#ff666666\" class=\"test\">Strikethrough</s><br>" // <s> or <strike> or <del>
        private val ORDERED =
            "<ol style=\"color:green\"><li>Ordered</li><li>should have color</li></ol>"
        private val TASK_LIST = "<ul type=\"task-list\">\n" +
                " <li><input type=\"checkbox\" class=\"task-list-item-checkbox\">Unchecked</li>\n" +
                " <li><input type=\"checkbox\" class=\"task-list-item-checkbox\" checked>Checked</li>\n" +
                "</ul>"
        private val ORDERED_WITH_START = "<h4>Start in 10 List:</h4>" +
                "<ol start=\"10\">\n" +
                "    <li>Ten</li>\n" +
                "    <li>Eleven</li>\n" +
                "    <li>Twelve</li>\n" +
                "</ol>"
        private val ORDERED_REVERSED = "<h4>Reversed List:</h4>" +
                "<ol reversed>\n" +
                "    <li>Three</li>\n" +
                "    <li>Two</li>\n" +
                "    <li>One</li>\n" +
                "</ol>"
        private val ORDERED_REVERSED_WITH_START = "<h4>Reversed Start in 10 List:</h4>" +
                "<ol reversed start=\"10\">\n" +
                "    <li>Ten</li>\n" +
                "    <li>Nine</li>\n" +
                "    <li>Eight</li>\n" +
                "</ol>"
        private val ORDERED_REVERSED_NEGATIVE_WITH_START = "<h4>Reversed Start in 1 List:</h4>" +
                "<ol reversed start=\"1\">\n" +
                "    <li>One</li>\n" +
                "    <li>Zero</li>\n" +
                "    <li>Minus One</li>\n" +
                "</ol>"
        private val ORDERED_REVERSED_WITH_START_IDENT = "<h4>Reversed Start in 6 List:</h4>" +
                "<ol reversed>" +
                "   <li>Six</li>" +
                "   <li>Five</li>" +
                "   <li>Four</li>" +
                "   <li>Three</li>" +
                "   <li>Two</li>" +
                "   <li>One<ol>" +
                "   <li>One</li>" +
                "   <li>Two</li>" +
                "   <li>Three</li>" +
                "   <li>Four</li>" +
                "   <li>Five</li>" +
                "   <li>Six</li>" +
                "   <li>Seven</li> " +
                "   </ol></li></ol>"
        private val LINE = "<hr />"
        private val UNORDERED =
            "<ul><li style=\"color:darkred\">Unordered</li><li>Should not have color</li></ul>"
        private val QUOTE = "<blockquote>Quote</blockquote>"
        private val LINK =
            "<a href=\"https://github.com/wordpress-mobile/WordPress-Aztec-Android\">Link</a><br>"
        private val UNKNOWN = "<iframe class=\"classic\">Menu</iframe><br>"
        private val COMMENT = "<!--Comment--><br>"
        private val COMMENT_MORE = "<!--more--><br>"
        private val COMMENT_PAGE = "<!--nextpage--><br>"
        private val HIDDEN =
            "<span></span>" +
                    "<div class=\"first\">" +
                    "    <div class=\"second\">" +
                    "        <div class=\"third\">" +
                    "            Div<br><span><b>Span</b></span><br>Hidden" +
                    "        </div>" +
                    "        <div class=\"fourth\"></div>" +
                    "        <div class=\"fifth\"></div>" +
                    "    </div>" +
                    "    <span class=\"second last\"></span>" +
                    "</div>" +
                    "<br>"
        private val GUTENBERG_CODE_BLOCK = "<!-- wp:core/image {\"id\":316} -->\n" +
                "<figure class=\"wp-block-image\"><img src=\"https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/WordPress_blue_logo.svg/1200px-WordPress_blue_logo.svg.png\" alt=\"\" />\n" +
                "  <figcaption>The WordPress logo!</figcaption>\n" +
                "</figure>\n" +
                "<!-- /wp:core/image -->"
        private val PREFORMAT =
            "<pre>" +
                    "when (person) {<br>" +
                    "    MOCTEZUMA -> {<br>" +
                    "        print (\"friend\")<br>" +
                    "    }<br>" +
                    "    CORTES -> {<br>" +
                    "        print (\"foe\")<br>" +
                    "    }<br>" +
                    "}" +
                    "</pre>"
        private val CODE = "<code>if (value == 5) printf(value)</code><br>"
        private val IMG =
            "[caption align=\"alignright\"]<img src=\"data:image/jpeg;base64,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\" />Caption[/caption]"
        private val EMOJI = "&#x1F44D;"
        private val NON_LATIN_TEXT = "测试一个"
        private val LONG_TEXT =
            "<br><br>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "
        private val VIDEO =
            "[video src=\"https://examplebloge.files.wordpress.com/2017/06/d7d88643-88e6-d9b5-11e6-92e03def4804.mp4\"]"
        private val AUDIO =
            "[audio src=\"https://upload.wikimedia.org/wikipedia/commons/9/94/H-Moll.ogg\"]"
        private val VIDEOPRESS = "[wpvideo OcobLTqC]"
        private val VIDEOPRESS_2 = "[wpvideo OcobLTqC w=640 h=400 autoplay=true html5only=true3]"
        private val QUOTE_RTL = "<blockquote>לְצַטֵט<br>same quote but LTR</blockquote>"
        private val MARK =
            "<p>Donec ipsum dolor, <mark style=\"color:#ff0000\">tempor sed</mark> bibendum <mark style=\"color:#1100ff\">vita</mark>.</p>"

        private val EXAMPLE =
            IMG +
                    HEADING +
                    BOLD +
                    ITALIC +
                    UNDERLINE +
                    BACKGROUND +
                    STRIKETHROUGH +
                    TASK_LIST +
                    ORDERED +
                    ORDERED_WITH_START +
                    ORDERED_REVERSED +
                    ORDERED_REVERSED_WITH_START +
                    ORDERED_REVERSED_NEGATIVE_WITH_START +
                    ORDERED_REVERSED_WITH_START_IDENT +
                    LINE +
                    UNORDERED +
                    QUOTE +
                    PREFORMAT +
                    LINK +
                    HIDDEN +
                    COMMENT +
                    COMMENT_MORE +
                    COMMENT_PAGE +
                    CODE +
                    UNKNOWN +
                    EMOJI +
                    NON_LATIN_TEXT +
                    LONG_TEXT +
                    VIDEO +
                    VIDEOPRESS +
                    VIDEOPRESS_2 +
                    AUDIO +
                    GUTENBERG_CODE_BLOCK +
                    QUOTE_RTL +
                    MARK

        private val isRunningTest: Boolean by lazy {
            try {
                Class.forName("androidx.test.espresso.Espresso")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }

    private val MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE: Int = 1001
    private val MEDIA_CAMERA_VIDEO_PERMISSION_REQUEST_CODE: Int = 1002
    private val MEDIA_PHOTOS_PERMISSION_REQUEST_CODE: Int = 1003
    private val MEDIA_VIDEOS_PERMISSION_REQUEST_CODE: Int = 1004
    private val REQUEST_MEDIA_CAMERA_PHOTO: Int = 2001
    private val REQUEST_MEDIA_CAMERA_VIDEO: Int = 2002
    private val REQUEST_MEDIA_PHOTO: Int = 2003
    private val REQUEST_MEDIA_VIDEO: Int = 2004
    private lateinit var fabSaveButton: ImageButton
    protected lateinit var aztec: Aztec
    private lateinit var mediaFile: String
    private lateinit var mediaPath: String
    private lateinit var audioData: AudioText
    private lateinit var invalidateOptionsHandler: Handler
    private var skipSaveCheck = false
    private lateinit var invalidateOptionsRunnable: Runnable

    private var mediaUploadDialog: AlertDialog? = null
    private var mediaMenu: PopupMenu? = null

    private var mIsKeyboardOpen = false
    private var mHideActionBarOnSoftKeyboardUp = false
    val appViewModel: AppViewModel by viewModels()


    private fun insertImageAndSimulateUpload(bitmap: Bitmap?, mediaPath: String) {
        val bitmapResized = getScaledBitmapAtLongestSide(bitmap, aztec.visualEditor.maxImagesWidth)
        val (id, attrs) = generateAttributesForMedia(mediaPath, isVideo = false)
        aztec.visualEditor.insertImage(BitmapDrawable(resources, bitmapResized), attrs)
        insertMediaAndSimulateUpload(id, attrs)
        aztec.toolbar.toggleMediaToolbar()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_MEDIA_CAMERA_PHOTO -> {
                    // By default, BitmapFactory.decodeFile sets the bitmap's density to the device default so, we need
                    //  to correctly set the input density to 160 ourselves.
                    val options = BitmapFactory.Options()
                    options.inDensity = DisplayMetrics.DENSITY_DEFAULT
                    val bitmap = BitmapFactory.decodeFile(mediaPath, options)
                    Log.d("MediaPath", mediaPath)
                    insertImageAndSimulateUpload(bitmap, mediaPath)
                }

                REQUEST_MEDIA_PHOTO -> {
                    val imageUri = data?.data
                    val filename = "${System.currentTimeMillis()}.jpg"
                    val imageDirectory =
                        File(applicationContext.getExternalFilesDir(null), "ImageDirectory")

                    if (!imageDirectory.exists()) {
                        imageDirectory.mkdirs()
                    }

                    val destFile = File(imageDirectory, filename)
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val outputStream = FileOutputStream(destFile)

                    inputStream.use { input ->
                        outputStream.use { output ->
                            input!!.copyTo(output)
                        }
                    }

                    val mediaPath = destFile.absolutePath

                    val bitmap = BitmapFactory.decodeFile(mediaPath)

                    insertImageAndSimulateUpload(bitmap, mediaPath)
                }


                REQUEST_MEDIA_CAMERA_VIDEO -> {
                    mediaPath = data?.data.toString()
                }

                REQUEST_MEDIA_VIDEO -> {
                    mediaPath = data?.data.toString()

                    aztec.visualEditor.videoThumbnailGetter?.loadVideoThumbnail(
                        mediaPath,
                        object : Html.VideoThumbnailGetter.Callbacks {
                            override fun onThumbnailFailed() {
                            }

                            override fun onThumbnailLoaded(drawable: Drawable?) {
                                val conf = Bitmap.Config.ARGB_8888 // see other conf types
                                val bitmap = Bitmap.createBitmap(
                                    drawable!!.intrinsicWidth,
                                    drawable.intrinsicHeight,
                                    conf
                                )
                                val canvas = Canvas(bitmap)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)

//                                    insertVideoAndSimulateUpload(bitmap, mediaPath)
                            }

                            override fun onThumbnailLoading(drawable: Drawable?) {
                            }
                        },
                        this.resources.displayMetrics.widthPixels
                    )
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun generateAttributesForMedia(
        mediaPath: String,
        isVideo: Boolean
    ): Pair<String, AztecAttributes> {
        val id = Random().nextInt(Integer.MAX_VALUE).toString()
        val attrs = AztecAttributes()
        attrs.setValue(
            "src",
            mediaPath
        ) // Temporary source value.  Replace with URL after uploaded.
        attrs.setValue("id", id)
        attrs.setValue("uploading", "true")

        if (isVideo) {
            attrs.setValue("video", "true")
        }

        return Pair(id, attrs)
    }

    private fun insertMediaAndSimulateUpload(id: String, attrs: AztecAttributes) {
        val predicate = object : AztecText.AttributePredicate {
            override fun matches(attrs: Attributes): Boolean {
                return attrs.getValue("id") == id
            }
        }

        aztec.visualEditor.setOverlay(predicate, 0, ColorDrawable(0x80000000.toInt()), Gravity.FILL)
        aztec.visualEditor.updateElementAttributes(predicate, attrs)

        val progressDrawable =
            AppCompatResources.getDrawable(this, android.R.drawable.progress_horizontal)!!
        // set the height of the progress bar to 2 (it's in dp since the drawable will be adjusted by the span)
        progressDrawable.setBounds(0, 0, 0, 4)

        aztec.visualEditor.setOverlay(
            predicate,
            1,
            progressDrawable,
            Gravity.FILL_HORIZONTAL or Gravity.TOP
        )
        aztec.visualEditor.updateElementAttributes(predicate, attrs)

        var progress = 0

        // simulate an upload delay
        val runnable = Runnable {
            aztec.visualEditor.setOverlayLevel(predicate, 1, progress)
            aztec.visualEditor.updateElementAttributes(predicate, attrs)
            aztec.visualEditor.resetAttributedMediaSpan(predicate)
            progress += 2000

            if (progress >= 10000) {
                attrs.removeAttribute(attrs.getIndex("uploading"))
                aztec.visualEditor.clearOverlays(predicate)

                if (attrs.hasAttribute("video")) {
                    attrs.removeAttribute(attrs.getIndex("video"))
                    aztec.visualEditor.setOverlay(
                        predicate,
                        0,
                        AppCompatResources.getDrawable(this, android.R.drawable.ic_media_play),
                        Gravity.CENTER
                    )
                }

                aztec.visualEditor.updateElementAttributes(predicate, attrs)
            }
        }

        Handler(Looper.getMainLooper()).post(runnable)
        Handler(Looper.getMainLooper()).postDelayed(runnable, 2000)
        Handler(Looper.getMainLooper()).postDelayed(runnable, 4000)
        Handler(Looper.getMainLooper()).postDelayed(runnable, 6000)
        Handler(Looper.getMainLooper()).postDelayed(runnable, 8000)

        aztec.visualEditor.refreshText()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aztec_editor)
        val dataId = intent.getStringExtra("id")
        fabSaveButton = findViewById(R.id.fab_save)
        WindowCompat.setDecorFitsSystemWindows(window, true)


        val fullData = appViewModel.fullData

        fabSaveButton.setOnClickListener {
            skipSaveCheck = true
            saveChanges()
            onBackPressed()

        }
        fun runCheck() {
            lifecycleScope.launch {
                while (true) {
                    delay(500)
                    val currentSpanned = aztec.visualEditor.text.toString().trim()
                    val savedSpanned =
                        HtmlCompat.fromHtml(audioData?.text ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
                            .toString().trim()
                    if (aztec.visualEditor.toHtml() != audioData!!.text) {
                        fabSaveButton.isEnabled = true
//                        fabSaveButton.backgroundTintList = ColorStateList.valueOf(Color.YELLOW)
                        fabSaveButton.alpha = 1f

                    } else {
                        fabSaveButton.isEnabled = false
                        fabSaveButton.alpha = 0.3f
//                        fabSaveButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
                    }
                }
            }
        }
        runCheck()
        fun toggleHighlight(button: ToggleButton) {
            val selectionStart = aztec.visualEditor.selectionStart
            val selectionEnd = aztec.visualEditor.selectionEnd

            if (selectionStart == selectionEnd) {
                // No text selected → do nothing
                button.isChecked = false
                return
            }

            val editable = aztec.visualEditor.text
            Log.d("val==>", editable.toHtml())
            val spans =
                editable.getSpans(selectionStart, selectionEnd, BackgroundColorSpan::class.java)

            if (spans.isNotEmpty()) {
                // If already highlighted → remove spans
                for (span in spans) {
                    editable.removeSpan(span)
                }
                button.isChecked = false
            } else {
                // Add highlight span
                editable.setSpan(
                    BackgroundColorSpan(android.graphics.Color.YELLOW),
                    selectionStart,
                    selectionEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                button.isChecked = true
            }
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mIsKeyboardOpen = false
                showActionBarIfNeeded()

                // Disable the callback temporarily to allow the system to handle the back pressed event. This usage
                // breaks predictive back gesture behavior and should be reviewed before enabling the predictive back
                // gesture feature.
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })

        // Setup hiding the action bar when the soft keyboard is displayed for narrow viewports
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ) {
            mHideActionBarOnSoftKeyboardUp = true
        }

        val visualEditor = findViewById<AztecText>(R.id.aztec)
        val sourceEditor = findViewById<SourceViewEditText>(R.id.source)
        val toolbar = findViewById<AztecToolbar>(R.id.formatting_toolbar)

        visualEditor.enableSamsungPredictiveBehaviorOverride()


        visualEditor.externalLogger = object : AztecLog.ExternalLogger {
            override fun log(message: String) {
            }

            override fun logException(tr: Throwable) {
            }

            override fun logException(tr: Throwable, message: String) {
            }
        }


//

        val galleryButton = MediaToolbarGalleryButton(toolbar)
        galleryButton.setMediaToolbarButtonClickListener(object :
            IMediaToolbarButton.IMediaToolbarClickListener {
            override fun onClick(view: View) {
                mediaMenu = PopupMenu(this@AztecView, view)
                mediaMenu?.setOnMenuItemClickListener(this@AztecView)
                mediaMenu?.inflate(R.menu.menu_gallery)
                mediaMenu?.show()
                if (view is ToggleButton) {
                    view.isChecked = false
                }
            }
        })

        val cameraButton = MediaToolbarCameraButton(toolbar)
        cameraButton.setMediaToolbarButtonClickListener(object :
            IMediaToolbarButton.IMediaToolbarClickListener {
            override fun onClick(view: View) {
                mediaMenu = PopupMenu(this@AztecView, view)
                mediaMenu?.setOnMenuItemClickListener(this@AztecView)
//                mediaMenu?.inflate(R.menu.menu_camera)
                mediaMenu?.show()
                if (view is ToggleButton) {
                    view.isChecked = false
                }
            }
        })

        aztec = Aztec.with(visualEditor, toolbar, this)
            .setImageGetter(GlideImageLoader(this))
            .setOnImeBackListener(this)
            .setOnTouchListener(this)
            .setHistoryListener(this)
            .setOnImageTappedListener(this)
            .setOnVideoTappedListener(this)
            .setOnAudioTappedListener(this)
            .addOnMediaDeletedListener(this)
            .setOnVideoInfoRequestedListener(this)
            .addPlugin(HiddenGutenbergPlugin(visualEditor))
            .addPlugin(galleryButton)
//            .addPlugin(cameraButton)
        findViewById<ToggleButton>(ToolbarAction.LINK.buttonId).visibility = View.GONE

        findViewById<ToggleButton>(ToolbarAction.QUOTE.buttonId).visibility = View.GONE
//        findViewById<ToggleButton>(ToolbarAction.HORIZONTAL_RULE.buttonId).visibility = View.GONE
        var toolbarItems = ToolbarItems.defaultAdvancedLayout
        val highlightButton = ToggleButton(this).apply {
            id = View.generateViewId()
            text = "HL" // Or use an icon
            textOn = "HL"
            textOff = "HL"
            // Optional: style it
        }

// Add it to your toolbar layout
//        toolbar.addView(highlightButton)
//        highlightButton.setOnClickListener {
//            Log.d("val==>0","called in here")
//            toggleHighlight(highlightButton)
//        }


        lifecycleScope.launch {
            if (dataId != null) {
                audioData = appViewModel.returnDataBasedOnId2(fullData.value, dataId.toInt())!!
            }


            audioData?.let { Log.d("audioData.text==>", it.text) }
            Log.d("id==>", dataId.toString())
            aztec.sourceEditor?.displayStyledAndFormattedHtml(
                audioData?.text
                    ?: ""
            )
            aztec.visualEditor.fromHtml(audioData?.text ?: "")
            aztec.initSourceEditorHistory()
        }

        // initialize the plugins, text & HTML
        if (true) {
            aztec.visualEditor.enableCrashLogging(object :
                AztecExceptionHandler.ExceptionHandlerHelper {
                override fun shouldLog(ex: Throwable): Boolean {
                    return true
                }
            })
            aztec.visualEditor.setCalypsoMode(false)
            aztec.sourceEditor?.setCalypsoMode(false)

            aztec.visualEditor.setBackgroundSpanColor(
                ContextCompat.getColor(
                    this,
                    R.color.purple_700
                )
            )

            aztec.sourceEditor?.displayStyledAndFormattedHtml(EXAMPLE)

            aztec.addPlugin(CssUnderlinePlugin())
            aztec.addPlugin(CssBackgroundColorPlugin())
            aztec.addPlugin(BackgroundColorButton(visualEditor))
        }


        Log.d("toolbarviews", toolbar.allViews.joinToString(" ,"))

        if (savedInstanceState == null) {
            if (false) {
                aztec.visualEditor.fromHtml(EXAMPLE)
            }
            aztec.initSourceEditorHistory()
        }

        invalidateOptionsHandler = Handler(Looper.getMainLooper())
        invalidateOptionsRunnable = Runnable { invalidateOptionsMenu() }
    }


    override fun onPause() {
        super.onPause()
        mIsKeyboardOpen = false
    }

    override fun onResume() {
        super.onResume()

        showActionBarIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        aztec.visualEditor.disableCrashLogging()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)


    }

    override fun onBackPressed() {
        if(skipSaveCheck) {
            super.onBackPressed()
        }
        else if(aztec.visualEditor.toHtml() != audioData.text) {
            showSaveChangesDialog {
                super.onBackPressed()

            }
        }
        else super.onBackPressed()
    }

    private fun showSaveChangesDialog(callOnBackPressed: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Unsaved Changes")
        builder.setMessage("You have unsaved changes. Do you want to save before leaving?")

        builder.setPositiveButton("Save") { dialog, _ ->
            // Replace with your save logic
            saveChanges()
            dialog.dismiss()
            callOnBackPressed()
        }

        builder.setNegativeButton("Discard") { dialog, _ ->
            dialog.dismiss()
            super.onBackPressed()
        }

        builder.setCancelable(true)
        builder.show()
    }

    private fun saveChanges() {
        val imageList = audioData.imageCollection
        var newImageList = extractImageSrcs(html = aztec.visualEditor.toHtml())
        Log.d("newImageList", newImageList.joinToString(","))
        imageList?.joinToString(",")?.let { Log.d("imageList", it) }
        val imageDirectory = File(applicationContext.getExternalFilesDir(null), "ImageDirectory")
        imageList?.map { existingImageFilename ->
            if (!newImageList.contains(existingImageFilename)) {
                val existingFile = File(imageDirectory, existingImageFilename)
                existingFile.delete()
            }

        }
        val newAudioData = audioData.copy(
            text = aztec.visualEditor.toHtml(),
            imageCollection = newImageList.toMutableStateList()
        )
        appViewModel.updateData(newAudioData)

    }

    fun extractImageSrcs(html: String): List<String> {
        val imageDirectory = File(applicationContext.getExternalFilesDir(null), "ImageDirectory")
        val regex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"]")
        return regex.findAll(html)
            .map { it.groupValues[1] }
            .toList().map { it ->
                it.replace(imageDirectory.absolutePath + "/", "")

            }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        aztec.initSourceEditorHistory()

        if (savedInstanceState.getBoolean("isMediaUploadDialogVisible")) {
            showMediaUploadDialog()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (mediaUploadDialog != null && mediaUploadDialog!!.isShowing) {
            outState.putBoolean("isMediaUploadDialogVisible", true)
        }
    }

    /**
     * Returns true if a hardware keyboard is detected, otherwise false.
     */
    private fun isHardwareKeyboardPresent(): Boolean {
        val config = resources.configuration
        var returnValue = false
        if (config.keyboard != Configuration.KEYBOARD_NOKEYS) {
            returnValue = true
        }
        return returnValue
    }

    private fun hideActionBarIfNeeded() {

        val actionBar = supportActionBar
        if (actionBar != null
            && !isHardwareKeyboardPresent()
            && mHideActionBarOnSoftKeyboardUp
            && mIsKeyboardOpen
            && actionBar.isShowing
        ) {
            actionBar.hide()
        }
    }

    /**
     * Show the action bar if needed.
     */
    private fun showActionBarIfNeeded() {

        val actionBar = supportActionBar
        if (actionBar != null && !actionBar.isShowing) {
            actionBar.show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            // If the WebView or EditText has received a touch event, the keyboard will be displayed and the action bar
            // should hide
            mIsKeyboardOpen = true
            hideActionBarIfNeeded()
        }
        return false
    }

    /**
     * Intercept back button press while soft keyboard is visible.
     */
    override fun onImeBack() {
        mIsKeyboardOpen = false
        showActionBarIfNeeded()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true) // Re-enable title
            title = "Edit Page"
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.undo ->
                if (aztec.visualEditor.visibility == View.VISIBLE) {
                    aztec.visualEditor.undo()
                } else {
                    aztec.sourceEditor?.undo()
                }

            R.id.redo ->
                if (aztec.visualEditor.visibility == View.VISIBLE) {
                    aztec.visualEditor.redo()
                } else {
                    aztec.sourceEditor?.redo()
                }

            else -> {
            }
        }

        return true
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.redo)?.isEnabled = aztec.visualEditor.history.redoValid()
        menu?.findItem(R.id.undo)?.isEnabled = aztec.visualEditor.history.undoValid()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onRedoEnabled() {
        invalidateOptionsHandler.removeCallbacks(invalidateOptionsRunnable)
        invalidateOptionsHandler.postDelayed(
            invalidateOptionsRunnable,
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        )
    }

    override fun onUndoEnabled() {
        invalidateOptionsHandler.removeCallbacks(invalidateOptionsRunnable)
        invalidateOptionsHandler.postDelayed(
            invalidateOptionsRunnable,
            resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        )
    }

    override fun onUndo() {}

    override fun onRedo() {}

    private fun onCameraPhotoMediaOptionSelected() {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(
                this,
                MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE
            )
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mediaFile = "wp-" + System.currentTimeMillis()
                mediaPath = File.createTempFile(
                    mediaFile,
                    ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ).absolutePath

            } else {
                mediaFile = "wp-" + System.currentTimeMillis() + ".jpg"
                @Suppress("DEPRECATION")
                mediaPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .toString() +
                            File.separator + "Camera" + File.separator + mediaFile
            }
            intent.putExtra(
                MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider", File(mediaPath)
                )
            )

            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, REQUEST_MEDIA_CAMERA_PHOTO)
            }
        }
    }

    private fun onCameraVideoMediaOptionSelected() {
        if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(
                this,
                MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE
            )
        ) {
            val intent = Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)

            if (intent.resolveActivity(packageManager) != null) {
                startActivityForResult(intent, REQUEST_MEDIA_CAMERA_VIDEO)
            }
        }
    }

    private fun onPhotosMediaOptionSelected() {
        if (true) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"

            try {
                startActivityForResult(intent, REQUEST_MEDIA_PHOTO)
            } catch (exception: ActivityNotFoundException) {
                Log.e("error==>", exception.message.toString())
                Toast.makeText(this, "Chosen photo has an issue", Toast.LENGTH_LONG)
            }
        }
    }

    private fun onVideosMediaOptionSelected() {
        if (PermissionUtils.checkAndRequestStoragePermission(
                this,
                MEDIA_PHOTOS_PERMISSION_REQUEST_CODE
            )
        ) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "video/*"

            try {
                startActivityForResult(intent, REQUEST_MEDIA_VIDEO)
            } catch (exception: ActivityNotFoundException) {
                Log.e("error==>", exception.message.toString())
                Toast.makeText(this, "Not Able to get video", Toast.LENGTH_LONG)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE,
            MEDIA_CAMERA_VIDEO_PERMISSION_REQUEST_CODE -> {
                var isPermissionDenied = false

                for (i in grantResults.indices) {
                    when (permissions[i]) {
                        Manifest.permission.CAMERA -> {
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                isPermissionDenied = true
                            }
                        }

                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                isPermissionDenied = true
                            }
                        }
                    }
                }

                if (isPermissionDenied) {
//                    ToastUtils.showToast(this, getString(R.string.permission_required_media_camera))
                } else {
                    when (requestCode) {
                        MEDIA_CAMERA_PHOTO_PERMISSION_REQUEST_CODE -> {
                            onCameraPhotoMediaOptionSelected()
                        }

                        MEDIA_CAMERA_VIDEO_PERMISSION_REQUEST_CODE -> {
                            onCameraVideoMediaOptionSelected()
                        }
                    }
                }
            }

            MEDIA_PHOTOS_PERMISSION_REQUEST_CODE,
            MEDIA_VIDEOS_PERMISSION_REQUEST_CODE -> {
                var isPermissionDenied = false

                for (i in grantResults.indices) {
                    when (permissions[i]) {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                isPermissionDenied = true
                            }
                        }
                    }
                }

                when (requestCode) {
                    MEDIA_PHOTOS_PERMISSION_REQUEST_CODE -> {
                        if (isPermissionDenied) {
//                            ToastUtils.showToast(this, getString(R.string.permission_required_media_photos))
                        } else {
                            onPhotosMediaOptionSelected()
                        }
                    }

                    MEDIA_VIDEOS_PERMISSION_REQUEST_CODE -> {
                        if (isPermissionDenied) {
//                            ToastUtils.showToast(this, getString(R.string.permission_required_media_videos))
                        } else {
                            onVideosMediaOptionSelected()
                        }
                    }
                }
            }

            else -> {
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onToolbarCollapseButtonClicked() {
    }

    override fun onToolbarExpandButtonClicked() {
    }

    override fun onToolbarFormatButtonClicked(format: ITextFormat, isKeyboardShortcut: Boolean) {
//        ToastUtils.showToast(this, format.toString())
    }

    override fun onToolbarHeadingButtonClicked() {
    }

    override fun onToolbarHtmlButtonClicked() {
        val uploadingPredicate = object : AztecText.AttributePredicate {
            override fun matches(attrs: Attributes): Boolean {
                return attrs.getIndex("uploading") > -1
            }
        }

        val mediaPending =
            aztec.visualEditor.getAllElementAttributes(uploadingPredicate).isNotEmpty()

        if (mediaPending) {
            Toast.makeText(this, "Media is uploading", Toast.LENGTH_LONG)
        } else {
            aztec.toolbar.toggleEditorMode()
        }
    }

    override fun onToolbarListButtonClicked() {
    }

    override fun onToolbarMediaButtonClicked(): Boolean {
        return false
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        item?.isChecked = (item?.isChecked == false)

        return when (item?.itemId) {
            R.id.take_photo -> {
                onCameraPhotoMediaOptionSelected()
                true
            }

            R.id.take_video -> {
                onCameraVideoMediaOptionSelected()
                true
            }

            R.id.gallery_photo -> {
                onPhotosMediaOptionSelected()
                true
            }

            R.id.gallery_video -> {
                onVideosMediaOptionSelected()
                true
            }

            else -> false
        }
    }

    private fun showMediaUploadDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("media is uploading")
        builder.setPositiveButton("Upload", null)
        mediaUploadDialog = builder.create()
        mediaUploadDialog!!.show()
    }

    override fun onImageTapped(attrs: AztecAttributes, naturalWidth: Int, naturalHeight: Int) {
//        ToastUtils.showToast(this, "Image tapped!")
    }

    override fun onVideoTapped(attrs: AztecAttributes) {

    }

    override fun onVideoInfoRequested(attrs: AztecAttributes) {

    }

    override fun onAudioTapped(attrs: AztecAttributes) {
        val url = attrs.getValue("src")
        url?.let {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.setDataAndType(Uri.parse(url), "audio/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                } catch (e: ActivityNotFoundException) {
//                    ToastUtils.showToast(this, "Audio tapped!")
                }
            }
        }
    }

    override fun onMediaDeleted(attrs: AztecAttributes) {
        val url = attrs.getValue("src")
//        ToastUtils.showToast(this, "Media Deleted! " + url)
    }

}




fun getScaledBitmapAtLongestSide(bitmap: Bitmap?, maxSize: Int): Bitmap? {
    if (bitmap == null) return null

    val width = bitmap.width
    val height = bitmap.height

    if (width <= 0 || height <= 0) return bitmap

    val longestSide = maxOf(width, height)
    if (longestSide <= maxSize) {
        // No need to scale, it's already smaller than max size
        return bitmap
    }

    val scaleFactor = maxSize.toFloat() / longestSide
    val newWidth = (width * scaleFactor).toInt()
    val newHeight = (height * scaleFactor).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}


object BuildConfig {
    val DEBUG: Boolean = "true".toBoolean()
    const val APPLICATION_ID: String = "com.example.devaudioreccordings"
    const val BUILD_TYPE: String = "debug"
    const val VERSION_CODE: Int = 1
    const val VERSION_NAME: String = "1.0"
}
