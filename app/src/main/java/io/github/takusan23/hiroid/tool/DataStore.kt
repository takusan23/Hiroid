package io.github.takusan23.hiroid.tool

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object DataStoreKey {

    /** モデルのパス。File() に入れられる。 */
    val MODEL_FILE_PATH = stringPreferencesKey("model_filepath")

}