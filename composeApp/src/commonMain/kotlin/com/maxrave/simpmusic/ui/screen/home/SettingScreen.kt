package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.toKmpUri
import com.maxrave.common.*
import com.maxrave.domain.data.model.lyrics.RomanizationDictionaryState
import com.maxrave.domain.data.model.lyrics.RomanizationLanguage
import com.maxrave.domain.extension.now
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.manager.DataStoreManager.Values.TRUE
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.domain.utils.LocalResource
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.fileSaverResult
import com.maxrave.simpmusic.expect.ui.isLyricsBlurSupported
import com.maxrave.simpmusic.expect.ui.isWallpaperDynamicColorSupported
import com.maxrave.simpmusic.extension.bytesToMB
import com.maxrave.simpmusic.extension.displayString
import com.maxrave.simpmusic.extension.isTwoLetterCode
import com.maxrave.simpmusic.extension.isValidProxyHost
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.component.*
import com.maxrave.simpmusic.ui.icon.*
import com.maxrave.simpmusic.ui.navigation.destination.login.DiscordLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LastfmLoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.LoginDestination
import com.maxrave.simpmusic.ui.navigation.destination.login.SpotifyLoginDestination
import com.maxrave.simpmusic.ui.theme.LocalIsDarkTheme
import com.maxrave.simpmusic.ui.theme.md_theme_dark_primary
import com.maxrave.simpmusic.ui.theme.parseThemeColorHex
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.ImportViewModel
import com.maxrave.simpmusic.viewModel.SettingAlertState
import com.maxrave.simpmusic.viewModel.SettingBasicAlertState
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.ChipColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.io.getPath
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoilApi::class,
    FormatStringsInDatetimeFormats::class,
    ExperimentalCalfApi::class,
    ExperimentalHazeMaterialsApi::class,
)
@Composable
fun SettingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinInject(),
) {
    val platformContext = LocalPlatformContext.current
    val calfContext = com.mohamedrejeb.calf.core.LocalPlatformContext.current
    val localDensity = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalIsDarkTheme.current
    val hazeState = rememberHazeState(blurEnabled = true)

    var width by rememberSaveable { mutableIntStateOf(0) }
    var topBarHeightPx by rememberSaveable { mutableIntStateOf(0) }

    val formatter = LocalDateTime.Format { byUnicodePattern("yyyyMMddHHmmss") }
    val appName = stringResource(Res.string.app_name)

    val backupLauncher = fileSaverResult(
        "${appName}_${now().format(formatter)}.backup",
        "application/octet-stream",
    ) { uri -> uri?.let { viewModel.backup(it.toKmpUri()) } }

    val restoreLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.All,
        selectionMode = FilePickerSelectionMode.Single,
    ) { file -> file.firstOrNull()?.getPath(calfContext)?.toKmpUri()?.let { viewModel.restore(it) } }

    val importViewModel: ImportViewModel = koinViewModel()
    val importState by importViewModel.importState.collectAsStateWithLifecycle()
    val importLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.All,
        selectionMode = FilePickerSelectionMode.Single,
    ) { file -> file.firstOrNull()?.let { importViewModel.import(it, calfContext) } }

    val enableTranslucentNavBar by remember { viewModel.translucentBottomBar.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val language by viewModel.language.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val quality by viewModel.quality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val autoDownloadLikedSongs by viewModel.autoDownloadLikedSongs.collectAsStateWithLifecycle()
    val videoDownloadQuality by viewModel.videoDownloadQuality.collectAsStateWithLifecycle()
    val keepYoutubePlaylistOffline by viewModel.keepYouTubePlaylistOffline.collectAsStateWithLifecycle()
    val localTrackingEnabled by viewModel.localTrackingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val playVideo by remember { viewModel.playVideoInsteadOfAudio.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val radioAudioOnly by remember { viewModel.radioAudioOnly.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val videoQuality by viewModel.videoQuality.collectAsStateWithLifecycle()
    val sendData by remember { viewModel.sendBackToGoogle.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val normalizeVolume by remember { viewModel.normalizeVolume.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val skipSilent by remember { viewModel.skipSilent.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val savePlaybackState by remember { viewModel.savedPlaybackState.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val saveLastPlayed by remember { viewModel.saveRecentSongAndQueue.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val killServiceOnExit by remember { viewModel.killServiceOnExit.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = true)
    val mainLyricsProvider by viewModel.mainLyricsProvider.collectAsStateWithLifecycle()
    val youtubeSubtitleLanguage by viewModel.youtubeSubtitleLanguage.collectAsStateWithLifecycle()
    val spotifyLoggedIn by viewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val spotifyLyrics by viewModel.spotifyLyrics.collectAsStateWithLifecycle()
    val spotifyCanvas by viewModel.spotifyCanvas.collectAsStateWithLifecycle()
    val amAnimatedArtwork by viewModel.amAnimatedArtwork.collectAsStateWithLifecycle()
    val enableSponsorBlock by remember { viewModel.sponsorBlockEnabled.map { it == TRUE } }.collectAsStateWithLifecycle(initialValue = false)
    val skipSegments by viewModel.sponsorBlockCategories.collectAsStateWithLifecycle()
    val playerCache by viewModel.cacheSize.collectAsStateWithLifecycle()
    val downloadedCache by viewModel.downloadedCacheSize.collectAsStateWithLifecycle()
    val thumbnailCache by viewModel.thumbCacheSize.collectAsStateWithLifecycle()
    val canvasCache by viewModel.canvasCacheSize.collectAsStateWithLifecycle()
    val limitPlayerCache by viewModel.playerCacheLimit.collectAsStateWithLifecycle()
    val fraction by viewModel.fraction.collectAsStateWithLifecycle()
    val lastCheckUpdate by viewModel.lastCheckForUpdate.collectAsStateWithLifecycle()
    val explicitContentEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle()
    val usingProxy by viewModel.usingProxy.collectAsStateWithLifecycle()
    val proxyType by viewModel.proxyType.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()
    val proxyUsername by viewModel.proxyUsername.collectAsStateWithLifecycle()
    val proxyPassword by viewModel.proxyPassword.collectAsStateWithLifecycle()
    val autoCheckUpdate by viewModel.autoCheckUpdate.collectAsStateWithLifecycle()
    val aiProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val isHasApiKey by viewModel.isHasApiKey.collectAsStateWithLifecycle()
    val useAITranslation by viewModel.useAITranslation.collectAsStateWithLifecycle()
    val translationLanguage by viewModel.translationLanguage.collectAsStateWithLifecycle()
    val customModelId by viewModel.customModelId.collectAsStateWithLifecycle()
    val customOpenAIBaseUrl by viewModel.customOpenAIBaseUrl.collectAsStateWithLifecycle()
    val customOpenAIHeaders by viewModel.customOpenAIHeaders.collectAsStateWithLifecycle()
    val helpBuildLyricsDatabase by viewModel.helpBuildLyricsDatabase.collectAsStateWithLifecycle()
    val contributor by viewModel.contributor.collectAsStateWithLifecycle()
    val backupDownloaded by viewModel.backupDownloaded.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsStateWithLifecycle()
    val autoBackupMaxFiles by viewModel.autoBackupMaxFiles.collectAsStateWithLifecycle()
    val autoBackupLastTime by viewModel.autoBackupLastTime.collectAsStateWithLifecycle()
    val enableLiquidGlass by viewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val themeMode by sharedViewModel.getThemeMode().collectAsStateWithLifecycle(DataStoreManager.THEME_MODE_DARK)
    val themeColorSource by sharedViewModel.getThemeColorSource().collectAsStateWithLifecycle(DataStoreManager.THEME_COLOR_DEFAULT)
    val customThemeColorHex by sharedViewModel.getCustomThemeColor().collectAsStateWithLifecycle(DataStoreManager.DEFAULT_THEME_COLOR_HEX)
    val nowPlayingStyle by sharedViewModel.getNowPlayingStyle().collectAsStateWithLifecycle(DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY)
    val lyricsStyle by sharedViewModel.getLyricsStyle().collectAsStateWithLifecycle(DataStoreManager.LYRICS_STYLE_CLASSIC)
    val romanizationStored by sharedViewModel.getRomanizationLanguages().collectAsStateWithLifecycle("")
    val japaneseDictionaryState by viewModel.japaneseDictionaryState.collectAsStateWithLifecycle()
    var showColorPickerDialog by rememberSaveable { mutableStateOf(false) }
    val discordLoggedIn by viewModel.discordLoggedIn.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val syncFollowToYouTube by viewModel.syncFollowToYouTube.collectAsStateWithLifecycle()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
    val delayEnabled by viewModel.delayEnabled.collectAsStateWithLifecycle()
    val reverbEnabled by viewModel.reverbEnabled.collectAsStateWithLifecycle()
    val lastfmLoggedIn by viewModel.lastfmLoggedIn.collectAsStateWithLifecycle()
    val lastfmUsername by viewModel.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by viewModel.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val richPresenceEnabled by viewModel.richPresenceEnabled.collectAsStateWithLifecycle()
    val keepServiceAlive by viewModel.keepServiceAlive.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsStateWithLifecycle()
    val crossfadeDjMode by viewModel.crossfadeDjMode.collectAsStateWithLifecycle()
    val crossfadeSkipAlbum by viewModel.crossfadeSkipAlbum.collectAsStateWithLifecycle()
    val castState by viewModel.castState.collectAsStateWithLifecycle()
    val isCheckingUpdate by sharedViewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val checkForUpdateSubtitle by remember {
        derivedStateOf {
            if (isCheckingUpdate) {
                return@derivedStateOf runBlocking { getString(Res.string.checking) }
            } else {
                val lastCheckLong = lastCheckUpdate?.toLong() ?: 0L
                return@derivedStateOf runBlocking {
                    getString(
                        Res.string.last_checked_at,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(lastCheckLong)),
                    )
                }
            }
        }
    }
    var showYouTubeAccountDialog by rememberSaveable { mutableStateOf(false) }
    var showThirdPartyLibraries by rememberSaveable { mutableStateOf(false) }

    var expandedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggleCard(id: String) {
        expandedCardId = if (expandedCardId == id) null else id
    }

    LaunchedEffect(true) {
        viewModel.getAllGoogleAccount()
        viewModel.setUpdateChannel(DataStoreManager.GITHUB)
        viewModel.getData()
        viewModel.getThumbCacheSize(platformContext)
    }

    val settingListState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
    ) {
        LazyColumn(
            state = settingListState,
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .padding(horizontal = 14.dp),
        ) {
            item {
                Spacer(
                    Modifier.height(
                        with(localDensity) { topBarHeightPx.toDp() } + 18.dp
                    )
                )
            }

            // 1. ACCOUNT CARD
            item {
                SettingsCardItem(
                    title = "Account",
                    subtitle = "Manage login and integrations",
                    expanded = expandedCardId == "account",
                    onToggle = { toggleCard("account") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.youtube_account),
                        subtitle = stringResource(Res.string.manage_your_youtube_accounts),
                        onClick = {
                            viewModel.getAllGoogleAccount()
                            showYouTubeAccountDialog = true
                        },
                    )
                    SettingItem(
                        title = if (spotifyLoggedIn) stringResource(Res.string.log_out_from_spotify) else stringResource(Res.string.log_in_to_spotify),
                        subtitle = if (spotifyLoggedIn) stringResource(Res.string.logged_in) else stringResource(Res.string.intro_login_to_spotify),
                        onClick = {
                            if (spotifyLoggedIn) {
                                viewModel.confirmLogOut(runBlocking { getString(Res.string.log_out_from_spotify) }) { viewModel.setSpotifyLogIn(false) }
                            } else {
                                navController.navigate(SpotifyLoginDestination)
                            }
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.enable_spotify_lyrics),
                        subtitle = stringResource(Res.string.spotify_lyrícs_info),
                        switch = (spotifyLyrics to { viewModel.setSpotifyLyrics(it) }),
                        isEnable = spotifyLoggedIn,
                    )
                    SettingItem(
                        title = stringResource(Res.string.enable_canvas),
                        subtitle = stringResource(Res.string.canvas_info),
                        switch = (spotifyCanvas to { viewModel.setSpotifyCanvas(it) }),
                        isEnable = spotifyLoggedIn,
                    )
                    SettingItem(
                        title = stringResource(Res.string.enable_animated_artwork),
                        subtitle = stringResource(Res.string.animated_artwork_info),
                        switch = (amAnimatedArtwork to { viewModel.setAMAnimatedArtwork(it) }),
                    )
                    SettingItem(
                        title = if (discordLoggedIn) stringResource(Res.string.log_out_from_discord) else stringResource(Res.string.log_in_to_discord),
                        subtitle = if (discordLoggedIn) stringResource(Res.string.logged_in) else stringResource(Res.string.intro_login_to_discord),
                        onClick = {
                            if (discordLoggedIn) {
                                viewModel.confirmLogOut(runBlocking { getString(Res.string.log_out_from_discord) }) { viewModel.logOutDiscord() }
                            } else {
                                navController.navigate(DiscordLoginDestination)
                            }
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.enable_rich_presence),
                        subtitle = stringResource(Res.string.rich_presence_info),
                        switch = (richPresenceEnabled to { viewModel.setDiscordRichPresenceEnabled(it) }),
                        isEnable = discordLoggedIn,
                    )
                    if (viewModel.lastfmAvailable) {
                        SettingItem(
                            title = if (lastfmLoggedIn) stringResource(Res.string.log_out_from_lastfm) else stringResource(Res.string.log_in_to_lastfm),
                            subtitle = if (lastfmLoggedIn) stringResource(Res.string.logged_in_as, lastfmUsername) else stringResource(Res.string.intro_login_to_lastfm),
                            onClick = {
                                if (lastfmLoggedIn) {
                                    viewModel.confirmLogOut(runBlocking { getString(Res.string.log_out_from_lastfm) }) { viewModel.logOutLastfm() }
                                } else {
                                    navController.navigate(LastfmLoginDestination)
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.enable_scrobbling),
                            subtitle = stringResource(Res.string.scrobbling_info),
                            switch = (lastfmScrobbleEnabled to { viewModel.setLastfmScrobbleEnabled(it) }),
                            isEnable = lastfmLoggedIn,
                        )
                    }
                }
            }

            // 2. AI HUB CARD
            item {
                SettingsCardItem(
                    title = "AI Hub",
                    subtitle = "AI-powered lyrics and translations",
                    expanded = expandedCardId == "ai_hub",
                    onToggle = { toggleCard("ai_hub") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.ai_provider),
                        subtitle = when (aiProvider) {
                            DataStoreManager.AI_PROVIDER_OPENAI -> stringResource(Res.string.openai)
                            DataStoreManager.AI_PROVIDER_GEMINI -> stringResource(Res.string.gemini)
                            DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI -> stringResource(Res.string.openai_api_compatible)
                            else -> stringResource(Res.string.unknown)
                        },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.ai_provider) },
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = listOf(
                                            (aiProvider == DataStoreManager.AI_PROVIDER_OPENAI) to runBlocking { getString(Res.string.openai) },
                                            (aiProvider == DataStoreManager.AI_PROVIDER_GEMINI) to runBlocking { getString(Res.string.gemini) },
                                            (aiProvider == DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI) to runBlocking { getString(Res.string.openai_api_compatible) },
                                        ),
                                    ),
                                    confirm = runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setAIProvider(
                                            when (state.selectOne?.getSelected()) {
                                                runBlocking { getString(Res.string.gemini) } -> DataStoreManager.AI_PROVIDER_GEMINI
                                                runBlocking { getString(Res.string.openai_api_compatible) } -> DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI
                                                else -> DataStoreManager.AI_PROVIDER_OPENAI
                                            }
                                        )
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.ai_api_key),
                        subtitle = if (isHasApiKey) "XXXXXXXXXX" else "N/A",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.ai_api_key) },
                                    textField = SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.ai_api_key) },
                                        value = "",
                                        verifyCodeBlock = { (it.isNotEmpty()) to runBlocking { getString(Res.string.invalid_api_key) } },
                                    ),
                                    message = "",
                                    confirm = runBlocking { getString(Res.string.set) } to { state -> viewModel.setAIApiKey(state.textField?.value ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.custom_ai_model_id),
                        subtitle = customModelId.ifEmpty { stringResource(Res.string.default_models) },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.custom_ai_model_id) },
                                    textField = SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.custom_ai_model_id) },
                                        value = "",
                                        verifyCodeBlock = { (it.isNotEmpty() && !it.contains(" ")) to runBlocking { getString(Res.string.invalid) } },
                                    ),
                                    message = runBlocking { getString(Res.string.custom_model_id_messages) },
                                    confirm = runBlocking { getString(Res.string.set) } to { state -> viewModel.setCustomModelId(state.textField?.value ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    if (aiProvider == DataStoreManager.AI_PROVIDER_CUSTOM_OPENAI) {
                        SettingItem(
                            title = "Custom Base URL",
                            subtitle = customOpenAIBaseUrl.ifEmpty { "https://api.openai.com/v1/" },
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Custom Base URL",
                                        textField = SettingAlertState.TextFieldData(
                                            label = "Base URL",
                                            value = customOpenAIBaseUrl,
                                            verifyCodeBlock = { (it.isEmpty() || it.startsWith("http")) to "Invalid URL format" },
                                        ),
                                        message = "Enter OpenAI-compatible API base URL (e.g., https://api.openai.com/v1/)",
                                        confirm = runBlocking { getString(Res.string.set) } to { state -> viewModel.setCustomOpenAIBaseUrl(state.textField?.value ?: "") },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    )
                                )
                            },
                        )
                        SettingItem(
                            title = "Custom Headers",
                            subtitle = if (customOpenAIHeaders.isNotEmpty()) "Configured" else "Not set",
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = "Custom Headers (JSON)",
                                        textField = SettingAlertState.TextFieldData(
                                            label = "Headers JSON",
                                            value = customOpenAIHeaders,
                                            verifyCodeBlock = { input ->
                                                if (input.isEmpty()) true to null
                                                else (input.trim().startsWith("{") && input.trim().endsWith("}")) to "Invalid JSON format"
                                            },
                                        ),
                                        message = "Enter custom headers in JSON format:\n{\"key1\":\"value1\",\"key2\":\"value2\"}",
                                        confirm = runBlocking { getString(Res.string.set) } to { state -> viewModel.setCustomOpenAIHeaders(state.textField?.value ?: "") },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    )
                                )
                            },
                        )
                    }
                    SettingItem(
                        title = stringResource(Res.string.use_ai_translation),
                        subtitle = stringResource(Res.string.use_ai_translation_description),
                        switch = (useAITranslation to { viewModel.setAITranslation(it) }),
                        isEnable = isHasApiKey,
                    )
                }
            }

            // 3. APPEARANCE CARD
            item {
                val themeModeLabels = listOf(
                    DataStoreManager.THEME_MODE_SYSTEM to stringResource(Res.string.theme_mode_system),
                    DataStoreManager.THEME_MODE_DARK to stringResource(Res.string.theme_mode_dark),
                    DataStoreManager.THEME_MODE_LIGHT to stringResource(Res.string.theme_mode_light),
                )
                val requiresAndroid12 = " (" + stringResource(Res.string.requires_android_12) + ")"
                val nowPlayingStyleLabels = listOf(
                    DataStoreManager.NOW_PLAYING_STYLE_SPOTIFY to stringResource(Res.string.now_playing_style_spotify),
                    DataStoreManager.NOW_PLAYING_STYLE_M3_EXPRESSIVE to stringResource(Res.string.now_playing_style_m3_expressive),
                    DataStoreManager.NOW_PLAYING_STYLE_APPLE_MUSIC to stringResource(Res.string.now_playing_style_apple_music) + requiresAndroid12,
                )
                val colorSourceLabels = buildList {
                    add(DataStoreManager.THEME_COLOR_DEFAULT to stringResource(Res.string.theme_color_default))
                    if (isWallpaperDynamicColorSupported()) {
                        add(DataStoreManager.THEME_COLOR_WALLPAPER to stringResource(Res.string.theme_color_wallpaper))
                    }
                    add(DataStoreManager.THEME_COLOR_CUSTOM to stringResource(Res.string.theme_color_custom))
                }
                val romanizationLabels = listOf(
                    RomanizationLanguage.JAPANESE to stringResource(Res.string.romanization_japanese),
                    RomanizationLanguage.KOREAN to stringResource(Res.string.romanization_korean),
                    RomanizationLanguage.CHINESE to stringResource(Res.string.romanization_chinese),
                    RomanizationLanguage.HINDI to stringResource(Res.string.romanization_hindi),
                    RomanizationLanguage.PUNJABI to stringResource(Res.string.romanization_punjabi),
                    RomanizationLanguage.RUSSIAN to stringResource(Res.string.romanization_russian),
                    RomanizationLanguage.UKRAINIAN to stringResource(Res.string.romanization_ukrainian),
                    RomanizationLanguage.SERBIAN to stringResource(Res.string.romanization_serbian),
                    RomanizationLanguage.BULGARIAN to stringResource(Res.string.romanization_bulgarian),
                    RomanizationLanguage.BELARUSIAN to stringResource(Res.string.romanization_belarusian),
                    RomanizationLanguage.KYRGYZ to stringResource(Res.string.romanization_kyrgyz),
                    RomanizationLanguage.MACEDONIAN to stringResource(Res.string.romanization_macedonian),
                )
                val romanizationSelected = RomanizationLanguage.parse(romanizationStored)

                SettingsCardItem(
                    title = "Appearance",
                    subtitle = "Themes, colors, and UI layout",
                    expanded = expandedCardId == "appearance",
                    onToggle = { toggleCard("appearance") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.theme),
                        subtitle = themeModeLabels.firstOrNull { it.first == themeMode }?.second ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.theme) },
                                    selectOne = SettingAlertState.SelectData(listSelect = themeModeLabels.map { (it.first == themeMode) to it.second }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state ->
                                        val selected = state.selectOne?.getSelected()
                                        themeModeLabels.firstOrNull { it.second == selected }?.first?.let { sharedViewModel.setThemeMode(it) }
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.now_playing_style),
                        subtitle = nowPlayingStyleLabels.firstOrNull { it.first == nowPlayingStyle }?.second ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.now_playing_style) },
                                    selectOne = SettingAlertState.SelectData(listSelect = nowPlayingStyleLabels.map { (it.first == nowPlayingStyle) to it.second }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state ->
                                        val selected = state.selectOne?.getSelected()
                                        nowPlayingStyleLabels.firstOrNull { it.second == selected }?.first?.let { sharedViewModel.setNowPlayingStyle(it) }
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    if (isLyricsBlurSupported()) {
                        val lyricsStyleLabels = listOf(
                            DataStoreManager.LYRICS_STYLE_CLASSIC to stringResource(Res.string.lyrics_style_classic),
                            DataStoreManager.LYRICS_STYLE_APPLE_MUSIC to stringResource(Res.string.lyrics_style_apple_music) + requiresAndroid12,
                        )
                        SettingItem(
                            title = stringResource(Res.string.lyrics_style),
                            subtitle = lyricsStyleLabels.firstOrNull { it.first == lyricsStyle }?.second ?: "",
                            onClick = {
                                viewModel.setAlertData(
                                    SettingAlertState(
                                        title = runBlocking { getString(Res.string.lyrics_style) },
                                        selectOne = SettingAlertState.SelectData(listSelect = lyricsStyleLabels.map { (it.first == lyricsStyle) to it.second }),
                                        confirm = runBlocking { getString(Res.string.change) } to { state ->
                                            val selected = state.selectOne?.getSelected()
                                            lyricsStyleLabels.firstOrNull { it.second == selected }?.first?.let { sharedViewModel.setLyricsStyle(it) }
                                        },
                                        dismiss = runBlocking { getString(Res.string.cancel) },
                                    )
                                )
                            },
                        )
                    }
                    SettingItem(
                        title = stringResource(Res.string.lyrics_romanization),
                        subtitle = if (romanizationSelected.isEmpty()) stringResource(Res.string.lyrics_romanization_description)
                        else romanizationLabels.filter { it.first in romanizationSelected }.joinToString(", ") { it.second },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.lyrics_romanization) },
                                    multipleSelect = SettingAlertState.SelectData(
                                        listSelect = romanizationLabels.map { (language, label) -> (language in romanizationSelected) to label },
                                    ),
                                    confirm = runBlocking { getString(Res.string.save) } to { state ->
                                        val chosen = state.multipleSelect?.getListSelected().orEmpty()
                                        val languages = romanizationLabels.filter { it.second in chosen }.map { it.first }.toSet()
                                        sharedViewModel.setRomanizationLanguages(languages)
                                        if (RomanizationLanguage.JAPANESE in languages) viewModel.downloadJapaneseDictionaryIfNeeded()
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.theme_color),
                        subtitle = colorSourceLabels.firstOrNull { it.first == themeColorSource }?.second ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.theme_color) },
                                    selectOne = SettingAlertState.SelectData(listSelect = colorSourceLabels.map { (it.first == themeColorSource) to it.second }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state ->
                                        val selected = state.selectOne?.getSelected()
                                        colorSourceLabels.firstOrNull { it.second == selected }?.first?.let {
                                            sharedViewModel.setThemeColorSource(it)
                                            if (it == DataStoreManager.THEME_COLOR_CUSTOM) showColorPickerDialog = true
                                        }
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    if (themeColorSource == DataStoreManager.THEME_COLOR_CUSTOM) {
                        SettingItem(
                            title = stringResource(Res.string.custom_color),
                            subtitle = "#${customThemeColorHex.takeLast(6)}",
                            smallSubtitle = true,
                            onClick = { showColorPickerDialog = true },
                        )
                    }
                    SettingItem(
                        title = stringResource(Res.string.translucent_bottom_navigation_bar),
                        subtitle = stringResource(Res.string.you_can_see_the_content_below_the_bottom_bar),
                        smallSubtitle = true,
                        switch = (enableTranslucentNavBar to { viewModel.setTranslucentBottomBar(it) }),
                    )
                    if (getPlatform() == Platform.Android) {
                        SettingItem(
                            title = stringResource(Res.string.enable_liquid_glass_effect),
                            subtitle = stringResource(Res.string.enable_liquid_glass_effect_description),
                            smallSubtitle = true,
                            switch = (enableLiquidGlass to { viewModel.setEnableLiquidGlass(it) }),
                        )
                    }
                }
            }

            // 4. PLAYER AND AUDIO CARD
            item {
                SettingsCardItem(
                    title = "Player and audio",
                    subtitle = "Playback, quality, and equalizer",
                    expanded = expandedCardId == "player_audio",
                    onToggle = { toggleCard("player_audio") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.quality),
                        subtitle = quality ?: "",
                        smallSubtitle = true,
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.quality) },
                                    selectOne = SettingAlertState.SelectData(listSelect = QUALITY.items.map { (it.toString() == quality) to it.toString() }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.changeQuality(state.selectOne?.getSelected()) },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.download_quality),
                        subtitle = downloadQuality ?: "",
                        smallSubtitle = true,
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.download_quality) },
                                    selectOne = SettingAlertState.SelectData(listSelect = QUALITY.items.map { (it.toString() == downloadQuality) to it.toString() }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> state.selectOne?.getSelected()?.let { viewModel.setDownloadQuality(it) } },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.video_quality),
                        subtitle = videoQuality ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.video_quality) },
                                    selectOne = SettingAlertState.SelectData(listSelect = VIDEO_QUALITY.items.map { (it.toString() == videoQuality) to it.toString() }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.changeVideoQuality(state.selectOne?.getSelected() ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.video_download_quality),
                        subtitle = videoDownloadQuality ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.video_download_quality) },
                                    selectOne = SettingAlertState.SelectData(listSelect = VIDEO_QUALITY.items.map { (it.toString() == videoDownloadQuality) to it.toString() }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.setVideoDownloadQuality(state.selectOne?.getSelected() ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.auto_download_liked_songs),
                        subtitle = stringResource(Res.string.auto_download_liked_songs_description),
                        smallSubtitle = true,
                        switch = (autoDownloadLikedSongs to { viewModel.setAutoDownloadLikedSongs(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.play_video_for_video_track_instead_of_audio_only),
                        subtitle = stringResource(Res.string.such_as_music_video_lyrics_video_podcasts_and_more),
                        smallSubtitle = true,
                        switch = (playVideo to { viewModel.setPlayVideoInsteadOfAudio(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.radio_audio_only),
                        subtitle = stringResource(Res.string.radio_audio_only_description),
                        smallSubtitle = true,
                        switch = (radioAudioOnly to { viewModel.setRadioAudioOnly(it) }),
                    )
                    if (getPlatform() == Platform.Android) {
                        SettingItem(
                            title = stringResource(Res.string.normalize_volume),
                            subtitle = stringResource(Res.string.balance_media_loudness),
                            switch = (normalizeVolume to { viewModel.setNormalizeVolume(it) }),
                        )
                        SettingItem(
                            title = stringResource(Res.string.skip_silent),
                            subtitle = stringResource(Res.string.skip_no_music_part),
                            switch = (skipSilent to { viewModel.setSkipSilent(it) }),
                        )
                    }
                    SettingItem(
                        title = stringResource(Res.string.equalizer),
                        subtitle = stringResource(Res.string.equalizer_description),
                        smallSubtitle = true,
                        switch = (equalizerEnabled to { viewModel.setEqualizerEnabled(it) }),
                    )
                    AnimatedVisibility(visible = equalizerEnabled) { EqualizerSection() }
                    SettingItem(
                        title = stringResource(Res.string.audio_delay),
                        subtitle = stringResource(Res.string.audio_delay_description),
                        smallSubtitle = true,
                        switch = (delayEnabled to { viewModel.setDelayEnabled(it) }),
                    )
                    AnimatedVisibility(visible = delayEnabled) { DelaySection() }
                    SettingItem(
                        title = stringResource(Res.string.audio_reverb),
                        subtitle = stringResource(Res.string.audio_reverb_description),
                        smallSubtitle = true,
                        switch = (reverbEnabled to { viewModel.setReverbEnabled(it) }),
                    )
                    AnimatedVisibility(visible = reverbEnabled) { ReverbSection() }
                    SettingItem(
                        title = stringResource(Res.string.crossfade),
                        subtitle = if (castState.isRemote) stringResource(Res.string.not_available_while_casting) else stringResource(Res.string.crossfade_description),
                        smallSubtitle = true,
                        switch = (crossfadeEnabled to { viewModel.setCrossfadeEnabled(it) }),
                        isEnable = !castState.isRemote,
                    )
                    AnimatedVisibility(visible = crossfadeEnabled) {
                        Column {
                            SettingItem(
                                title = stringResource(Res.string.crossfade_duration),
                                subtitle = if (castState.isRemote) stringResource(Res.string.not_available_while_casting)
                                else if (crossfadeDuration == DataStoreManager.CROSSFADE_DURATION_AUTO) stringResource(Res.string.crossfade_auto)
                                else "${crossfadeDuration / 1000}s",
                                isEnable = !castState.isRemote,
                                onClick = {
                                    viewModel.setAlertData(
                                        SettingAlertState(
                                            title = runBlocking { getString(Res.string.crossfade_duration) },
                                            selectOne = SettingAlertState.SelectData(
                                                listSelect = listOf(
                                                    (crossfadeDuration == DataStoreManager.CROSSFADE_DURATION_AUTO) to runBlocking { getString(Res.string.crossfade_auto) },
                                                    (crossfadeDuration == 1000) to "1s",
                                                    (crossfadeDuration == 2000) to "2s",
                                                    (crossfadeDuration == 3000) to "3s",
                                                    (crossfadeDuration == 5000) to "5s",
                                                    (crossfadeDuration == 8000) to "8s",
                                                    (crossfadeDuration == 10000) to "10s",
                                                ),
                                            ),
                                            confirm = runBlocking { getString(Res.string.change) } to { state ->
                                                val dur = when (state.selectOne?.getSelected()) {
                                                    runBlocking { getString(Res.string.crossfade_auto) } -> DataStoreManager.CROSSFADE_DURATION_AUTO
                                                    "1s" -> 1000
                                                    "2s" -> 2000
                                                    "3s" -> 3000
                                                    "5s" -> 5000
                                                    "8s" -> 8000
                                                    "10s" -> 10000
                                                    else -> 5000
                                                }
                                                viewModel.setCrossfadeDuration(dur)
                                            },
                                            dismiss = runBlocking { getString(Res.string.cancel) },
                                        )
                                    )
                                },
                            )
                            SettingItem(
                                title = stringResource(Res.string.crossfade_dj_mode),
                                subtitle = if (castState.isRemote) stringResource(Res.string.not_available_while_casting) else stringResource(Res.string.crossfade_dj_mode_description),
                                smallSubtitle = true,
                                switch = (crossfadeDjMode to { viewModel.setCrossfadeDjMode(it) }),
                                isEnable = !castState.isRemote,
                            )
                            SettingItem(
                                title = stringResource(Res.string.crossfade_skip_album),
                                subtitle = if (castState.isRemote) stringResource(Res.string.not_available_while_casting) else stringResource(Res.string.crossfade_skip_album_description),
                                smallSubtitle = true,
                                switch = (crossfadeSkipAlbum to { viewModel.setCrossfadeSkipAlbum(it) }),
                                isEnable = !castState.isRemote,
                            )
                        }
                    }
                    SettingItem(
                        title = stringResource(Res.string.save_playback_state),
                        subtitle = stringResource(Res.string.save_shuffle_and_repeat_mode),
                        switch = (savePlaybackState to { viewModel.setSavedPlaybackState(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.save_last_played),
                        subtitle = stringResource(Res.string.save_last_played_track_and_queue),
                        switch = (saveLastPlayed to { viewModel.setSaveLastPlayed(it) }),
                    )
                    if (getPlatform() == Platform.Android) {
                        SettingItem(
                            title = stringResource(Res.string.kill_service_on_exit),
                            subtitle = stringResource(Res.string.kill_service_on_exit_description),
                            switch = (killServiceOnExit to { viewModel.setKillServiceOnExit(it) }),
                        )
                        SettingItem(
                            title = stringResource(Res.string.keep_service_alive),
                            subtitle = stringResource(Res.string.keep_service_alive_description),
                            switch = (keepServiceAlive to { viewModel.setKeepServiceAlive(it) }),
                        )
                    }
                }
            }

            // 5. CONTENT CARD
            item {
                SettingsCardItem(
                    title = "Content",
                    subtitle = "Language, region, and providers",
                    expanded = expandedCardId == "content",
                    onToggle = { toggleCard("content") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.language),
                        subtitle = SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US"),
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.language) },
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = SUPPORTED_LANGUAGE.items.map { (it.toString() == SUPPORTED_LANGUAGE.getLanguageFromCode(language ?: "en-US")) to it.toString() },
                                    ),
                                    confirm = runBlocking { getString(Res.string.change) } to { state ->
                                        val code = SUPPORTED_LANGUAGE.getCodeFromLanguage(state.selectOne?.getSelected() ?: "English")
                                        sharedViewModel.activityRecreate()
                                        viewModel.changeLanguage(code)
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.content_country),
                        subtitle = location ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.content_country) },
                                    selectOne = SettingAlertState.SelectData(listSelect = SUPPORTED_LOCATION.items.map { (it.toString() == location) to it.toString() }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.changeLocation(state.selectOne?.getSelected() ?: "US") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.sync_follow_to_youtube),
                        subtitle = stringResource(Res.string.sync_follow_to_youtube_description),
                        smallSubtitle = true,
                        switch = (syncFollowToYouTube to { viewModel.setSyncFollowToYouTube(it) }),
                        isEnable = loggedIn == DataStoreManager.TRUE,
                    )
                    SettingItem(
                        title = stringResource(Res.string.send_back_listening_data_to_google),
                        subtitle = stringResource(Res.string.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in),
                        smallSubtitle = true,
                        switch = (sendData to { viewModel.setSendBackToGoogle(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.play_explicit_content),
                        subtitle = stringResource(Res.string.play_explicit_content_description),
                        switch = (explicitContentEnabled to { viewModel.setExplicitContentEnabled(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.keep_your_youtube_playlist_offline),
                        subtitle = stringResource(Res.string.keep_your_youtube_playlist_offline_description),
                        switch = (keepYoutubePlaylistOffline to { viewModel.setKeepYouTubePlaylistOffline(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.main_lyrics_provider),
                        subtitle = when (mainLyricsProvider) {
                            DataStoreManager.SIMPMUSIC -> stringResource(Res.string.simpmusic_lyrics)
                            DataStoreManager.YOUTUBE -> stringResource(Res.string.youtube_transcript)
                            DataStoreManager.LRCLIB -> stringResource(Res.string.lrclib)
                            DataStoreManager.BETTER_LYRICS -> stringResource(Res.string.better_lyrics)
                            else -> stringResource(Res.string.unknown)
                        },
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.main_lyrics_provider) },
                                    selectOne = SettingAlertState.SelectData(
                                        listSelect = listOf(
                                            (mainLyricsProvider == DataStoreManager.SIMPMUSIC) to runBlocking { getString(Res.string.simpmusic_lyrics) },
                                            (mainLyricsProvider == DataStoreManager.YOUTUBE) to runBlocking { getString(Res.string.youtube_transcript) },
                                            (mainLyricsProvider == DataStoreManager.LRCLIB) to runBlocking { getString(Res.string.lrclib) },
                                            (mainLyricsProvider == DataStoreManager.BETTER_LYRICS) to runBlocking { getString(Res.string.better_lyrics) },
                                        ),
                                    ),
                                    confirm = runBlocking { getString(Res.string.change) } to { state ->
                                        viewModel.setLyricsProvider(
                                            when (state.selectOne?.getSelected()) {
                                                runBlocking { getString(Res.string.youtube_transcript) } -> DataStoreManager.YOUTUBE
                                                runBlocking { getString(Res.string.lrclib) } -> DataStoreManager.LRCLIB
                                                runBlocking { getString(Res.string.better_lyrics) } -> DataStoreManager.BETTER_LYRICS
                                                else -> DataStoreManager.SIMPMUSIC
                                            }
                                        )
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.translation_language),
                        subtitle = translationLanguage ?: "",
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.translation_language) },
                                    textField = SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.translation_language) },
                                        value = translationLanguage ?: "",
                                        verifyCodeBlock = { (it.length == 2 && it.isTwoLetterCode()) to runBlocking { getString(Res.string.invalid_language_code) } },
                                    ),
                                    message = runBlocking { getString(Res.string.translation_language_message) },
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.setTranslationLanguage(state.textField?.value ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.youtube_subtitle_language),
                        subtitle = youtubeSubtitleLanguage,
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.youtube_subtitle_language) },
                                    textField = SettingAlertState.TextFieldData(
                                        label = runBlocking { getString(Res.string.youtube_subtitle_language) },
                                        value = youtubeSubtitleLanguage,
                                        verifyCodeBlock = { (it.length == 2 && it.isTwoLetterCode()) to runBlocking { getString(Res.string.invalid_language_code) } },
                                    ),
                                    message = runBlocking { getString(Res.string.youtube_subtitle_language_message) },
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.setYoutubeSubtitleLanguage(state.textField?.value ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.help_build_lyrics_database),
                        subtitle = stringResource(Res.string.help_build_lyrics_database_description),
                        switch = (helpBuildLyricsDatabase to { viewModel.setHelpBuildLyricsDatabase(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.contributor_name),
                        subtitle = contributor.first.ifEmpty { stringResource(Res.string.anonymous) },
                        isEnable = helpBuildLyricsDatabase,
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.contributor_name) },
                                    textField = SettingAlertState.TextFieldData(label = runBlocking { getString(Res.string.contributor_name) }, value = ""),
                                    message = "",
                                    confirm = runBlocking { getString(Res.string.set) } to { state -> viewModel.setContributorName(state.textField?.value ?: "") },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.proxy),
                        subtitle = stringResource(Res.string.proxy_description),
                        switch = (usingProxy to { viewModel.setUsingProxy(it) }),
                    )
                    Crossfade(usingProxy) { active ->
                        if (active) {
                            Column {
                                SettingItem(
                                    title = stringResource(Res.string.proxy_host),
                                    subtitle = proxyHost,
                                    onClick = {
                                        viewModel.setAlertData(
                                            SettingAlertState(
                                                title = runBlocking { getString(Res.string.proxy_host) },
                                                message = runBlocking { getString(Res.string.proxy_host_message) },
                                                textField = SettingAlertState.TextFieldData(
                                                    label = runBlocking { getString(Res.string.proxy_host) },
                                                    value = proxyHost,
                                                    verifyCodeBlock = { isValidProxyHost(it) to runBlocking { getString(Res.string.invalid_host) } },
                                                ),
                                                confirm = runBlocking { getString(Res.string.change) } to { state ->
                                                    viewModel.setProxy(proxyType, state.textField?.value ?: "", proxyPort)
                                                },
                                                dismiss = runBlocking { getString(Res.string.cancel) },
                                            )
                                        )
                                    },
                                )
                                SettingItem(
                                    title = stringResource(Res.string.proxy_port),
                                    subtitle = proxyPort.toString(),
                                    onClick = {
                                        viewModel.setAlertData(
                                            SettingAlertState(
                                                title = runBlocking { getString(Res.string.proxy_port) },
                                                message = runBlocking { getString(Res.string.proxy_port_message) },
                                                textField = SettingAlertState.TextFieldData(
                                                    label = runBlocking { getString(Res.string.proxy_port) },
                                                    value = proxyPort.toString(),
                                                    verifyCodeBlock = { (it.toIntOrNull() != null) to runBlocking { getString(Res.string.invalid_port) } },
                                                ),
                                                confirm = runBlocking { getString(Res.string.change) } to { state ->
                                                    viewModel.setProxy(proxyType, proxyHost, state.textField?.value?.toIntOrNull() ?: 0)
                                                },
                                                dismiss = runBlocking { getString(Res.string.cancel) },
                                            )
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 6. PRIVACY & TRACKING CARD
            item {
                SettingsCardItem(
                    title = "Privacy & Tracking",
                    subtitle = "History and tracking",
                    expanded = expandedCardId == "privacy",
                    onToggle = { toggleCard("privacy") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.local_tracking_title),
                        subtitle = stringResource(Res.string.local_tracking_description),
                        switch = (localTrackingEnabled to { viewModel.setLocalTrackingEnabled(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.clear_listening_history),
                        subtitle = stringResource(Res.string.clear_listening_history_description),
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_listening_history) },
                                    message = runBlocking { getString(Res.string.clear_listening_history_confirm) },
                                    confirm = runBlocking { getString(Res.string.clear) } to { viewModel.clearListeningHistory() },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.enable_sponsor_block),
                        subtitle = stringResource(Res.string.skip_sponsor_part_of_video),
                        switch = (enableSponsorBlock to { viewModel.setSponsorBlockEnabled(it) }),
                    )
                    val listName = SponsorBlockType.toList().map { it.displayString() }
                    SettingItem(
                        title = stringResource(Res.string.categories_sponsor_block),
                        subtitle = stringResource(Res.string.what_segments_will_be_skipped),
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.categories_sponsor_block) },
                                    multipleSelect = SettingAlertState.SelectData(
                                        listSelect = listName.mapIndexed { index, itm ->
                                            (skipSegments?.contains(SponsorBlockType.toList().getOrNull(index)?.value) == true) to itm
                                        },
                                    ),
                                    confirm = runBlocking { getString(Res.string.save) } to { state ->
                                        viewModel.setSponsorBlockCategories(
                                            state.multipleSelect?.getListSelected()?.map { selected -> listName.indexOf(selected) }
                                                ?.mapNotNull { s -> SponsorBlockType.toList().getOrNull(s)?.value }
                                                ?.toCollection(ArrayList()) ?: arrayListOf()
                                        )
                                    },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                        isEnable = enableSponsorBlock,
                    )
                }
            }

            // 7. STORAGE & BACKUP CARD
            item {
                SettingsCardItem(
                    title = "Storage",
                    subtitle = "Cache and downloads",
                    expanded = expandedCardId == "storage",
                    onToggle = { toggleCard("storage") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.player_cache),
                        subtitle = "${playerCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_player_cache) },
                                    message = null,
                                    confirm = runBlocking { getString(Res.string.clear) } to { viewModel.clearPlayerCache() },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.downloaded_cache),
                        subtitle = "${downloadedCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_downloaded_cache) },
                                    message = null,
                                    confirm = runBlocking { getString(Res.string.clear) } to { viewModel.clearDownloadedCache() },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.thumbnail_cache),
                        subtitle = "${thumbnailCache.bytesToMB()} MB",
                        onClick = {
                            viewModel.setBasicAlertData(
                                SettingBasicAlertState(
                                    title = runBlocking { getString(Res.string.clear_thumbnail_cache) },
                                    message = null,
                                    confirm = runBlocking { getString(Res.string.clear) } to { viewModel.clearThumbnailCache(platformContext) },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.limit_player_cache),
                        subtitle = LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache).toString(),
                        onClick = {
                            viewModel.setAlertData(
                                SettingAlertState(
                                    title = runBlocking { getString(Res.string.limit_player_cache) },
                                    selectOne = SettingAlertState.SelectData(listSelect = LIMIT_CACHE_SIZE.items.map { item -> (item == LIMIT_CACHE_SIZE.getItemFromData(limitPlayerCache)) to item.toString() }),
                                    confirm = runBlocking { getString(Res.string.change) } to { state -> viewModel.setPlayerCacheLimit(LIMIT_CACHE_SIZE.getDataFromItem(state.selectOne?.getSelected())) },
                                    dismiss = runBlocking { getString(Res.string.cancel) },
                                )
                            )
                        },
                    )
                    Box(Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .onGloballyPositioned { coordinates -> with(localDensity) { width = coordinates.size.width.toDp().value.toInt() } },
                        ) {
                            item { Box(modifier = Modifier.width((fraction.otherApp * width).dp).background(md_theme_dark_primary).fillMaxHeight()) }
                            item { Box(modifier = Modifier.width((fraction.downloadCache * width).dp).background(Color(0xD540FF17)).fillMaxHeight()) }
                            item { Box(modifier = Modifier.width((fraction.playerCache * width).dp).background(Color(0xD5FFFF00)).fillMaxHeight()) }
                            item { Box(modifier = Modifier.width((fraction.canvasCache * width).dp).background(Color.Cyan).fillMaxHeight()) }
                            item { Box(modifier = Modifier.width((fraction.thumbCache * width).dp).background(Color.Magenta).fillMaxHeight()) }
                            item { Box(modifier = Modifier.width((fraction.freeSpace * width).dp).background(Color.DarkGray).fillMaxHeight()) }
                        }
                    }
                    SettingItem(
                        title = stringResource(Res.string.backup_downloaded),
                        subtitle = stringResource(Res.string.backup_downloaded_description),
                        switch = (backupDownloaded to { viewModel.setBackupDownloaded(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.backup),
                        subtitle = stringResource(Res.string.save_all_your_playlist_data),
                        onClick = { coroutineScope.launch { backupLauncher.launch() } },
                    )
                    SettingItem(
                        title = stringResource(Res.string.restore_your_data),
                        subtitle = stringResource(Res.string.restore_your_saved_data),
                        onClick = { coroutineScope.launch { restoreLauncher.launch() } },
                    )
                    SettingItem(
                        title = stringResource(Res.string.import_data),
                        subtitle = stringResource(Res.string.import_playlists_from_other_apps),
                        onClick = { coroutineScope.launch { importLauncher.launch() } },
                    )
                }
            }

            // 8. ABOUT DHUN-MUSIC CARD
            item {
                SettingsCardItem(
                    title = "About DHUN-Music",
                    subtitle = "Version, repository & updates",
                    expanded = expandedCardId == "about",
                    onToggle = { toggleCard("about") }
                ) {
                    SettingItem(
                        title = stringResource(Res.string.version),
                        subtitle = stringResource(Res.string.version_format, VersionManager.getVersionName()),
                    )
                    SettingItem(
                        title = stringResource(Res.string.auto_check_for_update),
                        subtitle = stringResource(Res.string.auto_check_for_update_description),
                        switch = (autoCheckUpdate to { viewModel.setAutoCheckUpdate(it) }),
                    )
                    SettingItem(
                        title = stringResource(Res.string.update_channel),
                        subtitle = "DHUN-Music GitHub Release",
                    )
                    SettingItem(
                        title = stringResource(Res.string.check_for_update),
                        subtitle = checkForUpdateSubtitle,
                        onClick = { sharedViewModel.checkForUpdate() },
                    )
                    SettingItem(
                        title = "GitHub Repository",
                        subtitle = "silenteye1/DHUN-Music",
                        onClick = { uriHandler.openUri("https://github.com/silenteye1/DHUN-Music") },
                    )
                    SettingItem(
                        title = stringResource(Res.string.third_party_libraries),
                        subtitle = stringResource(Res.string.description_and_licenses),
                        onClick = { showThirdPartyLibraries = true },
                    )
                }
            }

            item { EndOfPage() }
        }

        // Floating Liquid Glass Header Capsule
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .onGloballyPositioned { coordinates ->
                    topBarHeightPx = coordinates.size.height
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = Color.Black.copy(alpha = 0.55f),
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .hazeEffect(hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    }
                    .background(
                        if (isDark) Color(14, 14, 18).copy(alpha = 0.45f)
                        else Color.White.copy(alpha = 0.35f),
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.50f),
                                Color.White.copy(alpha = 0.12f),
                                Color.Black.copy(alpha = 0.25f),
                            ),
                        ),
                        shape = RoundedCornerShape(26.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(
                            width = 0.8.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                            shape = CircleShape,
                        )
                        .clickable { navController.navigateUp() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SimpIcons.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Text(
                    text = stringResource(Res.string.settings),
                    style = typo().titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    // Color picker
    if (showColorPickerDialog) {
        val presetColors = listOf("FF8ECAE6", "FF4C82EF", "FF9B72CF", "FFEF6C9B", "FFEF5350", "FFF4A340", "FFFFCA28", "FF66BB6A", "FF26A69A", "FFBDBDBD")
        var pendingHex by rememberSaveable { mutableStateOf(customThemeColorHex.takeLast(6)) }
        val parsedColor = parseThemeColorHex(pendingHex)
        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text(text = stringResource(Res.string.custom_color), style = typo().titleSmall) },
            text = {
                Column {
                    presetColors.chunked(5).forEach { rowColors ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            rowColors.forEach { hex ->
                                val color = parseThemeColorHex(hex) ?: Color.Gray
                                val isSelected = pendingHex.equals(hex.takeLast(6), ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, shape = CircleShape)
                                        .clickable { pendingHex = hex.takeLast(6) },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = pendingHex,
                        onValueChange = { pendingHex = it.removePrefix("#").take(8).uppercase() },
                        label = { Text("HEX") },
                        prefix = { Text("#") },
                        singleLine = true,
                        isError = parsedColor == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsedColor != null,
                    onClick = {
                        parsedColor?.let {
                            val argb = "FF${pendingHex.takeLast(6).uppercase()}"
                            sharedViewModel.setCustomThemeColor(argb)
                            sharedViewModel.setThemeColorSource(DataStoreManager.THEME_COLOR_CUSTOM)
                        }
                        showColorPickerDialog = false
                    },
                ) { Text(text = stringResource(Res.string.change)) }
            },
            dismissButton = { TextButton(onClick = { showColorPickerDialog = false }) { Text(text = stringResource(Res.string.cancel)) } },
        )
    }

    // Account dialog
    if (showYouTubeAccountDialog) {
        BasicAlertDialog(onDismissRequest = { showYouTubeAccountDialog = false }, modifier = Modifier.wrapContentSize()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                val googleAccounts by viewModel.googleAccounts.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            IconButton(onClick = { showYouTubeAccountDialog = false }, modifier = Modifier.align(Alignment.CenterStart)) {
                                Icon(SimpIcons.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(stringResource(Res.string.youtube_account), style = typo().titleMedium, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    if (googleAccounts is LocalResource.Success) {
                        val data = googleAccounts.data
                        if (!data.isNullOrEmpty()) {
                            items(data) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp).clickable { viewModel.setUsedAccount(it) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(Modifier.width(16.dp))
                                    AsyncImage(model = it.thumbnailUrl, contentDescription = it.name, modifier = Modifier.size(44.dp).clip(CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(it.name, style = typo().labelMedium)
                                        Text(it.email, style = typo().bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Column {
                            ActionButton(icon = SimpIcons.PeopleAlt, text = Res.string.guest) {
                                viewModel.setUsedAccount(null)
                                showYouTubeAccountDialog = false
                            }
                            ActionButton(icon = SimpIcons.PlaylistAdd, text = Res.string.add_an_account) {
                                showYouTubeAccountDialog = false
                                navController.navigate(LoginDestination)
                            }
                        }
                    }
                }
            }
        }
    }

    // Third party libraries bottom sheet
    if (showThirdPartyLibraries) {
        val libraries by produceLibraries { Res.readBytes("files/aboutlibraries.json").decodeToString() }
        val lazyListState = rememberLazyListState()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            onDismissRequest = { showThirdPartyLibraries = false },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = sheetState,
            shape = RectangleShape,
        ) {
            LibrariesContainer(
                libraries?.copy(libraries = libraries?.libraries?.distinctBy { it.name }?.toImmutableList() ?: emptyList<Library>().toImmutableList()),
                Modifier.fillMaxSize(),
                lazyListState = lazyListState,
                contentPadding = innerPadding,
                header = {
                    item {
                        TopAppBar(
                            title = { Text(stringResource(Res.string.third_party_libraries), style = typo().titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { sheetState.hide(); showThirdPartyLibraries = false } }) {
                                    Icon(SimpIcons.Close, null)
                                }
                            }
                        )
                    }
                }
            )
        }
    }

    // Dialogs
    importState?.let { progress -> ImportProgressDialog(progress = progress, onDismiss = importViewModel::dismiss) }
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsStateWithLifecycle()
    if (showLoadingDialog.first) LoadingDialog(true, showLoadingDialog.second)

    val basisAlertData by viewModel.basicAlertData.collectAsStateWithLifecycle()
    if (basisAlertData != null) {
        val alertBasicState = basisAlertData ?: return
        AlertDialog(
            onDismissRequest = { viewModel.setBasicAlertData(null) },
            title = { Text(text = alertBasicState.title, style = typo().titleSmall) },
            text = { alertBasicState.message?.let { Text(text = it) } },
            confirmButton = {
                TextButton(onClick = { alertBasicState.confirm.second.invoke(); viewModel.setBasicAlertData(null) }) {
                    Text(text = alertBasicState.confirm.first)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.setBasicAlertData(null) }) { Text(text = alertBasicState.dismiss) } },
        )
    }

    val alertData by viewModel.alertData.collectAsStateWithLifecycle()
    if (alertData != null) {
        val alertState = alertData ?: return
        AlertDialog(
            onDismissRequest = { viewModel.setAlertData(null) },
            title = { Text(text = alertState.title, style = typo().titleSmall) },
            text = {
                if (alertState.textField != null) {
                    TextField(
                        value = alertState.textField.value,
                        onValueChange = { viewModel.setAlertData(alertState.copy(textField = alertState.textField.copy(value = it))) },
                        label = { Text(text = alertState.textField.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (alertState.selectOne != null) {
                    LazyColumn(Modifier.padding(vertical = 6.dp).heightIn(0.dp, 450.dp)) {
                        items(alertState.selectOne.listSelect) { item ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    viewModel.setAlertData(alertState.copy(selectOne = alertState.selectOne.copy(listSelect = alertState.selectOne.listSelect.map { (it == item) to it.second })))
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = item.first, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo().bodyMedium)
                            }
                        }
                    }
                } else if (alertState.multipleSelect != null) {
                    LazyColumn(Modifier.padding(vertical = 6.dp).heightIn(0.dp, 450.dp)) {
                        items(alertState.multipleSelect.listSelect) { item ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    viewModel.setAlertData(alertState.copy(multipleSelect = alertState.multipleSelect.copy(listSelect = alertState.multipleSelect.listSelect.map { if (it == item) !it.first to it.second else it })))
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = item.first, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.second, style = typo().bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { alertState.confirm.second.invoke(alertState); viewModel.setAlertData(null) }) {
                    Text(text = alertState.confirm.first)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.setAlertData(null) }) { Text(text = alertState.dismiss) } }
        )
    }
}

@Composable
private fun SettingsCardItem(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color.Black.copy(alpha = 0.40f),
            )
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF16161A).copy(alpha = 0.65f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.30f),
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(
                            width = 0.8.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color.White.copy(alpha = 0.05f),
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SimpIcons.PlaylistAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF9E9EA4)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(250))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F0F12).copy(alpha = 0.70f))
                        .padding(bottom = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ImportProgressDialog(progress: ImportProgress, onDismiss: () -> Unit) {
    val finished = progress is ImportProgress.Success || progress is ImportProgress.Error
    AlertDialog(
        onDismissRequest = { if (finished) onDismiss() },
        title = { Text(text = stringResource(if (progress is ImportProgress.Error) Res.string.import_failed else Res.string.import_data)) },
        text = { Text(text = progress.toString()) },
        confirmButton = {
            if (finished) TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.ok)) }
        }
    )
}