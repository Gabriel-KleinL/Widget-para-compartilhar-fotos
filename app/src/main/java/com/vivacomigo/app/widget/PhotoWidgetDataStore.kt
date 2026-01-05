package com.vivacomigo.app.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

object PhotoWidgetDataStore {
    private val PHOTO_URL_KEY = stringPreferencesKey("photo_url")

    suspend fun savePhotoUrl(context: Context, url: String) {
        context.widgetDataStore.edit { preferences ->
            preferences[PHOTO_URL_KEY] = url
        }
    }

    fun getPhotoUrl(context: Context): String? {
        return try {
            kotlinx.coroutines.runBlocking {
                context.widgetDataStore.data
                    .map { preferences -> preferences[PHOTO_URL_KEY] }
                    .first()
            }
        } catch (e: Exception) {
            null
        }
    }
}
