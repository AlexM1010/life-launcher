package app.lifelauncher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lifelauncher.data.AppInfo
import app.lifelauncher.data.AppListRepository
import app.lifelauncher.data.InstalledApp
import app.lifelauncher.data.PreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun AppDrawerScreen(
    appListRepo: AppListRepository,
    prefsRepo: PreferencesRepository,
    hiddenApps: Set<String>,
    selectFor: String?,
    onAppSelected: (InstalledApp, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val apps by appListRepo.apps.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Refresh apps on first load
    LaunchedEffect(Unit) {
        appListRepo.refresh()
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
    // Filter apps
    val filteredApps = remember(apps, searchQuery, hiddenApps) {
        apps.filter { app ->
            !hiddenApps.contains(app.packageName) &&
            (searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true))
        }
    }
    
    // Auto-launch when only one app matches
    LaunchedEffect(filteredApps.size, searchQuery) {
        if (filteredApps.size == 1 && searchQuery.isNotBlank()) {
            val app = filteredApps.first()
            if (selectFor != null) {
                scope.launch {
                    val appInfo = AppInfo(app.packageName, app.className, app.label)
                    when {
                        selectFor.startsWith("home_") -> {
                            val slot = selectFor.removePrefix("home_").toIntOrNull() ?: 0
                            prefsRepo.setHomeApp(slot, appInfo)
                        }
                        selectFor == "swipe_left" -> prefsRepo.setSwipeLeftApp(appInfo)
                        selectFor == "swipe_right" -> prefsRepo.setSwipeRightApp(appInfo)
                    }
                }
                onAppSelected(app, selectFor)
            } else {
                appListRepo.launchApp(app.packageName, app.className)
                onDismiss()
            }
        }
    }
    
    // Track drag for dismiss gesture
    var dragY by remember { mutableFloatStateOf(0f) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragY = 0f },
                    onDrag = { _, dragAmount ->
                        dragY += dragAmount.y
                        if (dragY < -150f) {
                            onDismiss()
                        }
                    }
                )
            }
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp)
    ) {
        // Search field
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { 
                Text(
                    if (selectFor != null) "Select an app" else "Search apps",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ) 
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    // Launch first app in filtered list
                    filteredApps.firstOrNull()?.let { app ->
                        if (selectFor != null) {
                            // Save selection
                            scope.launch {
                                val appInfo = AppInfo(app.packageName, app.className, app.label)
                                when {
                                    selectFor.startsWith("home_") -> {
                                        val slot = selectFor.removePrefix("home_").toIntOrNull() ?: 0
                                        prefsRepo.setHomeApp(slot, appInfo)
                                    }
                                    selectFor == "swipe_left" -> prefsRepo.setSwipeLeftApp(appInfo)
                                    selectFor == "swipe_right" -> prefsRepo.setSwipeRightApp(appInfo)
                                }
                            }
                            onAppSelected(app, selectFor)
                        } else {
                            appListRepo.launchApp(app.packageName, app.className)
                            onDismiss()
                        }
                    }
                }
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppListItem(
                    app = app,
                    onClick = {
                        if (selectFor != null) {
                            scope.launch {
                                val appInfo = AppInfo(app.packageName, app.className, app.label)
                                when {
                                    selectFor.startsWith("home_") -> {
                                        val slot = selectFor.removePrefix("home_").toIntOrNull() ?: 0
                                        prefsRepo.setHomeApp(slot, appInfo)
                                    }
                                    selectFor == "swipe_left" -> prefsRepo.setSwipeLeftApp(appInfo)
                                    selectFor == "swipe_right" -> prefsRepo.setSwipeRightApp(appInfo)
                                }
                            }
                            onAppSelected(app, selectFor)
                        } else {
                            appListRepo.launchApp(app.packageName, app.className)
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: InstalledApp,
    onClick: () -> Unit
) {
    Text(
        text = app.label,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    )
}
