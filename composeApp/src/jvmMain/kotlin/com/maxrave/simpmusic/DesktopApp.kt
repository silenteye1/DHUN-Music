package com.maxrave.simpmusic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.SingleInstanceManager
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.ToastType
import com.maxrave.domain.notification.DesktopNotificationManager
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.extension.DesktopWindowChrome
import com.maxrave.simpmusic.ui.component.CustomTitleBar
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerManager
import com.maxrave.simpmusic.ui.mini_player.MiniPlayerWindow
import com.maxrave.simpmusic.ui.theme.isDarkTheme
import com.maxrave.simpmusic.utils.VersionManager
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.maxrave.simpmusic.viewModel.changeLanguageNative
import io.sentry.Sentry
import io.sentry.SentryLevel
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastHost
import multiplatform.network.cmptoast.showToast
import okhttp3.OkHttpClient
import okio.FileSystem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.mp.KoinPlatform.getKoin
import org.simpmusic.lastfm.configLastfm
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.app_name
import simpmusic.composeapp.generated.resources.circle_app_icon
import simpmusic.composeapp.generated.resources.close_miniplayer
import simpmusic.composeapp.generated.resources.explicit_content_blocked
import simpmusic.composeapp.generated.resources.open_app
import simpmusic.composeapp.generated.resources.open_miniplayer
import simpmusic.composeapp.generated.resources.quit_app
import simpmusic.composeapp.generated.resources.time_out_check_internet_connection_or_change_piped_instance_in_settings

private val DEEP_LINK_ARG = Regex("^[A-Za-z][A-Za-z0-9+.\\-]*://.+")

@OptIn(ExperimentalMaterial3Api::class)
fun runDesktopApp(args: Array<String> = emptyArray()) {
    CrashDialog.install()

    java.awt.Desktop.isDesktopSupported()

    System.setProperty("compose.swing.render.on.graphics", "false")
    System.setProperty("compose.interop.blending", "false")
    System.setProperty("compose.layers.type", "COMPONENT")
    System.setProperty("skiko.vsync.enabled", "true")

    val isMacOS = System.getProperty("os.name", "").contains("Mac", ignoreCase = true)
    if (isMacOS && java.awt.Desktop.isDesktopSupported()) {
        val desktop = java.awt.Desktop.getDesktop()
        try {
            desktop.setOpenURIHandler { event ->
                DesktopDeepLinkHandler.onNewUri(event.uri.toString())
            }
        } catch (_: UnsupportedOperationException) {
        }
        if (desktop.isSupported(java.awt.Desktop.Action.APP_EVENT_REOPENED)) {
            desktop.addAppEventListener(
                java.awt.desktop.AppReopenedListener { DesktopRestoreSignal.request() },
            )
        }
    }

    val deepLinkArg = args.firstOrNull()?.takeIf { DEEP_LINK_ARG.matches(it) }
    val isSingleInstance =
        SingleInstanceManager.isSingleInstance(
            onRestoreRequest = { DesktopRestoreSignal.request() },
        )
    if (!isSingleInstance) {
        deepLinkArg?.let { DesktopDeepLinkHandler.writePendingUri(it) }
        return
    }

    if (!isMacOS) {
        deepLinkArg?.let { DesktopDeepLinkHandler.onNewUri(it) }
    }

    startKoin {
        loadAllModules(
            AppIdentity(
                applicationId = "com.silenteye1.dhunmusic",
                versionName = BuildKonfig.versionName,
                platform = "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
            ),
        )
        loadKoinModules(viewModelModule)
    }

    val language =
        runBlocking {
            getKoin()
                .get<DataStoreManager>()
                .language
                .first()
                .substring(0..1)
        }
    changeLanguageNative(language)

    VersionManager.initialize()
    configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)
    if (BuildKonfig.sentryDsn.isNotEmpty()) {
        Sentry.init { options ->
            options.dsn = BuildKonfig.sentryDsn
            options.release = "simpmusic-desktop@${VersionManager.getVersionName()}"
            options.setDiagnosticLevel(SentryLevel.ERROR)
        }
    }

    val mediaPlayerHandler by inject<MediaPlayerHandler>(MediaPlayerHandler::class.java)
    mediaPlayerHandler.showToast = { type ->
        showToast(
            when (type) {
                ToastType.ExplicitContent -> {
                    runBlocking { getString(Res.string.explicit_content_blocked) }
                }

                is ToastType.PlayerError -> {
                    runBlocking { getString(Res.string.time_out_check_internet_connection_or_change_piped_instance_in_settings, type.error) }
                }
            },
        )
    }
    mediaPlayerHandler.pushPlayerError = { error ->
        Sentry.withScope { scope ->
            Sentry.captureMessage("Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
        }
    }

    WindowsProtocolRegistrar.register()

    val desktopNotificationManager = getKoin().get<DesktopNotificationManager>()
    desktopNotificationManager.initialize()

    val sharedViewModel = getKoin().get<SharedViewModel>()
    if (sharedViewModel.shouldCheckForUpdate()) {
        sharedViewModel.checkForUpdate()
    }

    DesktopDeepLinkHandler.listener = { intent ->
        sharedViewModel.setIntent(intent)
    }

    application {
        val windowState =
            rememberWindowState(
                size = DpSize(1500.dp, 860.dp),
            )
        var isVisible by remember { mutableStateOf(true) }
        val showNotificationPermissionDialog by
        desktopNotificationManager.shouldShowPermissionDialog.collectAsState()

        val dataStoreManager = remember { getKoin().get<DataStoreManager>() }
        val handleCloseRequest: () -> Unit = {
            val shouldKill = runBlocking {
                dataStoreManager.killServiceOnExit.first() == DataStoreManager.TRUE
            }
            if (shouldKill) {
                mediaPlayerHandler.release()
                exitApplication()
                kotlin.system.exitProcess(0)
            } else {
                isVisible = false
            }
        }

        LaunchedEffect(Unit) {
            DesktopRestoreSignal.requests.collect {
                isVisible = true
                windowState.isMinimized = false
                DesktopDeepLinkHandler.consumePendingUri()
            }
        }
        val openAppString = stringResource(Res.string.open_app)
        val quitAppString = stringResource(Res.string.quit_app)
        val openMiniPlayer = stringResource(Res.string.open_miniplayer)
        val closeMiniPlayer = stringResource(Res.string.close_miniplayer)
        val appName = stringResource(Res.string.app_name)
        val nowPlayingData by sharedViewModel.nowPlayingScreenData.collectAsState()
        val nowPlayingTitle = nowPlayingData.nowPlayingTitle
        val nowPlayingArtist = nowPlayingData.artistName
        val hasTrack = nowPlayingTitle.isNotBlank()
        val trayLabel: (String) -> String = { if (it.length > 40) it.take(39).trimEnd() + "…" else it }
        Tray(
            icon = painterResource(Res.drawable.circle_app_icon),
            tooltip =
                when {
                    hasTrack && nowPlayingArtist.isNotBlank() -> "$nowPlayingTitle — $nowPlayingArtist"
                    hasTrack -> nowPlayingTitle
                    else -> appName
                },
            primaryAction = {
                DesktopRestoreSignal.request()
            },
        ) {
            if (hasTrack) {
                Item(trayLabel(nowPlayingTitle), isEnabled = false)
                if (nowPlayingArtist.isNotBlank()) {
                    Item(trayLabel(nowPlayingArtist), isEnabled = false)
                }
                Divider()
            }
            if (!isVisible) {
                Item(openAppString) {
                    DesktopRestoreSignal.request()
                }
            }
            if (MiniPlayerManager.isOpen) {
                Item(closeMiniPlayer) {
                    MiniPlayerManager.isOpen = false
                }
            } else {
                Item(openMiniPlayer) {
                    MiniPlayerManager.isOpen = true
                }
            }
            Divider()
            Item(quitAppString) {
                mediaPlayerHandler.release()
                exitApplication()
                kotlin.system.exitProcess(0)
            }
        }

        val isVM =
            remember {
                val osName = System.getProperty("os.name", "")
                if (!osName.contains("Windows", ignoreCase = true)) {
                    return@remember false
                }
                val probes =
                    listOf(
                        listOf(
                            "powershell",
                            "-NoProfile",
                            "-Command",
                            "(Get-CimInstance Win32_ComputerSystem | " +
                                "Select-Object Manufacturer,Model | " +
                                "Format-List | Out-String).Trim()",
                        ),
                        listOf("wmic", "computersystem", "get", "manufacturer,model"),
                    )
                val sysInfo =
                    probes
                        .asSequence()
                        .mapNotNull { cmd ->
                            runCatching {
                                val p =
                                    ProcessBuilder(cmd)
                                        .redirectErrorStream(true)
                                        .start()
                                val out = p.inputStream.bufferedReader().readText()
                                if (p.waitFor() == 0 && out.isNotBlank()) out else null
                            }.getOrNull()
                        }
                        .firstOrNull()
                        .orEmpty()
                val vmTokens = listOf("Parallels", "VirtualBox", "VMware", "QEMU", "KVM", "Xen", "Hyper-V")
                vmTokens.any { sysInfo.contains(it, ignoreCase = true) } ||
                    System.getProperty("compose.window.no-transparent", "false").toBooleanStrictOrNull() == true
            }

        LaunchedEffect(isVM) {
            DesktopWindowChrome.customTitleBarVisible = !isVM
        }

        Window(
            onCloseRequest = handleCloseRequest,
            title = stringResource(Res.string.app_name),
            icon = painterResource(Res.drawable.circle_app_icon),
            undecorated = !isVM,
            transparent = false,
            state = windowState,
            visible = isVisible,
        ) {
            DisposableEffect(window) {
                val listener =
                    object : WindowAdapter() {
                        override fun windowGainedFocus(event: WindowEvent) {
                            desktopNotificationManager.refreshPermission()
                        }
                    }
                window.addWindowFocusListener(listener)
                onDispose {
                    window.removeWindowFocusListener(listener)
                }
            }

            LaunchedEffect(Unit) {
                DesktopRestoreSignal.requests.collect {
                    window.toFront()
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (!isVM) {
                    val themeMode by sharedViewModel
                        .getThemeMode()
                        .collectAsState(DataStoreManager.THEME_MODE_DARK)
                    val isDark = isDarkTheme(themeMode)
                    CustomTitleBar(
                        title = stringResource(Res.string.app_name),
                        windowState = windowState,
                        window = window,
                        onCloseRequest = handleCloseRequest,
                        containerColor = if (isDark) Color.Black else Color.White,
                        titleColor = if (isDark) Color.White else Color.Black,
                    )
                }

                val context = LocalPlatformContext.current
                val sharedOkHttp = remember { OkHttpClient() }
                setSingletonImageLoaderFactory {
                    ImageLoader
                        .Builder(context)
                        .components {
                            add(
                                OkHttpNetworkFetcherFactory(
                                    callFactory = { sharedOkHttp },
                                ),
                            )
                        }
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .memoryCache {
                            MemoryCache.Builder()
                                .maxSizeBytes(64L * 1024L * 1024L)
                                .build()
                        }
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .diskCache(
                            DiskCache
                                .Builder()
                                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                                .maxSizeBytes(256L * 1024L * 1024L)
                                .build(),
                        )
                        .crossfade(false)
                        .build()
                }

                App(
                    showDesktopNotificationPermissionDialog = showNotificationPermissionDialog,
                    onDismissDesktopNotificationPermissionDialog =
                        desktopNotificationManager::dismissPermissionDialog,
                    onOpenDesktopNotificationSettings =
                        desktopNotificationManager::openNotificationSettings,
                )

                ToastHost()
            }
        }

        if (MiniPlayerManager.isOpen) {
            MiniPlayerWindow(
                sharedViewModel = sharedViewModel,
                onCloseRequest = {
                    MiniPlayerManager.isOpen = false
                },
            )
        }
    }
}

private object DesktopRestoreSignal {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    fun request() {
        _requests.tryEmit(Unit)
    }
}