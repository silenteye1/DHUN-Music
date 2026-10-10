package com.maxrave.simpmusic.ui.component

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.repository.LyricsRomanizerRepository
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.ui.component.lyrics.ShareLyricsSheet
import com.maxrave.simpmusic.ui.component.lyrics.toShareLyricsLines
import com.maxrave.simpmusic.ui.icon.Share
import com.maxrave.simpmusic.ui.screen.player.content.stripRichSyncTimestamps
import org.koin.compose.koinInject
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.model.streams.TimeLine
import com.maxrave.simpmusic.extension.KeepScreenOn
import com.maxrave.simpmusic.extension.ParsedRichSyncLine
import com.maxrave.simpmusic.extension.animateScrollAndAnchorItemTop
import com.maxrave.simpmusic.extension.animateScrollAndCentralizeItem
import com.maxrave.simpmusic.extension.formatDuration
import com.maxrave.simpmusic.extension.hsvToColor
import com.maxrave.simpmusic.extension.parseRichSyncWords
import com.maxrave.simpmusic.extension.parseTimestampToMilliseconds
import com.maxrave.simpmusic.ui.icon.Info
import com.maxrave.simpmusic.ui.icon.MoreVert
import com.maxrave.simpmusic.ui.icon.QueueMusic
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.navigation.destination.list.ArtistDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.NowPlayingScreenData
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.crossfading
import simpmusic.composeapp.generated.resources.share_lyrics
import simpmusic.composeapp.generated.resources.unavailable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max

private const val TAG = "LyricsView"

private const val MIN_WIPE_MS = 150
private const val EMP_AMOUNT_REF_MS = 2000f
private const val EMP_BLUR_REF_MS = 3000f
private const val EMP_MIN_DURATION_MS = 1000f
private const val EMP_AMOUNT_GAIN = 0.6f
private const val EMP_BLUR_GAIN = 0.5f
private const val EMP_AMOUNT_CAP = 1.2f
private const val EMP_BLUR_CAP = 0.8f
private const val EMP_LAST_WORD_AMOUNT = 1.6f
private const val EMP_LAST_WORD_BLUR = 1.5f
private const val EMP_SCALE_EM = 0.1f
private const val EMP_RISE_EM = 0.025f
private const val EMP_GLOW_RADIUS_EM = 0.3f
private const val FLARE_REACH_CHARS = 1.5f
private val FULLSCREEN_LYRICS_GUTTER = 50.dp
private const val FOOTER_ALPHA = 0.45f
private const val SUNG_BASE_GLOW_ALPHA = 0.75f
private const val SUNG_BASE_GLOW_EM = 0.26f

private val EmpBezIn = CubicBezierEasing(0.2f, 0.4f, 0.58f, 1f)
private val EmpBezOut = CubicBezierEasing(0.3f, 0f, 0.58f, 1f)

private fun empEasing(x: Float): Float =
    if (x < 0.5f) {
        EmpBezIn.transform((x / 0.5f).coerceIn(0f, 1f))
    } else {
        1f - EmpBezOut.transform(((x - 0.5f) / 0.5f).coerceIn(0f, 1f))
    }

private val DimOriginalColor = Color.LightGray.copy(alpha = 0.35f)
private val DimTranslatedColor = Color(0xFF97971A).copy(alpha = 0.3f)
private val DimRomanizedCurrentColor = Color.White.copy(alpha = 0.7f)
private val DimRomanizedColor = Color.LightGray.copy(alpha = 0.3f)
private val DimRichPendingColor = Color.LightGray.copy(alpha = 0.6f)

private data class TimedLineIndex(
    val index: Int,
    val startTimeMs: Long,
)

private fun List<TimedLineIndex>.activeIndexAt(nowMs: Long): Int {
    if (isEmpty()) return -1
    if (nowMs < first().startTimeMs) return -1
    var lo = 0
    var hi = size - 1
    var ans = -1
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        if (this[mid].startTimeMs <= nowMs) {
            ans = mid
            lo = mid + 1
        } else {
            hi = mid - 1
        }
    }
    return if (ans >= 0) this[ans].index else -1
}

private fun buildSyncedTranslatedWordsByLineIndex(
    originalLines: List<com.maxrave.domain.data.model.metadata.Line>,
    translatedLines: List<com.maxrave.domain.data.model.metadata.Line>,
    thresholdMs: Long = 1000L,
): Map<Int, String> {
    if (originalLines.isEmpty() || translatedLines.isEmpty()) return emptyMap()

    val sortedTranslated =
        translatedLines
            .mapNotNull { line ->
                val ts = line.startTimeMs.toLongOrNull()
                    ?: parseTimestampToMilliseconds(line.startTimeMs).toLong().takeIf { it > 0 }
                    ?: return@mapNotNull null
                ts to line.words
            }.sortedBy { it.first }

    if (sortedTranslated.isEmpty()) return emptyMap()

    data class OriginalEntry(val index: Int, val ts: Long)

    val sortedOriginal =
        originalLines
            .mapIndexedNotNull { index, line ->
                val ts = line.startTimeMs.toLongOrNull()
                    ?: parseTimestampToMilliseconds(line.startTimeMs).toLong().takeIf { it > 0 }
                    ?: return@mapIndexedNotNull null
                OriginalEntry(index, ts)
            }.sortedBy { it.ts }

    if (sortedOriginal.isEmpty()) return emptyMap()

    val result = HashMap<Int, String>(sortedOriginal.size)
    var j = 0
    for (orig in sortedOriginal) {
        while (j + 1 < sortedTranslated.size && sortedTranslated[j + 1].first <= orig.ts) {
            j++
        }
        val candA = sortedTranslated[j]
        val diffA = abs(candA.first - orig.ts)
        var bestTs = candA.first
        var bestWords = candA.second
        var bestDiff = diffA
        if (j + 1 < sortedTranslated.size) {
            val candB = sortedTranslated[j + 1]
            val diffB = abs(candB.first - orig.ts)
            if (diffB < bestDiff) {
                bestTs = candB.first
                bestWords = candB.second
                bestDiff = diffB
            }
        }
        if (bestDiff < thresholdMs) {
            result[orig.index] = bestWords
            @Suppress("UNUSED_VARIABLE")
            val _bt = bestTs
        }
    }
    return result
}

@Composable
fun LyricsView(
    lyricsData: NowPlayingScreenData.LyricsData,
    timeLine: StateFlow<TimeLine>,
    onLineClick: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showScrollShadows: Boolean = false,
    backgroundColor: Color = Color(0xFF242424),
    footerContent: (@Composable () -> Unit)? = null,
    dataStoreManager: DataStoreManager = koinInject(),
    romanizer: LyricsRomanizerRepository = koinInject(),
) {
    val listState = rememberLazyListState()
    val isDragging by listState.interactionSource.collectIsDraggedAsState()
    val current by timeLine.collectAsStateWithLifecycle()

    val lyricsStyle by dataStoreManager.lyricsStyle.collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val appleStyle = lyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()

    val romanizationStored by dataStoreManager.romanizationLanguages.collectAsStateWithLifecycle("")
    val romanizationLanguages =
        remember(romanizationStored) { RomanizationLanguage.parse(romanizationStored) }

    val exposedRowPx =
        with(LocalDensity.current) {
            AppleMusicLyricLineHeight.toPx() + AppleMusicLyricGap.toPx()
        }

    val timedLineIndexes =
        remember(lyricsData.lyrics.lines) {
            val timed =
                lyricsData.lyrics.lines
                    .orEmpty()
                    .mapIndexedNotNull { index, line ->
                        val parsed = line.startTimeMs.toLongOrNull()
                            ?: parseTimestampToMilliseconds(line.startTimeMs).toLong().takeIf { it > 0 }
                        parsed?.let { TimedLineIndex(index, it) }
                    }
            if (timed.distinctBy { it.startTimeMs }.size <= 1) {
                emptyList()
            } else {
                timed.sortedBy { it.startTimeMs }
            }
        }

    val currentLineIndex by remember(timedLineIndexes) {
        derivedStateOf {
            val now = current.current
            if (now <= 0L) -1 else timedLineIndexes.activeIndexAt(now)
        }
    }

    val allLinesCurrent = timedLineIndexes.isEmpty()

    val syncedTranslatedWordsByLineIndex =
        remember(
            lyricsData.lyrics.lines,
            lyricsData.translatedLyrics?.first?.lines,
        ) {
            buildSyncedTranslatedWordsByLineIndex(
                originalLines = lyricsData.lyrics.lines.orEmpty(),
                translatedLines = lyricsData.translatedLyrics?.first?.lines.orEmpty(),
                thresholdMs = 1000L,
            )
        }
    LaunchedEffect(currentLineIndex, lyricsData.lyrics.syncType, appleStyle) {
        if (currentLineIndex > -1 &&
            (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED")
        ) {
            if (appleStyle) {
                listState.animateScrollAndAnchorItemTop(currentLineIndex, -exposedRowPx)
            } else {
                listState.animateScrollAndCentralizeItem(currentLineIndex)
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val tailPadding = if (appleStyle) maxHeight * 0.72f else 0.dp
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = tailPadding),
        ) {
            items(lyricsData.lyrics.lines?.size ?: 0) { index ->
                val line = lyricsData.lyrics.lines?.getOrNull(index)
                val translatedWords =
                    if (lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED") {
                        syncedTranslatedWordsByLineIndex[index]
                    } else {
                        lyricsData.translatedLyrics
                            ?.first
                            ?.lines
                            ?.getOrNull(index)
                            ?.words
                    }

                line?.words?.let { words ->
                    val distanceFromCurrent = if (currentLineIndex < 0) 0 else index - currentLineIndex

                    val romanizedWords =
                        if (romanizationLanguages.isEmpty()) {
                            null
                        } else {
                            remember(words, romanizationLanguages) {
                                val source =
                                    if (lyricsData.lyrics.syncType == "RICH_SYNCED") words.stripRichSyncTimestamps() else words
                                romanizer.romanize(source, romanizationLanguages)
                            }
                        }

                    val triggerSeek: () -> Unit = {
                        val parsedMs = line.startTimeMs.toLongOrNull()
                            ?: parseTimestampToMilliseconds(line.startTimeMs).toLong()
                        if (timeLine.value.total > 0L && parsedMs >= 0L) {
                            val progress = (parsedMs.toFloat() * 100f / timeLine.value.total.toFloat()).coerceIn(0f, 100f)
                            onLineClick(progress)
                        }
                    }

                    val isSeekable = lyricsData.lyrics.syncType == "LINE_SYNCED" || lyricsData.lyrics.syncType == "RICH_SYNCED"

                    val renderLine: @Composable () -> Unit = {
                        when {
                            lyricsData.lyrics.syncType == "RICH_SYNCED" -> {
                                val parsedLine =
                                    remember(words, line.startTimeMs, line.endTimeMs) {
                                        parseRichSyncWords(words, line.startTimeMs, line.endTimeMs)
                                    }

                                if (parsedLine != null) {
                                    RichSyncLyricsLineItem(
                                        parsedLine = parsedLine,
                                        translatedWords = translatedWords,
                                        romanizedWords = romanizedWords,
                                        currentTimeMs = current.current,
                                        isCurrent = index == currentLineIndex,
                                        customFontSize = if (appleStyle) AppleMusicLyricFontSize else null,
                                        glow = if (appleStyle && index == currentLineIndex) AppleMusicActiveLineGlow else null,
                                        pendingColorOverride = if (appleStyle) AppleMusicPendingWordColor else null,
                                        translatedColorOverride = if (appleStyle) AppleMusicTranslatedColor else null,
                                        translatedStyleOverride =
                                            if (appleStyle) {
                                                typo().bodyMedium.copy(
                                                    fontSize = AppleMusicSubLineFontSize,
                                                    lineHeight = AppleMusicSubLineHeight,
                                                )
                                            } else {
                                                null
                                            },
                                        customPadding = if (appleStyle) AppleMusicLyricGap else 12.dp,
                                        wrappedLineSpacing = if (appleStyle) AppleMusicWrappedLineSpacing else 0.dp,
                                        modifier =
                                            if (appleStyle) {
                                                Modifier
                                            } else {
                                                Modifier.clickable(enabled = isSeekable) {
                                                    triggerSeek()
                                                }
                                            },
                                    )
                                } else if (appleStyle) {
                                    AppleMusicLyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isCurrent = index == currentLineIndex || allLinesCurrent,
                                        romanizedWords = romanizedWords,
                                    )
                                } else {
                                    LyricsLineItem(
                                        originalWords = words,
                                        translatedWords = translatedWords,
                                        isBold = index <= currentLineIndex,
                                        isCurrent = index == currentLineIndex,
                                        romanizedWords = romanizedWords,
                                        modifier =
                                            Modifier.clickable(enabled = isSeekable) {
                                                triggerSeek()
                                            },
                                    )
                                }
                            }

                            appleStyle -> {
                                AppleMusicLyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    romanizedWords = romanizedWords,
                                    isCurrent = index == currentLineIndex || allLinesCurrent,
                                )
                            }

                            else -> {
                                LyricsLineItem(
                                    originalWords = words,
                                    translatedWords = translatedWords,
                                    romanizedWords = romanizedWords,
                                    isBold = index <= currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    isCurrent = index == currentLineIndex || lyricsData.lyrics.syncType != "LINE_SYNCED",
                                    modifier =
                                        Modifier.clickable(enabled = isSeekable) {
                                            triggerSeek()
                                        },
                                )
                            }
                        }
                    }

                    if (appleStyle) {
                        val lineInteraction = remember { MutableInteractionSource() }
                        val linePressed by lineInteraction.collectIsPressedAsState()
                        Column(
                            modifier =
                                Modifier
                                    .background(
                                        color = if (linePressed) AppleMusicLyricPressedBackground else Color.Transparent,
                                        shape = RoundedCornerShape(AppleMusicLyricCornerRadius),
                                    ).clickable(
                                        interactionSource = lineInteraction,
                                        indication = null,
                                        enabled = isSeekable,
                                    ) {
                                        triggerSeek()
                                    },
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .appleMusicLyricFocus(
                                            distanceFromCurrent,
                                            blurEnabled = !isDragging,
                                            hasActiveLine = currentLineIndex >= 0,
                                            allLinesCurrent = allLinesCurrent,
                                        ),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = AppleMusicLyricPaddingX)) {
                                    renderLine()
                                }
                            }
                        }
                    } else {
                        renderLine()
                    }
                }
            }
            footerContent?.let { footer ->
                item {
                    if (appleStyle) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(horizontal = AppleMusicLyricPaddingX)
                                    .alpha(FOOTER_ALPHA),
                        ) {
                            footer()
                        }
                    } else {
                        footer()
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsLineItem(
    originalWords: String,
    translatedWords: String?,
    isBold: Boolean,
    isCurrent: Boolean = false,
    romanizedWords: String? = null,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = isBold) {
        if (it) {
            Column(
                modifier = modifier,
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = originalWords,
                    style = typo().headlineLarge,
                    color = if (isCurrent) Color.White else DimOriginalColor,
                )
                if (romanizedWords != null) {
                    Text(
                        text = romanizedWords,
                        style = typo().bodyMedium,
                        color = if (isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
                    )
                }
                if (translatedWords != null) {
                    Text(
                        text = translatedWords,
                        style = typo().bodyMedium,
                        color = if (isCurrent) Color.Yellow else DimTranslatedColor,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    if (!isBold) {
        Column(
            modifier = modifier,
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = originalWords,
                style = typo().headlineMedium,
                color = DimOriginalColor,
            )
            if (romanizedWords != null) {
                Text(
                    text = romanizedWords,
                    style = typo().bodyMedium,
                    color = DimRomanizedColor,
                )
            }
            if (translatedWords != null) {
                Text(
                    text = translatedWords,
                    style = typo().bodyMedium,
                    color = DimTranslatedColor,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichSyncLyricsLineItem(
    parsedLine: ParsedRichSyncLine,
    translatedWords: String?,
    romanizedWords: String? = null,
    currentTimeMs: Long,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    customPadding: Dp = 12.dp,
    glow: Shadow? = null,
    pendingColorOverride: Color? = null,
    translatedColorOverride: Color? = null,
    translatedStyleOverride: TextStyle? = null,
    wrappedLineSpacing: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val currentWordIndex by remember(currentTimeMs, parsedLine.words) {
        derivedStateOf {
            if (!isCurrent) return@derivedStateOf -1
            parsedLine.words.indexOfLast { it.startTimeMs <= currentTimeMs }
        }
    }

    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(customPadding))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement =
                if (wrappedLineSpacing > 0.dp) Arrangement.spacedBy(wrappedLineSpacing) else Arrangement.Center,
        ) {
            parsedLine.words.forEachIndexed { index, wordTiming ->
                val wordEndTimeMs =
                    if (index < parsedLine.words.size - 1) {
                        parsedLine.words[index + 1].startTimeMs
                    } else if (parsedLine.lineEndTimeMs == Long.MAX_VALUE || parsedLine.lineEndTimeMs <= wordTiming.startTimeMs) {
                        if (index > 0 && parsedLine.words[index - 1].startTimeMs < wordTiming.startTimeMs) {
                            val prevWordDuration = wordTiming.startTimeMs - parsedLine.words[index - 1].startTimeMs
                            wordTiming.startTimeMs + prevWordDuration
                        } else {
                            wordTiming.startTimeMs + 500L
                        }
                    } else {
                        parsedLine.lineEndTimeMs
                    }
                AnimatedWord(
                    word = wordTiming.text,
                    wordIndex = index,
                    wordStartTimeMs = wordTiming.startTimeMs,
                    wordEndTimeMs = wordEndTimeMs,
                    currentTimeMs = currentTimeMs,
                    isActive = isCurrent && index == currentWordIndex,
                    isPast = isCurrent && index < currentWordIndex,
                    isCurrent = isCurrent,
                    customFontSize = customFontSize,
                    glow = glow,
                    isLastWord = index == parsedLine.words.lastIndex,
                    pendingColorOverride = pendingColorOverride,
                )
            }
        }

        if (romanizedWords != null) {
            Text(
                text = romanizedWords,
                style = translatedStyleOverride ?: typo().bodyMedium,
                color = if (isCurrent) DimRomanizedCurrentColor else DimRomanizedColor,
            )
        }

        if (translatedWords != null) {
            Text(
                text = translatedWords,
                style = translatedStyleOverride ?: typo().bodyMedium,
                color = translatedColorOverride ?: if (isCurrent) Color.Yellow else DimTranslatedColor,
            )
        }

        Spacer(modifier = Modifier.height(customPadding))
    }
}

@Composable
private fun AnimatedWord(
    word: String,
    wordIndex: Int,
    wordStartTimeMs: Long,
    wordEndTimeMs: Long,
    currentTimeMs: Long,
    isActive: Boolean,
    isPast: Boolean,
    isCurrent: Boolean,
    customFontSize: TextUnit? = null,
    glow: Shadow? = null,
    isLastWord: Boolean = false,
    pendingColorOverride: Color? = null,
) {
    val style =
        typo().headlineLarge.copy(
            fontSize = customFontSize ?: typo().headlineLarge.fontSize,
        )

    if (!isCurrent) {
        Text(text = word, style = style, color = pendingColorOverride ?: DimOriginalColor)
        return
    }

    val wordDurationMs = (wordEndTimeMs - wordStartTimeMs).coerceAtLeast(100L)
    val anim =
        remember(wordStartTimeMs, wordEndTimeMs) {
            val initial =
                ((currentTimeMs - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                    .coerceIn(0f, 1f)
            androidx.compose.animation.core.Animatable(initial)
        }

    LaunchedEffect(wordStartTimeMs, wordEndTimeMs, isActive, isPast) {
        when {
            isPast -> anim.snapTo(1f)
            isActive -> {
                val now = currentTimeMs
                val current =
                    ((now - wordStartTimeMs).toFloat() / wordDurationMs.toFloat())
                        .coerceIn(0f, 1f)
                anim.snapTo(current)
                val remainingMs =
                    (wordEndTimeMs - now).coerceAtLeast(0L).toInt().coerceAtLeast(MIN_WIPE_MS)
                anim.animateTo(1f, tween(remainingMs, easing = LinearEasing))
            }
        }
    }

    val progress = anim.value

    val wordProgress =
        when {
            isPast -> 1f
            isActive -> progress
            else -> 0f
        }

    val emphasisDurationMs = max(EMP_MIN_DURATION_MS, wordDurationMs.toFloat())
    val amount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_AMOUNT_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_AMOUNT_CAP, shaped * EMP_AMOUNT_GAIN * (if (isLastWord) EMP_LAST_WORD_AMOUNT else 1f))
        }
    val blurAmount =
        if (glow == null) {
            0f
        } else {
            val raw = emphasisDurationMs / EMP_BLUR_REF_MS
            val shaped = if (raw > 1f) sqrt(raw) else raw * raw * raw
            min(EMP_BLUR_CAP, shaped * EMP_BLUR_GAIN * (if (isLastWord) EMP_LAST_WORD_BLUR else 1f))
        }
    val eased = if (amount <= 0f && blurAmount <= 0f) 0f else empEasing(wordProgress)
    val fontPx = with(LocalDensity.current) { style.fontSize.toPx() }
    val heldGlow =
        if (eased * blurAmount <= 0.01f) {
            null
        } else {
            glow?.copy(
                color = glow.color.copy(alpha = (eased * blurAmount).coerceIn(0f, 1f)),
                blurRadius = min(EMP_GLOW_RADIUS_EM, blurAmount * EMP_GLOW_RADIUS_EM) * fontPx,
            )
        }

    Box(
        modifier =
            Modifier.graphicsLayer {
                val scale = 1f + eased * EMP_SCALE_EM * amount
                scaleX = scale
                scaleY = scale
                translationY = -eased * EMP_RISE_EM * amount * fontPx
            },
    ) {
        val chars = word.toCharArray()
        val charCount = chars.size.coerceAtLeast(1)
        Row {
            chars.forEachIndexed { charIndex, ch ->
                val charFrom = charIndex.toFloat() / charCount
                val charTo = (charIndex + 1).toFloat() / charCount
                val charProgress = ((wordProgress - charFrom) / (charTo - charFrom)).coerceIn(0f, 1f)
                val charPast = wordProgress >= charTo
                val charActive = isActive && wordProgress >= charFrom && wordProgress < charTo
                val charCenter = (charFrom + charTo) / 2f
                val reach = (FLARE_REACH_CHARS / charCount).coerceAtLeast(0.0001f)
                val charFlare =
                    if (!isActive || glow == null) {
                        0f
                    } else {
                        (1f - abs(wordProgress - charCenter) / reach).coerceIn(0f, 1f)
                    }
                val restingColor = pendingColorOverride ?: DimRichPendingColor
                Box {
                    val glowShadow = heldGlow ?: glow?.copy(blurRadius = SUNG_BASE_GLOW_EM * fontPx)
                    if (glowShadow != null) {
                        Text(
                            text = ch.toString(),
                            style =
                                style.copy(
                                    shadow =
                                        glowShadow.copy(
                                            color = glowShadow.color.copy(alpha = SUNG_BASE_GLOW_ALPHA * charFlare),
                                        ),
                                ),
                            color = Color.Transparent,
                        )
                    }
                    Text(
                        text = ch.toString(),
                        style = style,
                        color =
                            when {
                                charPast -> Color.White
                                charActive -> lerp(restingColor, Color.White, charProgress)
                                else -> restingColor
                            },
                    )
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun FullscreenLyricsSheet(
    sharedViewModel: SharedViewModel,
    navController: NavController,
    color: Color = Color(0xFF242424),
    onDismiss: () -> Unit,
) {
    val fullscreenLyricsStyle by sharedViewModel
        .getLyricsStyle()
        .collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val fullscreenAppleLyrics =
        fullscreenLyricsStyle == DataStoreManager.LYRICS_STYLE_APPLE_MUSIC && isLyricsBlurSupported()
    val screenDataState by sharedViewModel.nowPlayingScreenData.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    val windowInsets = WindowInsets.systemBars

    var sliderValue by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    var showControlButtons by rememberSaveable {
        mutableStateOf(true)
    }

    var showNowPlayingSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val startColor = remember { Animatable(color) }
    val midColor1 = remember { Animatable(color.copy(alpha = 0.95f)) }
    val midColor2 = remember { Animatable(color.copy(alpha = 0.85f)) }
    val endColor = remember { Animatable(Color.Black) }

    val gradientTransition = rememberInfiniteTransition(label = "lyricsGradient")
    val animatedAngle by gradientTransition.animateFloat(
        initialValue = -45f,
        targetValue = 45f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "lyricsGradientAngle",
    )
    val animatedOffsetX by gradientTransition.animateFloat(
        initialValue = -1500f,
        targetValue = 1500f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "lyricsGradientOffsetX",
    )
    val animatedOffsetY by gradientTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "lyricsGradientOffsetY",
    )
    val gradientAngle = animatedAngle
    val gradientOffsetX = animatedOffsetX
    val gradientOffsetY = animatedOffsetY

    LaunchedEffect(color) {
        launch {
            startColor.animateTo(
                targetValue = color,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
        launch {
            midColor1.animateTo(
                targetValue = color.copy(alpha = 0.95f),
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
        launch {
            midColor2.animateTo(
                targetValue = color.copy(alpha = 0.85f),
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
        launch {
            endColor.animateTo(
                targetValue = Color.Black,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(key1 = showControlButtons) {
        if (showControlButtons) {
            delay(4000)
            showControlButtons = false
        }
    }

    LaunchedEffect(key1 = timelineState) {
        sliderValue =
            if (timelineState.total > 0L) {
                timelineState.current.toFloat() * 100 / timelineState.total.toFloat()
            } else {
                0f
            }
    }

    if (screenDataState.lyricsData != null) {
        KeepScreenOn()
    }

    var showQueueBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showInfoBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    var showShareLyricsSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val shareTimedLineIndexes =
        remember(screenDataState.lyricsData?.lyrics?.lines) {
            screenDataState.lyricsData
                ?.lyrics
                ?.lines
                .orEmpty()
                .mapIndexedNotNull { index, line ->
                    val parsed = line.startTimeMs.toLongOrNull()
                        ?: parseTimestampToMilliseconds(line.startTimeMs).toLong().takeIf { it > 0 }
                    parsed?.let { TimedLineIndex(index, it) }
                }.sortedBy { it.startTimeMs }
        }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        containerColor = Color.Black,
        contentColor = Color.Transparent,
        dragHandle = {},
        scrimColor = Color.Black.copy(alpha = .5f),
        sheetState = sheetState,
        modifier =
            Modifier
                .fillMaxHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    showControlButtons = true
                },
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RectangleShape,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "crossfadeRainbow")
        val rainbowHue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "rainbowHue",
        )
        val rainbowColor = hsvToColor(rainbowHue, 1f, 1f)
        val sliderTrackColor by animateColorAsState(
            targetValue = if (timelineState.isCrossfading) rainbowColor else Color.White,
            animationSpec = tween(300),
            label = "sliderCrossfadeColor",
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        startColor.value,
                                        midColor1.value,
                                        midColor2.value,
                                        endColor.value.copy(alpha = 0.9f),
                                        endColor.value,
                                    ),
                                start =
                                    Offset(
                                        x = gradientOffsetX + (cos(gradientAngle * PI.toFloat() / 180f) * 800f),
                                        y = gradientOffsetY + (sin(gradientAngle * PI.toFloat() / 180f) * 800f),
                                    ),
                                end =
                                    Offset(
                                        x = gradientOffsetX + 2500f + (cos((gradientAngle + 180f) * PI.toFloat() / 180f) * 800f),
                                        y = gradientOffsetY + 2500f + (sin((gradientAngle + 180f) * PI.toFloat() / 180f) * 800f),
                                    ),
                            ),
                        ),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            bottom =
                                with(localDensity) {
                                    windowInsets.getBottom(localDensity).toDp()
                                },
                            top =
                                with(localDensity) {
                                    windowInsets.getTop(localDensity).toDp()
                                },
                        ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 36.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(LocalPlatformContext.current)
                                .data(screenDataState.thumbnailURL)
                                .crossfade(300)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(screenDataState.thumbnailURL)
                                .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(8.dp)),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = screenDataState.nowPlayingTitle,
                            style = typo().labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            modifier =
                                Modifier
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        animationMode = MarqueeAnimationMode.Immediately,
                                    ).focusable(),
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.clickable {
                                    coroutineScope.launch {
                                        val song = sharedViewModel.nowPlayingState.value?.songEntity
                                        (
                                            song?.artistId?.firstOrNull()?.takeIf { it.isNotEmpty() }
                                                ?: screenDataState.songInfoData?.authorId
                                            )?.let { channelId ->
                                                sheetState.hide()
                                                onDismiss()
                                                navController.navigate(
                                                    ArtistDestination(
                                                        channelId = channelId,
                                                    ),
                                                )
                                            }
                                    }
                                },
                        ) {
                            if (screenDataState.isExplicit) {
                                ExplicitBadge(
                                    modifier =
                                        Modifier
                                            .size(16.dp)
                                            .padding(end = 4.dp),
                                )
                            }
                            Text(
                                text = screenDataState.artistName,
                                style = typo().bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier =
                                    Modifier
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            animationMode = MarqueeAnimationMode.Immediately,
                                        ).focusable(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    HeartCheckBox(
                        checked = controllerState.isLiked,
                        size = 28,
                    ) {
                        sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (screenDataState.lyricsData != null) {
                        IconButton(
                            onClick = { showShareLyricsSheet = true },
                        ) {
                            Icon(
                                imageVector = SimpIcons.Share,
                                contentDescription = stringResource(Res.string.share_lyrics),
                                tint = Color.White,
                            )
                        }
                    }

                    IconButton(
                        onClick = { showNowPlayingSheet = true },
                    ) {
                        Icon(
                            imageVector = SimpIcons.MoreVert,
                            contentDescription = "",
                            tint = Color.White,
                        )
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    if (fullscreenAppleLyrics) {
                                        FULLSCREEN_LYRICS_GUTTER - AppleMusicLyricPaddingX
                                    } else {
                                        FULLSCREEN_LYRICS_GUTTER
                                    },
                            ),
                ) {
                    Crossfade(
                        targetState = screenDataState.lyricsData != null,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (it) {
                            screenDataState.lyricsData?.let { lyrics ->
                                LyricsView(
                                    lyricsData = lyrics,
                                    timeLine = sharedViewModel.timeline,
                                    onLineClick = { f ->
                                        sharedViewModel.onUIEvent(UIEvent.UpdateProgress(f))
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    showScrollShadows = true,
                                    backgroundColor = startColor.value,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.unavailable),
                                    style = typo().bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Column {
                    Box(
                        Modifier
                            .padding(
                                top = 15.dp,
                            ).padding(horizontal = 40.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Crossfade(timelineState.loading) {
                                if (it) {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .padding(
                                                        horizontal = 3.dp,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                        )
                                    }
                                } else {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        LinearProgressIndicator(
                                            progress = { timelineState.bufferedPercent.toFloat() / 100 },
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .padding(
                                                        horizontal = 3.dp,
                                                    ).clip(
                                                        RoundedCornerShape(8.dp),
                                                    ),
                                            color = Color.Gray,
                                            trackColor = Color.DarkGray,
                                            strokeCap = StrokeCap.Round,
                                            drawStopIndicator = {},
                                        )
                                    }
                                }
                            }
                        }
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            Slider(
                                value = sliderValue / 100f,
                                onValueChange = {
                                    sharedViewModel.onUIEvent(
                                        UIEvent.UpdateProgress(it * 100f),
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 3.dp)
                                        .align(
                                            Alignment.TopCenter,
                                        ),
                                track = { sliderState ->
                                    SliderDefaults.Track(
                                        modifier =
                                            Modifier
                                                .height(5.dp),
                                        enabled = true,
                                        sliderState = sliderState,
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = sliderTrackColor,
                                                activeTrackColor = sliderTrackColor,
                                                inactiveTrackColor = Color.Transparent,
                                            ),
                                        thumbTrackGapSize = 0.dp,
                                        drawTick = { _, _ -> },
                                        drawStopIndicator = null,
                                    )
                                },
                                thumb = {
                                    SliderDefaults.Thumb(
                                        modifier =
                                            Modifier
                                                .height(18.dp)
                                                .width(8.dp)
                                                .padding(
                                                    vertical = 4.dp,
                                                ),
                                        thumbSize = DpSize(8.dp, 8.dp),
                                        interactionSource =
                                            remember {
                                                MutableInteractionSource()
                                            },
                                        colors =
                                            SliderDefaults.colors().copy(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color.White,
                                                inactiveTrackColor = Color.Transparent,
                                            ),
                                        enabled = true,
                                    )
                                },
                            )
                        }
                    }
                    LazyColumn {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp),
                            ) {
                                Text(
                                    text = formatDuration(timelineState.current),
                                    style = typo().bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Left,
                                )
                                AnimatedVisibility(
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                    visible = timelineState.isCrossfading,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.crossfading),
                                        style = typo().bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                Text(
                                    text = formatDuration(timelineState.total),
                                    style = typo().bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right,
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(5.dp),
                            )
                        }

                        item {
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter =
                                    expandVertically(
                                        tween(300),
                                    ),
                                exit =
                                    shrinkVertically(
                                        tween(300),
                                    ),
                            ) {
                                PlayerControlLayout(controllerState) {
                                    sharedViewModel.onUIEvent(it)
                                }
                            }
                            AnimatedVisibility(
                                visible = showControlButtons,
                                enter =
                                    expandVertically(
                                        tween(300),
                                    ),
                                exit =
                                    shrinkVertically(
                                        tween(300),
                                    ),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .height(32.dp)
                                            .fillMaxWidth()
                                            .padding(horizontal = 40.dp),
                                ) {
                                    IconButton(
                                        modifier =
                                            Modifier
                                                .size(24.dp)
                                                .aspectRatio(1f)
                                                .align(Alignment.CenterStart)
                                                .clip(
                                                    CircleShape,
                                                ),
                                        onClick = {
                                            showInfoBottomSheet = true
                                            showControlButtons = true
                                        },
                                    ) {
                                        Icon(imageVector = SimpIcons.Info, tint = Color.White, contentDescription = "")
                                    }
                                    Row(
                                        Modifier.align(Alignment.CenterEnd),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        IconButton(
                                            modifier =
                                                Modifier
                                                    .size(24.dp)
                                                    .aspectRatio(1f)
                                                    .clip(
                                                        CircleShape,
                                                    ),
                                            onClick = {
                                                showQueueBottomSheet = true
                                                showControlButtons = true
                                            },
                                        ) {
                                            Icon(
                                                imageVector = SimpIcons.QueueMusic,
                                                tint = Color.White,
                                                contentDescription = "",
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }

                if (!showControlButtons) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
    if (showQueueBottomSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueBottomSheet = false
            },
        )
    }
    if (showInfoBottomSheet) {
        InfoPlayerBottomSheet(
            onDismiss = {
                showInfoBottomSheet = false
            },
        )
    }
    if (showNowPlayingSheet) {
        NowPlayingBottomSheet(
            onDismiss = {
                showNowPlayingSheet = false
            },
            navController = navController,
            onNavigateToOtherScreen = {
                onDismiss()
            },
            song = null,
            setSleepTimerEnable = true,
            changeMainLyricsProviderEnable = true,
        )
    }

    screenDataState.lyricsData?.let { lyricsData ->
        if (showShareLyricsSheet) {
            ShareLyricsSheet(
                lines = lyricsData.toShareLyricsLines(),
                songTitle = screenDataState.nowPlayingTitle,
                artistName = screenDataState.artistName,
                artwork = screenDataState.bitmap,
                seedColor = color,
                initialLineIndex = shareTimedLineIndexes.activeIndexAt(timelineState.current),
                onDismiss = { showShareLyricsSheet = false },
            )
        }
    }
}