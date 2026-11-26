package com.zenithsky.app

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

internal enum class ZenithDestination(
    val label: String,
    val activityClass: Class<out Activity>,
    val icon: ImageVector
) {
    HOME(
        "Inicio",
        MainActivity::class.java,
        drawerIcon(
            "Inicio",
            "M10 20v-6h4v6h5v-9h3L12 2 2 11h3v9z"
        )
    ),
    AIRPORTS(
        "Aeropuertos",
        AirportListActivity::class.java,
        drawerIcon(
            "Aeropuertos",
            "M3 19h18v2H3zM4 14l-2-4 1.5-.4L6 12l4-.9L7 5l2-.5 5 5.7 5.2-1.3a1.5 1.5 0 0 1 .7 2.9L6 15z"
        )
    ),
    AIRLINES(
        "Aerolíneas",
        AirlineListActivity::class.java,
        drawerIcon(
            "Aerolíneas",
            "M20 6h-4V4H8v2H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zM10 6V5h4v1zM6 8h2v11H6zM16 8h2v11h-2z"
        )
    ),
    AIRPLANES(
        "Aviones",
        AirplaneListActivity::class.java,
        drawerIcon(
            "Aviones",
            "M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z"
        )
    )
}

private val menuIcon = drawerIcon("Menú", "M3 6h18v2H3zM3 11h18v2H3zM3 16h18v2H3z")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ZenithNavigationScaffold(
    currentDestination: ZenithDestination,
    title: @Composable () -> Unit,
    hasUnsavedChanges: Boolean = false,
    navigationLocked: Boolean = false,
    onPermissionNeeded: (() -> Unit)? = null,
    onFormExit: (() -> Unit)? = null,
    content: @Composable (PaddingValues, requestFormExit: () -> Unit) -> Unit
) {
    val activity = LocalContext.current as Activity
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pendingDestination by rememberSaveable { mutableStateOf<ZenithDestination?>(null) }

    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    fun requestFormExit() {
        if (navigationLocked || onFormExit == null) return
        if (hasUnsavedChanges) {
            pendingDestination = null
            showDiscardDialog = true
        } else {
            onFormExit()
        }
    }

    BackHandler(enabled = drawerState.isOpen || navigationLocked || onFormExit != null) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            requestFormExit()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen && !navigationLocked,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ZenithSky",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                )
                ZenithDestination.entries.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        selected = destination == currentDestination,
                        onClick = {
                            if (navigationLocked) return@NavigationDrawerItem
                            scope.launch {
                                drawerState.close()
                                if (activity.javaClass == destination.activityClass) return@launch
                                if (onPermissionNeeded != null) {
                                    onPermissionNeeded()
                                } else if (hasUnsavedChanges) {
                                    pendingDestination = destination
                                    showDiscardDialog = true
                                } else {
                                    activity.navigateToZenithDestination(destination)
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = title,
                    navigationIcon = {
                        IconButton(
                            enabled = !navigationLocked,
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Icon(menuIcon, contentDescription = "Abrir menú de navegación")
                        }
                    }
                )
            },
            content = { padding -> content(padding, ::requestFormExit) }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("¿Descartar los cambios?") },
            text = { Text("Si sales del formulario, se perderán los cambios sin guardar.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        val destination = pendingDestination
                        if (destination == null) onFormExit?.invoke()
                        else activity.navigateToZenithDestination(destination)
                        pendingDestination = null
                    }
                ) { Text("Descartar cambios") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir editando") }
            }
        )
    }
}

internal fun Activity.navigateToZenithDestination(destination: ZenithDestination) {
    if (javaClass == destination.activityClass) return

    startActivity(
        Intent(this, destination.activityClass)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )
    if (this !is MainActivity) finish()
}

private fun drawerIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()
