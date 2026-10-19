package com.maxrave.simpmusic

import android.annotation.SuppressLint
import android.app.Application
import android.database.CursorWindow
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.maxrave.common.AppIdentity
import com.maxrave.data.di.loader.loadAllModules
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.di.viewModelModule
import com.maxrave.simpmusic.service.backup.AutoBackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.AppContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.simpmusic.crashlytics.configCrashlytics
import org.simpmusic.lastfm.configLastfm
import java.lang.reflect.Field
import java.util.concurrent.TimeUnit

class SimpMusicApplication :
    Application(),
    KoinComponent,
    SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStoreManager: DataStoreManager by inject()
    private lateinit var autoBackupScheduler: AutoBackupScheduler

    // Ek shared Singleton OkHttpClient instance reuse hoga image networking ke liye
    private val imageOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 2, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        configCrashlytics(this, BuildKonfig.sentryDsn)
        configLastfm(BuildKonfig.lastfmApiKey, BuildKonfig.lastfmSecret)
        startKoin {
            androidLogger(level = Level.DEBUG)
            androidContext(this@SimpMusicApplication)
            loadAllModules(
                AppIdentity(
                    applicationId = BuildConfig.APPLICATION_ID,
                    versionName = BuildConfig.VERSION_NAME,
                    platform = "Android ${Build.VERSION.RELEASE}",
                ),
            )
            loadKoinModules(viewModelModule)
        }
        // provide custom configuration
        val workConfig =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build()

        // initialize WorkManager
        WorkManager.initialize(this, workConfig)

        // Initialize and start AutoBackupScheduler
        autoBackupScheduler = AutoBackupScheduler(this, dataStoreManager)
        applicationScope.launch {
            autoBackupScheduler.observeAndSchedule()
        }

        CaocConfig.Builder
            .create()
            .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT)
            .enabled(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .errorDrawable(R.mipmap.ic_launcher_round)
            .logErrorOnRestart(false)
            .trackActivities(true)
            .minTimeBetweenCrashesMs(2000)
            .restartActivity(MainActivity::class.java)
            .apply()

        @SuppressLint("DiscouragedPrivateApi")
        val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
        field.isAccessible = true
        val expectSize = 100 * 1024 * 1024
        field.set(null, expectSize)

        AppContext.apply {
            set(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Logger.w("Terminate", "Checking")
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { imageOkHttpClient },
                    ),
                )
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25) // Device ki available RAM ka 25% cache
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024) // 256MB disk cache
                    .build()
            }
            .crossfade(true)
            .build()
}