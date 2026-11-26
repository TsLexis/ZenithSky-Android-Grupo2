
package com.zenithsky.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zenithsky.app.data.local.database.ZenithSkyDatabase
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {

                var hasPermission = remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < 37 ||
                                checkSelfPermission(
                                    "android.permission.ACCESS_LOCAL_NETWORK"
                                ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasPermission.value = granted
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 37 && !hasPermission.value) {
                        permissionLauncher.launch(
                            "android.permission.ACCESS_LOCAL_NETWORK"
                        )
                    }
                }

                if (hasPermission.value) {

                    val context = androidx.compose.ui.platform.LocalContext.current

                    val database = remember {
                        ZenithSkyDatabase.getDatabase(context)
                    }

                    val airlines by database.airlineDao()
                        .obtenerTodas()
                        .collectAsState(initial = emptyList())

                    val airplanes by database.avionDestacadoDao()
                        .obtenerTodos()
                        .collectAsState(initial = emptyList())

                    val operations by database.operacionLogDao()
                        .obtenerTodos()
                        .collectAsState(initial = emptyList())

                    MainMenu(
                        airlineCount = airlines.size,
                        airplaneCount = airplanes.size,
                        operationCount = operations.size,

                        onAirportsClick = {
                            startActivity(
                                Intent(
                                    this,
                                    AirportActivity::class.java
                                )
                            )
                        },

                        onAirlinesClick = {
                            startActivity(
                                Intent(
                                    this,
                                    AirlineListActivity::class.java
                                )
                            )
                        },

                        onAirplanesClick = {
                            startActivity(
                                Intent(
                                    this,
                                    AirplaneListActivity::class.java
                                )
                            )
                        }
                    )

                } else {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(
                            "ZenithSky necesita permiso para conectarse al servidor local."
                        )

                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    "android.permission.ACCESS_LOCAL_NETWORK"
                                )
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Conceder permiso")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu(
    airlineCount: Int,
    airplaneCount: Int,
    operationCount: Int,
    onAirportsClick: () -> Unit,
    onAirlinesClick: () -> Unit,
    onAirplanesClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ZenithSky")
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Button(
                onClick = onAirportsClick,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Text("Aeropuertos")
            }

            Button(
                onClick = onAirlinesClick,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text("Aerolíneas")
            }

            Button(
                onClick = onAirplanesClick,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text("Aviones")
            }

            Text(
                text = "Resumen local",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 30.dp)
            )

            Text(
                text = "Aerolíneas guardadas: $airlineCount",
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Aviones destacados: $airplaneCount",
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Operaciones registradas: $operationCount",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

