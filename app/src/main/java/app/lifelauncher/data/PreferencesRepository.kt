package app.lifelauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.lifelauncher.widgets.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {
    
    companion object {
        private val HOME_APPS_COUNT = intPreferencesKey("home_apps_count")
        private val ALIGNMENT = intPreferencesKey("alignment") // 0=left, 1=center, 2=right
        
        // Home app slots (package|class|user|label)
        private fun homeAppKey(slot: Int) = stringPreferencesKey("home_app_$slot")
        
        // Swipe apps
        private val SWIPE_LEFT_APP = stringPreferencesKey("swipe_left_app")
        private val SWIPE_RIGHT_APP = stringPreferencesKey("swipe_right_app")
        
        // Hidden apps (comma-separated package names)
        private val HIDDEN_APPS = stringPreferencesKey("hidden_apps")
        
        // Widget slots
        private val TOP_WIDGET = stringPreferencesKey("top_widget")
        private val BOTTOM_WIDGET = stringPreferencesKey("bottom_widget")
    }
    
    val homeAppsCount: Flow<Int> = context.dataStore.data.map { it[HOME_APPS_COUNT] ?: 4 }
    val alignment: Flow<Int> = context.dataStore.data.map { it[ALIGNMENT] ?: 0 }
    
    fun getHomeApp(slot: Int): Flow<AppInfo?> = context.dataStore.data.map { prefs ->
        prefs[homeAppKey(slot)]?.let { AppInfo.fromString(it) }
    }
    
    val swipeLeftApp: Flow<AppInfo?> = context.dataStore.data.map { prefs ->
        prefs[SWIPE_LEFT_APP]?.let { AppInfo.fromString(it) }
    }
    
    val swipeRightApp: Flow<AppInfo?> = context.dataStore.data.map { prefs ->
        prefs[SWIPE_RIGHT_APP]?.let { AppInfo.fromString(it) }
    }
    
    val hiddenApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[HIDDEN_APPS]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }
    
    val topWidget: Flow<WidgetType> = context.dataStore.data.map { prefs ->
        prefs[TOP_WIDGET]?.let { runCatching { WidgetType.valueOf(it) }.getOrNull() } ?: WidgetType.TODAYS_PLAN
    }
    
    val bottomWidget: Flow<WidgetType> = context.dataStore.data.map { prefs ->
        prefs[BOTTOM_WIDGET]?.let { runCatching { WidgetType.valueOf(it) }.getOrNull() } ?: WidgetType.NONE
    }
    
    suspend fun setHomeAppsCount(count: Int) {
        context.dataStore.edit { it[HOME_APPS_COUNT] = count.coerceIn(0, 8) }
    }
    
    suspend fun setAlignment(alignment: Int) {
        context.dataStore.edit { it[ALIGNMENT] = alignment.coerceIn(0, 2) }
    }
    
    suspend fun setHomeApp(slot: Int, app: AppInfo?) {
        context.dataStore.edit { prefs ->
            if (app != null) {
                prefs[homeAppKey(slot)] = app.toString()
            } else {
                prefs.remove(homeAppKey(slot))
            }
        }
    }
    
    suspend fun setSwipeLeftApp(app: AppInfo?) {
        context.dataStore.edit { prefs ->
            if (app != null) prefs[SWIPE_LEFT_APP] = app.toString()
            else prefs.remove(SWIPE_LEFT_APP)
        }
    }
    
    suspend fun setSwipeRightApp(app: AppInfo?) {
        context.dataStore.edit { prefs ->
            if (app != null) prefs[SWIPE_RIGHT_APP] = app.toString()
            else prefs.remove(SWIPE_RIGHT_APP)
        }
    }
    
    suspend fun toggleHiddenApp(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HIDDEN_APPS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) current.remove(packageName)
            else current.add(packageName)
            prefs[HIDDEN_APPS] = current.joinToString(",")
        }
    }
    
    suspend fun setTopWidget(type: WidgetType) {
        context.dataStore.edit { it[TOP_WIDGET] = type.name }
    }
    
    suspend fun setBottomWidget(type: WidgetType) {
        context.dataStore.edit { it[BOTTOM_WIDGET] = type.name }
    }
}

data class AppInfo(
    val packageName: String,
    val className: String?,
    val label: String
) {
    override fun toString() = "$packageName|${className ?: ""}|$label"
    
    companion object {
        fun fromString(s: String): AppInfo? {
            val parts = s.split("|")
            if (parts.size < 3) return null
            return AppInfo(
                packageName = parts[0],
                className = parts[1].ifBlank { null },
                label = parts[2]
            )
        }
    }
}
