package com.pilotothegreat.deencompanion.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pilotothegreat.deencompanion.R
import com.pilotothegreat.deencompanion.services.SpeechState
import com.pilotothegreat.deencompanion.ui.navigation.Navigator
import com.pilotothegreat.deencompanion.ui.navigation.QuranReaderKey
import com.pilotothegreat.deencompanion.ui.theme.arabicFontFamily
import com.pilotothegreat.deencompanion.ui.theme.uthmaniFontFamily
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    paddingValues: PaddingValues
) {
    val viewModel: AssistantViewModel = koinViewModel()
    val navigator: Navigator = koinInject()
    val context = LocalContext.current
    
    val hazeState = rememberHazeState()
    val messages by viewModel.messages.collectAsState()
    val speechState by viewModel.speechState.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_assistant)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.purgeHistory() }) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Purge History", tint = colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surfaceContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .hazeSource(hazeState)
                .padding(innerPadding)
        ) {
            
            // Conversation History List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.settings),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = colorScheme.secondary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Ask me anything about the Quran and Hadith.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.secondary
                                )
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(message = msg, navigator = navigator)
                    }
                }
            }

            // Speech State Indicator / Feedback Banner
            AnimatedSpeechStateBanner(speechState)

            // Input Row
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (speechState is SpeechState.Recording) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        }
                    ) {
                        val icon = if (speechState is SpeechState.Recording) Icons.Default.MicOff else Icons.Default.Mic
                        val tint = if (speechState is SpeechState.Recording) colorScheme.error else colorScheme.primary
                        Icon(imageVector = icon, contentDescription = "Voice Input", tint = tint)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("Search Quran or Hadith...") },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.medium
                    )

                    IconButton(
                        onClick = {
                            if (inputText.trim().isNotEmpty()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.trim().isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: AssistantMessage,
    navigator: Navigator
) {
    val isArabic = isArabicText(message.text)
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh
    val textColor = if (message.isUser) colorScheme.onPrimaryContainer else colorScheme.onSurface
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    MaterialTheme.shapes.medium.copy(
                        bottomStart = if (message.isUser) CornerSize(16.dp) else CornerSize(0.dp),
                        bottomEnd = if (message.isUser) CornerSize(0.dp) else CornerSize(16.dp)
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (isArabic) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val annotatedString = parseVerseCitations(message.text, colorScheme.primary)
                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = uthmaniFontFamily,
                            lineHeight = 32.sp,
                            color = textColor,
                            localeList = androidx.compose.ui.text.intl.LocaleList(androidx.compose.ui.text.intl.Locale("ar"))
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "DEEP_LINK", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    val parts = annotation.item.split(":")
                                    val surah = parts.getOrNull(0)?.toIntOrNull()
                                    val verse = parts.getOrNull(1)?.toIntOrNull()
                                    if (surah != null && verse != null) {
                                        navigator.goTo(QuranReaderKey(surah, "Surah", scrollToVerse = verse))
                                    }
                                }
                        }
                    )
                }
            } else {
                val annotatedString = parseVerseCitations(message.text, colorScheme.primary)
                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color = textColor
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "DEEP_LINK", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val parts = annotation.item.split(":")
                                val surah = parts.getOrNull(0)?.toIntOrNull()
                                val verse = parts.getOrNull(1)?.toIntOrNull()
                                if (surah != null && verse != null) {
                                    navigator.goTo(QuranReaderKey(surah, "Surah", scrollToVerse = verse))
                                }
                            }
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedSpeechStateBanner(state: SpeechState) {
    if (state is SpeechState.Idle) return

    val text = when (state) {
        is SpeechState.Recording -> "Listening for speech (auto-stops on silence)..."
        is SpeechState.Processing -> "Processing speech..."
        is SpeechState.ConfidenceCheck -> "Checking confidence score: ${String.format(Locale.US, "%.0f%%", state.confidence * 100)}..."
        is SpeechState.PlayingTajweed -> "Playing Tajweed audio recitation..."
        is SpeechState.PlayingTTS -> "Playing translation TTS..."
        is SpeechState.Error -> "Error: ${state.errorMsg}"
        else -> ""
    }

    val bannerBg = if (state is SpeechState.Error) colorScheme.errorContainer else colorScheme.tertiaryContainer
    val bannerText = if (state is SpeechState.Error) colorScheme.onErrorContainer else colorScheme.onTertiaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerBg)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = bannerText,
            textAlign = TextAlign.Center
        )
    }
}

// Regex to match verse citation formats like: "2:255", "114:6", or Arabic digits "٢:٢٥٥"
private val citationRegex = Regex("(\\d+|[\\u0660-\\u0669]+)[:：](\\d+|[\\u0660-\\u0669]+)")

fun parseVerseCitations(text: String, linkColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var lastIndex = 0

    citationRegex.findAll(text).forEach { match ->
        // Add plain text preceding match
        if (match.range.first > lastIndex) {
            builder.append(text.substring(lastIndex, match.range.first))
        }

        val matchText = match.value
        val groups = match.groupValues
        val surahStr = groups.getOrNull(1) ?: ""
        val verseStr = groups.getOrNull(2) ?: ""

        val surahNum = parseArabicOrEnglishInt(surahStr)
        val verseNum = parseArabicOrEnglishInt(verseStr)

        val startIndex = builder.length
        builder.append(matchText)
        
        builder.addStyle(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold
            ),
            start = startIndex,
            end = builder.length
        )

        builder.addStringAnnotation(
            tag = "DEEP_LINK",
            annotation = "$surahNum:$verseNum",
            start = startIndex,
            end = builder.length
        )

        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        builder.append(text.substring(lastIndex))
    }

    return builder.toAnnotatedString()
}

private fun parseArabicOrEnglishInt(s: String): Int {
    val engDigits = s.map { c ->
        if (c in '٠'..'٩') {
            (c - '٠').toString()
        } else {
            c.toString()
        }
    }.joinToString("")
    return engDigits.toIntOrNull() ?: 1
}

private fun isArabicText(text: String): Boolean {
    // Detect if text contains Arabic block characters
    for (char in text) {
        if (char.code in 0x0600..0x06FF || char.code in 0x0750..0x077F) {
            return true
        }
    }
    return false
}
