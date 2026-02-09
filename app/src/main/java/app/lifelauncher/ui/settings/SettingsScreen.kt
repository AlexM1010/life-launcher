package app.lifelauncher.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lifelauncher.data.PreferencesRepository
import app.lifelauncher.helper.ScreenTimeHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    prefsRepo: PreferencesRepository,
    onNavigateToAppSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val homeAppsCount by prefsRepo.homeAppsCount.collectAsState(initial = 4)
    val alignment by prefsRepo.alignment.collectAsState(initial = 0)
    val swipeLeftApp by prefsRepo.swipeLeftApp.collectAsState(initial = null)
    val swipeRightApp by prefsRepo.swipeRightApp.collectAsState(initial = null)
    
    var hasUsagePermission by remember { mutableStateOf(ScreenTimeHelper.hasUsagePermission(context)) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Home Apps Count
        item {
            SettingsSection(title = "Home Apps") {
                SliderSetting(
                    label = "Number of apps",
                    value = homeAppsCount,
                    range = 0..8,
                    onValueChange = { scope.launch { prefsRepo.setHomeAppsCount(it) } }
                )
            }
        }
        
        // Alignment
        item {
            SettingsSection(title = "Alignment") {
                SegmentedButton(
                    options = listOf("Left", "Center", "Right"),
                    selectedIndex = alignment,
                    onSelect = { scope.launch { prefsRepo.setAlignment(it) } }
                )
            }
        }
        
        // Swipe Gestures
        item {
            SettingsSection(title = "Swipe Gestures") {
                AppSelectSetting(
                    label = "Swipe left",
                    currentApp = swipeLeftApp?.label,
                    onClick = { onNavigateToAppSelect("swipe_left") }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppSelectSetting(
                    label = "Swipe right",
                    currentApp = swipeRightApp?.label,
                    onClick = { onNavigateToAppSelect("swipe_right") }
                )
            }
        }
        
        // Permissions
        item {
            SettingsSection(title = "Features") {
                ToggleSetting(
                    label = "Screen time",
                    description = if (hasUsagePermission) "Enabled" else "Requires usage access",
                    isEnabled = hasUsagePermission,
                    onClick = {
                        if (!hasUsagePermission) {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        }
                    }
                )
            }
        }
        
        // Close button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Done",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onBackground,
                activeTrackColor = MaterialTheme.colorScheme.onBackground,
                inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            Surface(
                color = if (isSelected) 
                    MaterialTheme.colorScheme.onBackground 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(index) }
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.background 
                    else 
                        MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AppSelectSetting(
    label: String,
    currentApp: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = currentApp ?: "None",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = if (currentApp != null) 1f else 0.4f
            )
        )
    }
}

@Composable
private fun ToggleSetting(
    label: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Text(
            text = if (isEnabled) "✓" else "→",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = if (isEnabled) 1f else 0.4f
            )
        )
    }
}
