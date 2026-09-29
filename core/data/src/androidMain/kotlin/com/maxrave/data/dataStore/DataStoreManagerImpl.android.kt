package com.maxrave.data.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.maxrave.common.SETTINGS_FILENAME
import createDataStore
import java.io.File
import org.koin.mp.KoinPlatform.getKoin

actual fun createDataStoreInstance(): DataStore<Preferences> {
    return createDataStore(
        producePath = {
            val context = getKoin().get<Context>()
            val file = File(context.filesDir, "datastore/$SETTINGS_FILENAME.preferences_pb")
            file.parentFile?.mkdirs()
            file.absolutePath
        }
    )
}