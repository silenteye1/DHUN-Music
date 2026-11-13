package com.maxrave.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.io.getHomeFolderPath
import createDataStore
import java.io.File

private val dataStoreInstance: DataStore<Preferences> by lazy {
    createDataStore(
        producePath = {
            val file = File(getHomeFolderPath(listOf(".simpmusic")), "$SETTINGS_FILENAME.preferences_pb")
            file.absolutePath
        }
    )
}

actual fun createDataStoreInstance(): DataStore<Preferences> = dataStoreInstance