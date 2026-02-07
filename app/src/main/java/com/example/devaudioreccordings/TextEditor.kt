
import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.coroutines.delay

/**
Be sure that you have those two dependencies:
// Rich Text Editor
implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-beta03")
// Extension Icons
implementation("androidx.compose.material:material-icons-extended:1.5.3")
 */
data class EditorState(
    val boldSelected: Boolean = false,
    val italicSelected: Boolean = false,
    val underlineSelected: Boolean = false,
    val titleSelected: Boolean = false,
    val subtitleSelected: Boolean = false,
    val textColorSelected: Boolean = false,
    val highlightSelected: Boolean = false,
    val alignmentSelected: Int = 0 // 0: start, 1: center, 2: end
)


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TextEditorRich(
    text: String = "",
    saveChanges: (text: String) -> Unit,
    transcriptionText: MutableState<String>,
    hasTranscriptionTextChanged: MutableState<Boolean>,
    state: RichTextState
) {
    var editorState by remember { mutableStateOf(EditorState()) }
    val interactionData = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    var showFullScreenEditor by remember { mutableStateOf(false) }


    // Initialize editor content and styles
    LaunchedEffect(text) {
        if (text.isNotEmpty()) {
            state.setHtml(text)
        }
        if (state.toHtml().isEmpty()) {
            state.toggleSpanStyle(SpanStyle(fontSize = 22.sp))
        }
    }

    // Track changes to HTML reliably
//    LaunchedEffect(Unit) {
//        snapshotFlow { state.annotatedString }
//            .map { state.toHtml() }
//            .distinctUntilChanged()
//            .debounce(300)
//            .collect { html ->
//
//                hasTranscriptionTextChanged.value = transcriptionText.value != html
//            }
//    }

    LaunchedEffect(state.toHtml().length) {
        Log.d("text change","detected")
    }


    LaunchedEffect(Unit) {
        while (true) {
            // Your code here
            Log.d("state==>",state.toHtml())
            Log.d("state==>",state.toHtml().length.toString())
            transcriptionText.value = state.toHtml()
            delay(500)
        }
    }

    val titleSize = MaterialTheme.typography.titleLarge.fontSize
    val subtitleSize = MaterialTheme.typography.titleMedium.fontSize

    UpdateEditorState(
        state = state,
        titleSize = titleSize,
        subtitleSize = subtitleSize,
        onStateUpdate = { editorState = it }
    )

    // Full-screen editor dialog
    if (showFullScreenEditor) {
        FullScreenEditorDialog(
            state = state,
            editorState = editorState,
            titleSize = titleSize,
            subtitleSize = subtitleSize,
            onStateUpdate = { editorState = it },
            saveChanges = saveChanges,
            onDismiss = { showFullScreenEditor = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        // Controls UI
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            EditorControls(
                modifier = Modifier.padding(16.dp),
                state = state,
                editorState = editorState,
                titleSize = titleSize,
                subtitleSize = subtitleSize,
                onBoldClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                onItalicClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                onUnderlineClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                onTitleClick = { state.toggleSpanStyle(SpanStyle(fontSize = titleSize)) },
                onSubtitleClick = { state.toggleSpanStyle(SpanStyle(fontSize = subtitleSize)) },
                onTextColorClick = { state.toggleSpanStyle(SpanStyle(color = Color.Red)) },
                onHighlightClick = {
                    val highlightColor = Color.Yellow.copy(alpha = 0.7f)
                    state.toggleSpanStyle(SpanStyle(background = highlightColor))
                    // Force recomposition by updating a dummy state
                    editorState = editorState.copy()
                },
                onStartAlignClick = { state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Start)) },
                onCenterAlignClick = { state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center)) },
                onEndAlignClick = { state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.End)) },
                onExportClick = {
                    saveChanges(state.toHtml())
                },
                onLinkClick = { linkUrl ->
                    if (linkUrl.isNotEmpty()) {
                        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    }
                }
            )
        }

        // Fullscreen button positioned above the editor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {

        }

        // Editor area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                RichTextEditor(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .focusRequester(focusRequester)
                        .focusable(),
                    state = state,
                    interactionSource = interactionData,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(
                    onClick = { showFullScreenEditor = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp), // Made it rounded
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp, // Added light shadow
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen",
                            modifier = Modifier
                                .size(20.dp)
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Auto-focus when entering editor
    LaunchedEffect(Unit) {
//        focusRequester.requestFocus()
    }
}


@Composable
private fun UpdateEditorState(
    state: RichTextState,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    onStateUpdate: (EditorState) -> Unit
) {
    val currentStyles = state.currentSpanStyle
    val currentParagraphStyle = state.currentParagraphStyle

    // Create a stable reference for the highlight color
    val highlightColor = remember { Color.Yellow.copy(alpha = 0.7f) }

    LaunchedEffect(state.selection, currentStyles, currentParagraphStyle, state.annotatedString) {
        val newState = EditorState(
            boldSelected = currentStyles?.fontWeight == FontWeight.Bold,
            italicSelected = currentStyles?.fontStyle == FontStyle.Italic,
            underlineSelected = currentStyles?.textDecoration?.contains(TextDecoration.Underline) == true,
            titleSelected = currentStyles?.fontSize == titleSize,
            subtitleSelected = currentStyles?.fontSize == subtitleSize,
            textColorSelected = currentStyles?.color == Color.Red,
            highlightSelected = currentStyles?.background?.let { bg ->
                // More flexible color comparison
                bg.red == highlightColor.red &&
                        bg.green == highlightColor.green &&
                        bg.blue == highlightColor.blue &&
                        kotlin.math.abs(bg.alpha - highlightColor.alpha) < 0.1f
            } ?: false,
            alignmentSelected = when (currentParagraphStyle?.textAlign) {
                TextAlign.Center -> 1
                TextAlign.End -> 2
                else -> 0
            }
        )
        onStateUpdate(newState)
    }
}

@Composable
fun EditorControls(
    modifier: Modifier = Modifier,
    state: RichTextState,
    editorState: EditorState,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onTitleClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onStartAlignClick: () -> Unit,
    onEndAlignClick: () -> Unit,
    onCenterAlignClick: () -> Unit,
    onExportClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    onHighlightClick: () -> Unit
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }

    // Link Dialog
    if (showLinkDialog) {
        LinkDialog(
            linkUrl = linkUrl,
            onLinkChange = { linkUrl = it },
            onConfirm = {
                onLinkClick(linkUrl)
                showLinkDialog = false
                linkUrl = ""
            },
            onDismiss = {
                showLinkDialog = false
                linkUrl = ""
            }
        )
    }
    val listState = rememberLazyListState()

    Box {
        LazyRow(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // Text formatting controls
            item {
                ControlButton(
                    selected = editorState.boldSelected,
                    onClick = onBoldClick,
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold"
                )
            }

            item {
                ControlButton(
                    selected = editorState.italicSelected,
                    onClick = onItalicClick,
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic"
                )
            }

            item {
                ControlButton(
                    selected = editorState.underlineSelected,
                    onClick = onUnderlineClick,
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = "Underline"
                )
            }

            item { Divider() }

            // Size controls
            item {
                ControlButton(
                    selected = editorState.titleSelected,
                    onClick = onTitleClick,
                    icon = Icons.Default.Title,
                    contentDescription = "Title Size"
                )
            }

            item {
                ControlButton(
                    selected = editorState.subtitleSelected,
                    onClick = onSubtitleClick,
                    icon = Icons.Default.FormatSize,
                    contentDescription = "Subtitle Size"
                )
            }

            item { Divider() }

            // Color controls
            item {
                ControlButton(
                    selected = editorState.textColorSelected,
                    onClick = onTextColorClick,
                    icon = Icons.Default.FormatColorText,
                    contentDescription = "Text Color"
                )
            }

            item {
                ControlButton(
                    selected = editorState.highlightSelected,
                    onClick = onHighlightClick,
                    icon = Icons.Default.Highlight,
                    contentDescription = "Highlight",
                    selectedColor = Color.Yellow.copy(alpha = 0.7f)
                )
            }

            item { Divider() }

            // Alignment controls
            item {
                ControlButton(
                    selected = editorState.alignmentSelected == 0,
                    onClick = onStartAlignClick,
                    icon = Icons.Default.FormatAlignLeft,
                    contentDescription = "Align Left"
                )
            }

            item {
                ControlButton(
                    selected = editorState.alignmentSelected == 1,
                    onClick = onCenterAlignClick,
                    icon = Icons.Default.FormatAlignCenter,
                    contentDescription = "Align Center"
                )
            }

            item {
                ControlButton(
                    selected = editorState.alignmentSelected == 2,
                    onClick = onEndAlignClick,
                    icon = Icons.Default.FormatAlignRight,
                    contentDescription = "Align Right"
                )
            }

            item { Divider() }

            // Additional controls
//        item {
//            ControlButton(
//                selected = false,
//                onClick = { showLinkDialog = true },
//                icon = Icons.Default.AddLink,
//                contentDescription = "Add Link"
//            )
//        }

            item {
                ControlButton(
                    selected = false,
                    onClick = onExportClick,
                    icon = Icons.Default.Save,
                    contentDescription = "Save",
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val totalItems = layoutInfo.totalItemsCount

        if (visibleItems.isNotEmpty() && totalItems > 0) {
            val firstVisibleIndex = visibleItems.first().index
            val indicatorWidth = 200.dp
            val trackColor = Color.LightGray.copy(alpha = 0.4f)
            val thumbColor = MaterialTheme.colorScheme.primary

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(trackColor)
            )

            Box(
                Modifier
                    .width(indicatorWidth)
                    .height(4.dp)
                    .align(Alignment.BottomStart)
                    .offset {
                        val trackWidthPx =
                            layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val scrollFraction = firstVisibleIndex / (totalItems.toFloat())
                        val xOffset = trackWidthPx * scrollFraction
                        IntOffset(xOffset.toInt(), 0)
                    }
                    .background(thumbColor)
            )
        }
    }
}

@Composable
private fun ControlButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) {
                if (selectedColor == Color.Yellow.copy(alpha = 0.7f)) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(0.5.dp)
            )
    )
}

@Composable
fun FullScreenEditorDialog(
    state: RichTextState,
    editorState: EditorState,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    onStateUpdate: (EditorState) -> Unit,
    saveChanges: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val interactionData = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    var currentEditorState by remember { mutableStateOf(editorState) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val minHeight = screenHeight * 0.8f

    UpdateEditorState(
        state = state,
        titleSize = titleSize,
        subtitleSize = subtitleSize,
        onStateUpdate = {
            currentEditorState = it
            onStateUpdate(it)
        }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            // This is the key addition - makes dialog decorFitsSystemWindows
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize().heightIn(300.dp)
                .padding(0.dp, 8.dp)
                .statusBarsPadding()
                .imePadding(), // This handles keyboard padding
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar with title and close button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Full Screen Editor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    saveChanges(state.toHtml())
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save"
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onDismiss
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    }
                }

                // Fixed controls at top
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 8.dp),
                    tonalElevation = 2.dp
                ) {
                    FullScreenEditorControls(
                        modifier = Modifier.padding(16.dp),
                        state = state,
                        editorState = currentEditorState,
                        titleSize = titleSize,
                        subtitleSize = subtitleSize,
                        onBoldClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                        onItalicClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                        onUnderlineClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                        onTitleClick = { state.toggleSpanStyle(SpanStyle(fontSize = titleSize)) },
                        onSubtitleClick = { state.toggleSpanStyle(SpanStyle(fontSize = subtitleSize)) },
                        onTextColorClick = { state.toggleSpanStyle(SpanStyle(color = Color.Red)) },
                        onHighlightClick = {
                            state.toggleSpanStyle(
                                SpanStyle(
                                    background = Color.Yellow.copy(
                                        alpha = 0.7f
                                    )
                                )
                            )
                        },
                        onStartAlignClick = { state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Start)) },
                        onCenterAlignClick = { state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center)) },
                        onEndAlignClick = { state.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.End)) },
                        onLinkClick = { linkUrl ->
                            if (linkUrl.isNotEmpty()) {
                                state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                            }
                        }
                    )
                }

                // Scrollable editor area - removed imePadding from here since it's on the parent
                Box(modifier = Modifier
                    .padding(0.dp, 8.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.89F)) {
                    RichTextEditor(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart).heightIn(minHeight)
//                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .focusRequester(focusRequester)
                            .focusable(),
                        state = state,
                        interactionSource = interactionData,
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }

    // Auto-focus when entering full screen editor
    LaunchedEffect(Unit) {
        // Uncomment if you want auto-focus
        // focusRequester.requestFocus()
    }
}

// Alternative approach using Activity composition if the above doesn't work completely


// Alternative: Use a custom composable that handles keyboard manually

@Composable
fun FullScreenEditorControls(
    modifier: Modifier = Modifier,
    state: RichTextState,
    editorState: EditorState,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onTitleClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onTextColorClick: () -> Unit,
    onStartAlignClick: () -> Unit,
    onEndAlignClick: () -> Unit,
    onCenterAlignClick: () -> Unit,
    onLinkClick: (String) -> Unit,
    onHighlightClick: () -> Unit
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }

    // Link Dialog
    if (showLinkDialog) {
        LinkDialog(
            linkUrl = linkUrl,
            onLinkChange = { linkUrl = it },
            onConfirm = {
                onLinkClick(linkUrl)
                showLinkDialog = false
                linkUrl = ""
            },
            onDismiss = {
                showLinkDialog = false
                linkUrl = ""
            }
        )
    }

    val listState = rememberLazyListState()

    Box {
        LazyRow(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Text formatting section
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.boldSelected,
                        onClick = onBoldClick,
                        icon = Icons.Default.FormatBold,
                        contentDescription = "Bold",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Bold",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.italicSelected,
                        onClick = onItalicClick,
                        icon = Icons.Default.FormatItalic,
                        contentDescription = "Italic",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Italic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.underlineSelected,
                        onClick = onUnderlineClick,
                        icon = Icons.Default.FormatUnderlined,
                        contentDescription = "Underline",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Underline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.titleSelected,
                        onClick = onTitleClick,
                        icon = Icons.Default.Title,
                        contentDescription = "Title",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.subtitleSelected,
                        onClick = onSubtitleClick,
                        icon = Icons.Default.FormatSize,
                        contentDescription = "Subtitle",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Subtitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.textColorSelected,
                        onClick = onTextColorClick,
                        icon = Icons.Default.FormatColorText,
                        contentDescription = "Text Color",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.highlightSelected,
                        onClick = onHighlightClick,
                        icon = Icons.Default.Highlight,
                        contentDescription = "Highlight",
                        selectedColor = Color.Yellow.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Highlight",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.alignmentSelected == 0,
                        onClick = onStartAlignClick,
                        icon = Icons.Default.FormatAlignLeft,
                        contentDescription = "Align Left",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.alignmentSelected == 1,
                        onClick = onCenterAlignClick,
                        icon = Icons.Default.FormatAlignCenter,
                        contentDescription = "Center",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Center",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ControlButton(
                        selected = editorState.alignmentSelected == 2,
                        onClick = onEndAlignClick,
                        icon = Icons.Default.FormatAlignRight,
                        contentDescription = "Right",
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Right",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

//        item {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.spacedBy(2.dp)
//            ) {
//                ControlButton(
//                    selected = false,
//                    onClick = { showLinkDialog = true },
//                    icon = Icons.Default.AddLink,
//                    contentDescription = "Add Link",
//                    modifier = Modifier.size(40.dp)
//                )
//                Text(
//                    text = "Link",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
        }

        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val totalItems = layoutInfo.totalItemsCount

        if (visibleItems.isNotEmpty() && totalItems > 0) {
            val firstVisibleIndex = visibleItems.first().index
            val indicatorWidth = 320.dp
            val trackColor = Color.LightGray.copy(alpha = 0.4f)
            val thumbColor = MaterialTheme.colorScheme.primary

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(trackColor)
            )

            Box(
                Modifier
                    .width(indicatorWidth)
                    .height(4.dp)
                    .align(Alignment.BottomStart)
                    .offset {
                        val trackWidthPx =
                            layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val scrollFraction = firstVisibleIndex / (totalItems.toFloat())
                        val xOffset = trackWidthPx * scrollFraction
                        IntOffset(xOffset.toInt(), 0)
                    }
                    .background(thumbColor)
            )
        }
    }


}

// Keyboard height detector
@Composable
fun keyboardAsState(): State<Dp> {
    val keyboardHeight = remember { mutableStateOf(0.dp) }
    val view = LocalView.current
    val density = LocalDensity.current

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            val imeHeight = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            keyboardHeight.value = with(density) { imeHeight.toDp() }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return keyboardHeight
}


@Composable
private fun LinkDialog(
    linkUrl: String,
    onLinkChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Link",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            OutlinedTextField(
                value = linkUrl,
                onValueChange = onLinkChange,
                label = { Text("Enter URL") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (linkUrl.isNotBlank()) onConfirm() }
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = linkUrl.isNotBlank()
            ) {
                Text("Add Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ControlButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) {
                // Fixed color contrast issue
                if (selectedColor == Color.Yellow.copy(alpha = 0.7f)) {
                    Color.Black // Use black text on yellow background
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

// Legacy ControlWrapper for backward compatibility

@Composable
fun ShowEditedScreen(text: String) {
    val state = rememberRichTextState()
    LaunchedEffect(Unit) {
        state.setHtml(text)

    }
    val titleSize = MaterialTheme.typography.displaySmall.fontSize
    val subtitleSize = MaterialTheme.typography.titleLarge.fontSize

    // Track selection changes to update controls
    val selectionTracker = state.selection

    RichText(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent),
        state = state,
    )
}