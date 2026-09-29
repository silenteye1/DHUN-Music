import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.maxrave.logger.Logger
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { ex ->
                Logger.e("DataStore", "DataStore file corrupted: ${ex.message}")
                emptyPreferences()
            }
        ),
        produceFile = { producePath().toPath() }
    )
}