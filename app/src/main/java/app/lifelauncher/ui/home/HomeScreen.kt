package app.lifelauncher.ui.home

import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import app.lifelauncher.data.AppInfo
import app.lifelauncher.data.AppListRepository
import app.lifelauncher.data.PreferencesRepository
import app.lifelauncher.helper.ScreenTimeHelper
import app.lifelauncher.widgets.WidgetHost
import app.lifelauncher.widgets.WidgetType
import app.lifelauncher.widgets.data.UserEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun HomeScreen(
    prefsRepo: PreferencesRepository,
    appListRepo: AppListRepository,
    onOpenDrawer: (selectFor: String?) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val homeAppsCount by prefsRepo.homeAppsCount.collectAsState(initial = 4)
    val alignment by prefsRepo.alignment.collectAsState(initial = 0)
    val topWidget by prefsRepo.topWidget.collectAsState(initial = WidgetType.TODAYS_PLAN)
    val bottomWidget by prefsRepo.bottomWidget.collectAsState(initial = WidgetType.NONE)
    
    // Collect home apps
    val homeApps = remember { mutableStateListOf<AppInfo?>() }
    LaunchedEffect(Unit) {
        repeat(8) { slot ->
            launch {
                prefsRepo.getHomeApp(slot).collect { homeApps.getOrNull(slot)?.let { homeApps[slot] = it } ?: run {
                    while (homeApps.size <= slot) homeApps.add(null)
                    homeApps[slot] = it
                }}
            }
        }
    }
    
    val swipeLeftApp by prefsRepo.swipeLeftApp.collectAsState(initial = null)
    val swipeRightApp by prefsRepo.swipeRightApp.collectAsState(initial = null)
    
    // Gesture state
    var gestureHandled by remember { mutableStateOf(false) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    
    val horizontalAlignment = when (alignment) {
        1 -> Alignment.CenterHorizontally
        2 -> Alignment.End
        else -> Alignment.Start
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureHandled = false
                        dragStartX = offset.x
                        dragStartY = offset.y
                    },
                    onDragEnd = {},
                    onDrag = { change, _ ->
                        if (gestureHandled) return@detectDragGestures
                        
                        val totalDragX = change.position.x - dragStartX
                        val totalDragY = change.position.y - dragStartY
                        val threshold = 100f
                        
                        when {
                            // Swipe up
                            totalDragY < -threshold && abs(totalDragY) > abs(totalDragX) * 1.5f -> {
                                gestureHandled = true
                                onOpenDrawer(null)
                            }
                            // Swipe down
                            totalDragY > threshold && abs(totalDragY) > abs(totalDragX) * 1.5f -> {
                                gestureHandled = true
                                expandNotifications(context)
                            }
                            // Swipe left
                            totalDragX < -threshold && abs(totalDragX) > abs(totalDragY) * 1.5f -> {
                                gestureHandled = true
                                swipeLeftApp?.let { appListRepo.launchApp(it.packageName, it.className) }
                            }
                            // Swipe right
                            totalDragX > threshold && abs(totalDragX) > abs(totalDragY) * 1.5f -> {
                                gestureHandled = true
                                swipeRightApp?.let { appListRepo.launchApp(it.packageName, it.className) }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onOpenSettings()
                    }
                )
            }
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 48.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            // Date & Time
            DateTimeSection(
                horizontalAlignment = horizontalAlignment,
                onClockClick = { openAlarm(context) },
                onDateClick = { openCalendar(context) }
            )
            
            // Top Widget Slot
            if (topWidget != WidgetType.NONE) {
                Spacer(modifier = Modifier.height(24.dp))
                WidgetHost(
                    widgetType = topWidget,
                    horizontalAlignment = horizontalAlignment,
                    onOpenLifeManager = { openLifeManager(context) },
                    onOpenEvent = { event -> openEvent(context, event) }
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Home Apps
            Column(
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(homeAppsCount.coerceAtMost(8)) { slot ->
                    val app = homeApps.getOrNull(slot)
                    HomeAppItem(
                        label = app?.label ?: "App",
                        isEmpty = app == null,
                        onClick = {
                            app?.let { appListRepo.launchApp(it.packageName, it.className) }
                        },
                        onLongClick = {
                            onOpenDrawer("home_$slot")
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom Widget Slot
            if (bottomWidget != WidgetType.NONE) {
                WidgetHost(
                    widgetType = bottomWidget,
                    horizontalAlignment = horizontalAlignment,
                    onOpenLifeManager = { openLifeManager(context) },
                    onOpenEvent = { event -> openEvent(context, event) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DateTimeSection(
    horizontalAlignment: Alignment.Horizontal,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }
    var screenTime by remember { mutableStateOf("") }
    var battery by remember { mutableIntStateOf(0) }
    
    // Update time and screen time periodically
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = timeFormat.format(Date())
            currentDate = dateFormat.format(Date())
            
            val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as BatteryManager
            battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            if (ScreenTimeHelper.hasUsagePermission(context)) {
                val millis = ScreenTimeHelper.getTodayScreenTime(context)
                screenTime = ScreenTimeHelper.formatScreenTime(millis)
            }
            
            delay(60_000) // Update every minute
        }
    }
    
    Column(horizontalAlignment = horizontalAlignment) {
        Text(
            text = currentTime,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable { onClockClick() }
        )
        
        val infoText = buildString {
            append(currentDate)
            if (battery > 0) append(" • $battery%")
            if (screenTime.isNotBlank()) append(" • $screenTime")
        }
        
        Text(
            text = infoText,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.clickable { onDateClick() }
        )
    }
}

@Composable
private fun HomeAppItem(
    label: String,
    isEmpty: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Text(
        text = label,
        fontSize = 24.sp,
        color = if (isEmpty) 
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        else 
            MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (!isEmpty) onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(vertical = 8.dp)
    )
}

private fun expandNotifications(context: android.content.Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openAlarm(context: android.content.Context) {
    try {
        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openCalendar(context: android.content.Context) {
    try {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openLifeManager(context: android.content.Context) {
    try {
        // Open Life Manager web app - adjust URL as needed
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:5173"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to calendar if web app not accessible
        openCalendar(context)
    }
}

private fun openEvent(context: android.content.Context, event: UserEvent) {
    try {
        // Try meeting link first, then location, then calendar
        val uri = event.meetingLink?.let { Uri.parse(it) }
            ?: event.location?.takeIf { it.startsWith("http") }?.let { Uri.parse(it) }
            ?: CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
        
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        openCalendar(context)
    }
}
