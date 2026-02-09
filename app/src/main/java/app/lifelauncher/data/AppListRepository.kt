package app.lifelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.text.Collator

data class InstalledApp(
    val label: String,
    val packageName: String,
    val className: String,
    val isNew: Boolean = false
)

class AppListRepository(private val context: Context) {
    
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: Flow<List<InstalledApp>> = _apps
    
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val collator = Collator.getInstance()
    
    suspend fun refresh() = withContext(Dispatchers.IO) {
        val appList = mutableListOf<InstalledApp>()
        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L
        
        for (profile in userManager.userProfiles) {
            for (app in launcherApps.getActivityList(null, profile)) {
                // Skip self
                if (app.applicationInfo.packageName == context.packageName) continue
                
                appList.add(
                    InstalledApp(
                        label = app.label.toString(),
                        packageName = app.applicationInfo.packageName,
                        className = app.componentName.className,
                        isNew = (now - app.firstInstallTime) < oneHour
                    )
                )
            }
        }
        
        appList.sortWith { a, b -> collator.compare(a.label.lowercase(), b.label.lowercase()) }
        _apps.value = appList
    }
    
    fun launchApp(packageName: String, className: String? = null) {
        val intent = if (className != null) {
            Intent().apply {
                setClassName(packageName, className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }
        
        intent?.let { context.startActivity(it) }
    }
}
