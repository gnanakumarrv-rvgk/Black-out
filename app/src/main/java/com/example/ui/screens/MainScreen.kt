package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import android.graphics.Color as AndroidColor
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BlackoutHistory
import com.example.data.Bookmark
import com.example.ui.theme.*
import com.example.viewmodel.BlackoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BlackoutViewModel) {
    val context = LocalContext.current
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val isBlackedOut by viewModel.isBlackedOut.collectAsStateWithLifecycle()
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check overlay permission helper
    val checkOverlayPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        if (!checkOverlayPermission()) {
            showPermissionDialog = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Hearing,
                            contentDescription = "App logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Blackout Audio",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { /* menu */ },
                        modifier = Modifier.testTag("menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!checkOverlayPermission()) {
                                showPermissionDialog = true
                            } else {
                                viewModel.toggleService()
                            }
                        },
                        modifier = Modifier.testTag("power_action_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = "Active Toggle",
                            tint = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Overview"
                        )
                    },
                    label = { Text("Overview", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Language else Icons.Outlined.Language,
                            contentDescription = "Browser"
                        )
                    },
                    label = { Text("Web Player", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "Savings Logs"
                        )
                    },
                    label = { Text("Logs & Stats", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                OverviewTab(viewModel) {
                    if (!checkOverlayPermission()) {
                        showPermissionDialog = true
                    } else {
                        viewModel.toggleService()
                    }
                }
            }
            
            // Continuous persistent browser layout to preserve WebView and background playback state during tab swaps
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = if (selectedTab == 1) 0.dp else 2000.dp)
            ) {
                BrowserTab(viewModel)
            }
            
            if (selectedTab == 2) {
                LogsTab(viewModel)
            }
        }
    }

    // Modal dialog to grant SYSTEM_ALERT_WINDOW permission
    if (showPermissionDialog) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            showPermissionDialog = !checkOverlayPermission()
        }

        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(
                    "Overlay Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Blackout Audio needs power overlay privileges to make the screen completely black over streaming video (like YouTube) without locking or turning off the system.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            launcher.launch(intent)
                        } else {
                            showPermissionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Grant Permission", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Not Now", color = MaterialTheme.colorScheme.outline)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun OverviewTab(
    viewModel: BlackoutViewModel,
    onTogglePower: () -> Unit
) {
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val isBlackedOut by viewModel.isBlackedOut.collectAsStateWithLifecycle()
    val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()
    val useProximity by viewModel.useProximity.collectAsStateWithLifecycle()
    val useAccel by viewModel.useAccel.collectAsStateWithLifecycle()
    val accelThreshold by viewModel.accelThreshold.collectAsStateWithLifecycle()
    val showFloatingShortcut by viewModel.showFloatingShortcut.collectAsStateWithLifecycle()
    val floatingShortcutOpacity by viewModel.floatingShortcutOpacity.collectAsStateWithLifecycle()
    val useTransparentLock by viewModel.useTransparentLock.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val customThemeColor by viewModel.customThemeColor.collectAsStateWithLifecycle()

    val scale by animateFloatAsState(
        targetValue = if (isMonitoring) 1.05f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "PowerButtonScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("overview_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMonitoring) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .alpha(scale)
                            .clip(CircleShape)
                            .background(
                                if (isMonitoring) MaterialTheme.colorScheme.primary else ElegantCardLighter
                            )
                            .clickable { onTogglePower() }
                            .testTag("power_toggle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = "Switch ON/OFF",
                            modifier = Modifier.size(50.dp),
                            tint = if (isMonitoring) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isMonitoring) "Service Active" else "Ready to Monitor",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Text(
                        text = if (isMonitoring) "Monitoring device physical orientation..." else "Turn service on to safeguard playback",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Live Sensor Monitor Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElegantCardLighter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sensor Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isMonitoring) "Live" else "Standby",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = if (isMonitoring) 0.75f else 0f,
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = if (isMonitoring) sensorState else "Sensors Off",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Turn screen down / cover light to instantly protect background playback of YouTube, Twitch, etc. Blackout wakes instantly when picked up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElegantTextSubtle
                    )
                }
            }
        }

        // Quick Controls Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElegantCardLighter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Active Handshake Sensors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Proximity Trigger", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Covering top of screen directly blacks out", fontSize = 12.sp, color = ElegantTextSubtle)
                        }
                        Switch(
                            checked = useProximity,
                            onCheckedChange = { viewModel.updateUseProximity(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Divider(color = ElegantCardLighter, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gravity/Orientation Trigger", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Flipping device flat face-down blacks out", fontSize = 12.sp, color = ElegantTextSubtle)
                        }
                        Switch(
                            checked = useAccel,
                            onCheckedChange = { viewModel.updateUseAccel(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = useAccel,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            HorizontalDivider(color = ElegantCardLighter, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Face-Down Sensitivity Threshold",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when {
                                        accelThreshold <= -9.0f -> "Strict (${String.format(Locale.US, "%.1f", accelThreshold)})"
                                        accelThreshold <= -7.5f -> "Firm (${String.format(Locale.US, "%.1f", accelThreshold)})"
                                        accelThreshold <= -6.5f -> "Balanced (${String.format(Locale.US, "%.1f", accelThreshold)})"
                                        else -> "Sensitive (${String.format(Locale.US, "%.1f", accelThreshold)})"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Slider(
                                value = accelThreshold,
                                onValueChange = { viewModel.updateAccelThreshold(it) },
                                valueRange = -9.5f..-5.0f,
                                steps = 8, // Gives discrete increments of 0.5: -9.5, -9.0, -8.5, -8.0, -7.5, -7.0, -6.5, -6.0, -5.5, -5.0
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("accel_threshold_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Strict (No accidental trigs)", fontSize = 11.sp, color = ElegantTextSubtle)
                                Text("Sensitive (Easy flip)", fontSize = 11.sp, color = ElegantTextSubtle)
                            }
                        }
                    }
                }
            }
        }

        // Floating Toggle Shortcut Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElegantCardLighter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Floating Toggle Shortcut",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Floating Blackout Bubble", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Shows a draggable overlay bubble while monitoring is active. Tap to toggle blackout instantly.", fontSize = 12.sp, color = ElegantTextSubtle)
                        }
                        Switch(
                            checked = showFloatingShortcut,
                            onCheckedChange = { viewModel.updateShowFloatingShortcut(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("floating_shortcut_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = showFloatingShortcut,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            HorizontalDivider(color = ElegantCardLighter, thickness = 1.dp, modifier = Modifier.padding(bottom = 12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shortcut Bubble Opacity",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(floatingShortcutOpacity * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Slider(
                                value = floatingShortcutOpacity,
                                onValueChange = { viewModel.updateFloatingShortcutOpacity(it) },
                                valueRange = 0.10f..0.90f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("floating_shortcut_opacity_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Invisibly Dim", fontSize = 11.sp, color = ElegantTextSubtle)
                                Text("Very Clear", fontSize = 11.sp, color = ElegantTextSubtle)
                            }
                        }
                    }
                }
            }
        }

        // Screen Lock Type Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElegantCardLighter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Protection Overlay Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Transparent Touch Lock (Optimal)", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text("Keeps the video fully bright & visible under a transparent shield that secures all touches and inputs.", fontSize = 12.sp, color = ElegantTextSubtle)
                        }
                        Switch(
                            checked = useTransparentLock,
                            onCheckedChange = { viewModel.updateUseTransparentLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("transparent_lock_switch")
                        )
                    }
                }
            }
        }

        // App Tint & Dark/Light Theme Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElegantCardLighter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "App Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Theme selector (Dark vs Light buttons)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Theme Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dark Theme button
                            Button(
                                onClick = { viewModel.updateThemeMode(0) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (themeMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (themeMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌙 Dark Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Light Theme button
                            Button(
                                onClick = { viewModel.updateThemeMode(1) },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (themeMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (themeMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("☀️ Light Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = ElegantCardLighter.copy(alpha = 0.5f))

                    // Interface Tint Selector
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Interface Color Accent",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Elegant color presets
                        val presets = listOf(
                            0xFFD0BCFF, // Indigo / Default Purple
                            0xFF00C853, // Emerald Green
                            0xFF00E5FF, // Cyan Surge
                            0xFFFFA000, // Golden Amber
                            0xFFFF4081, // Neon Pink
                            0xFFFF3D00, // Vibrant Crimson
                            0xFF636AFF, // Electric Blue
                            0xFFECEFF1  // Slate Silver
                        )
                        
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(presets) { presetArgb ->
                                val rgb = Color(presetArgb)
                                val isSelected = (customThemeColor == presetArgb.toInt())
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(rgb)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateCustomThemeColor(presetArgb.toInt())
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = if (presetArgb == 0xFFECEFF1) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Custom Color Wheel text and widget
                        Text(
                            text = "Interactive Color Wheel (Fine Tune)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = ElegantTextSubtle
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ColorWheel(
                                selectedColor = Color(customThemeColor),
                                onColorSelected = { color ->
                                    viewModel.updateCustomThemeColor(color.toArgb())
                                },
                                modifier = Modifier.size(170.dp)
                            )
                        }

                        // Selected Color Value Display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Selected Accent Hex:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("#%08X", customThemeColor),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Direct Force Blackout Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElegantCardLighter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Instant Screen Protection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Force blackout now without flip/sensor. Best for listening while leaving device in back pocket, or during long commutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElegantTextSubtle,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.triggerManualBlackout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("force_blackout_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = "Manual Blackout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Blackout Now", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserTab(viewModel: BlackoutViewModel) {
    val context = LocalContext.current
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf(currentUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoadingWeb by remember { mutableStateOf(false) }

    var showAddBookmark by remember { mutableStateOf(false) }
    var newBookmarkName by remember { mutableStateOf("") }
    var newBookmarkUrl by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("browser_tab")
    ) {
        // Top control bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Navigation Back
            IconButton(
                onClick = { webViewInstance?.goBack() },
                enabled = webViewInstance?.canGoBack() == true,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Web Back",
                    tint = if (webViewInstance?.canGoBack() == true) MaterialTheme.colorScheme.onBackground else ElegantCardLighter
                )
            }

            // Url Input field
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("address_bar"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    var target = urlInput.trim()
                    if (target.isNotEmpty()) {
                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                            target = "https://www.google.com/search?q=$target"
                        }
                        viewModel.updateBrowserUrl(target)
                        webViewInstance?.loadUrl(target)
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(),
                placeholder = { Text("Search or web address", color = ElegantTextSubtle, fontSize = 13.sp) }
            )

            // Direct Search action
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    var target = urlInput.trim()
                    if (target.isNotEmpty()) {
                        if (!target.startsWith("http://") && !target.startsWith("https://")) {
                            target = "https://www.google.com/search?q=$target"
                        }
                        viewModel.updateBrowserUrl(target)
                        webViewInstance?.loadUrl(target)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Go URL", modifier = Modifier.size(20.dp))
            }
        }

        // Bookmark row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(bookmarks) { bookmark ->
                        InputChip(
                            selected = false,
                            onClick = {
                                viewModel.updateBrowserUrl(bookmark.url)
                                urlInput = bookmark.url
                                webViewInstance?.loadUrl(bookmark.url)
                            },
                            label = { Text(bookmark.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (bookmark.iconType == "music") Icons.Filled.MusicNote else Icons.Filled.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete bookmark",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.deleteBookmark(bookmark) },
                                    tint = ElegantTextSubtle
                                )
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, ElegantCardLighter)
                        )
                    }
                }

                IconButton(
                    onClick = { showAddBookmark = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = "Add bookmark", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Live Loader indicator
        if (isLoadingWeb) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }

        // Browser WebView Holder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    val webView = WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowContentAccess = true
                        
                        // Guarantee safe background play by injecting clean mobile user agent
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoadingWeb = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoadingWeb = false
                                url?.let {
                                    urlInput = it
                                    viewModel.updateBrowserUrl(it)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                isLoadingWeb = newProgress < 100
                            }
                        }
                    }
                    webViewInstance = webView
                    webView.loadUrl(currentUrl)
                    webView
                },
                update = { webView ->
                    // Do nothing here to prevent infinite viewport reload
                },
                modifier = Modifier.fillMaxSize()
            )

            // Direct Force Overlay Floating Button on top of the video browser
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.triggerManualBlackout() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("floating_blackout_action")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = "Protect Playback Now"
                    )
                }
            }
        }
    }

    // Modal dialog to save bookmarks
    if (showAddBookmark) {
        AlertDialog(
            onDismissRequest = { showAddBookmark = false },
            title = { Text("Save Favorite Platform", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newBookmarkName,
                        onValueChange = { newBookmarkName = it },
                        label = { Text("Label (e.g. DailyMotion)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = ElegantCardLighter,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    OutlinedTextField(
                        value = newBookmarkUrl,
                        onValueChange = { newBookmarkUrl = it },
                        label = { Text("URL (e.g. www.vimeo.com)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = ElegantCardLighter,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBookmarkName.trim().isNotEmpty() && newBookmarkUrl.trim().isNotEmpty()) {
                            viewModel.addBookmark(newBookmarkName, newBookmarkUrl)
                            newBookmarkName = ""
                            newBookmarkUrl = ""
                            showAddBookmark = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Favorite", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmark = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun LogsTab(viewModel: BlackoutViewModel) {
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    val totalBlackoutSeconds = historyList.sumOf { it.durationSeconds }
    val totalEnergySavedEstWh = historyList.sumOf { it.energySavedEstWh }

    val totalHours = totalBlackoutSeconds / 3600
    val totalMinutes = (totalBlackoutSeconds % 3600) / 60
    val totalSecondsText = if (totalBlackoutSeconds < 60) "${totalBlackoutSeconds}s" else "${totalHours}h ${totalMinutes}m"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("logs_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cumulative Metrics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Power Preservation Statement",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Clean Energy saved",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f Wh", totalEnergySavedEstWh),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "OLED Draw Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElegantTextSubtle
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(56.dp)
                                .width(1.dp),
                            color = ElegantCardLighter
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Saved play hours",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = totalSecondsText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Protected Playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElegantTextSubtle
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Stats calculated from OLED screen draw savings (approx 1.8W preserved by drawing full-screen dark pixels).",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElegantTextSubtle,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Action header log list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historical Protection Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear Logs", color = Color(0xFFF44336), fontSize = 13.sp)
                    }
                }
            }
        }

        if (historyList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CloudQueue,
                            contentDescription = "Empty",
                            tint = ElegantCardLighter,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No protection sessions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElegantTextSubtle
                        )
                    }
                }
            }
        } else {
            items(historyList) { historyItem ->
                HistoryItemRow(historyItem)
            }
        }
    }
}

@Composable
fun HistoryItemRow(history: BlackoutHistory) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val dateText = remember(history.timestamp) { formatter.format(Date(history.timestamp)) }

    val durationText = if (history.durationSeconds < 60) {
        "${history.durationSeconds}s"
    } else {
        "${history.durationSeconds / 60}m ${history.durationSeconds % 60}s"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ElegantCardLighter)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Triggered via ${history.triggerType}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElegantTextSubtle
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(Locale.getDefault(), "+%.3f Wh", history.energySavedEstWh),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ColorWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }

    val hsv = remember(selectedColor) {
        val outHsv = FloatArray(3)
        AndroidColor.colorToHSV(selectedColor.toArgb(), outHsv)
        outHsv
    }
    val currentHue = hsv[0]
    val currentSat = hsv[1]

    val indicatorOffset = remember(currentHue, currentSat, center, radius) {
        if (radius == 0f) Offset.Zero else {
            val angleInRad = currentHue * Math.PI / 180.0
            val r = currentSat * radius
            Offset(
                (center.x + r * Math.cos(angleInRad)).toFloat(),
                (center.y + r * Math.sin(angleInRad)).toFloat()
            )
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                fun updateColor(position: Offset) {
                    val dx = position.x - center.x
                    val dy = position.y - center.y
                    val r = sqrt(dx * dx + dy * dy)
                    if (radius > 0f) {
                        val angleInRad = atan2(dy, dx)
                        var hue = (angleInRad * 180.0 / Math.PI).toFloat()
                        if (hue < 0) hue += 360f
                        val sat = (r / radius).coerceIn(0f, 1f)
                        val rgbInt = AndroidColor.HSVToColor(floatArrayOf(hue, sat, 1f))
                        onColorSelected(Color(rgbInt))
                    }
                }

                detectTapGestures(
                    onPress = { offset ->
                        updateColor(offset)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y
                    val r = sqrt(dx * dx + dy * dy)
                    if (radius > 0f) {
                        val angleInRad = atan2(dy, dx)
                        var hue = (angleInRad * 180.0 / Math.PI).toFloat()
                        if (hue < 0) hue += 360f
                        val sat = (r / radius).coerceIn(0f, 1f)
                        val rgbInt = AndroidColor.HSVToColor(floatArrayOf(hue, sat, 1f))
                        onColorSelected(Color(rgbInt))
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                    val clSize = layoutCoordinates.size
                    center = Offset(clSize.width / 2f, clSize.height / 2f)
                    radius = (clSize.width / 2f) - 16f
                }
        ) {
            val drawRadius = size.width / 2f - 8f
            
            val sweepColors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            drawCircle(
                brush = Brush.sweepGradient(sweepColors, center),
                radius = drawRadius,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = drawRadius
                ),
                radius = drawRadius,
                center = center
            )

            if (indicatorOffset != Offset.Zero) {
                drawCircle(
                    color = Color.Black,
                    radius = 12f,
                    center = indicatorOffset,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = indicatorOffset,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = selectedColor,
                    radius = 6f,
                    center = indicatorOffset
                )
            }
        }
    }
}
