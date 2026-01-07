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
    private val PHOTO_ID_KEY = stringPreferencesKey("photo_id")

    suspend fun savePhotoId(context: Context, photoId: String) {
        context.widgetDataStore.edit { preferences ->
            preferences[PHOTO_ID_KEY] = photoId
        }
    }

    suspend fun getPhotoId(context: Context): String? {
        return try {
            context.widgetDataStore.data
                .map { preferences -> preferences[PHOTO_ID_KEY] }
                .first()
        } catch (e: Exception) {
            null
        }
    }
}
