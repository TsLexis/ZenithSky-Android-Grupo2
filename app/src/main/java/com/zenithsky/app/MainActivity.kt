
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                var hasPermission = remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < 37 ||
                                checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") ==
                                PackageManager.PERMISSION_GRANTED
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
                    MainMenu(
                        onAirportsClick = {
                            startActivity(Intent(this, AirportActivity::class.java))
                        },
                        onAirlinesClick = {
                            startActivity(Intent(this, AirlineListActivity::class.java))
                        },
                        onAirplanesClick = {
                            startActivity(Intent(this, AirplaneActivity::class.java))
                        },
                        onAirplaneTypesClick = {
                            startActivity(Intent(this, AirplaneTypeActivity::class.java))
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
                        Text("ZenithSky necesita permiso para conectarse al servidor local.")

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

@Composable
fun MainMenu(
    onAirportsClick: () -> Unit,
    onAirlinesClick: () -> Unit,
    onAirplanesClick: () -> Unit,
    onAirplaneTypesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ZenithSky")

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

        Button(
            onClick = onAirplaneTypesClick,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text("Tipos de avión")
        }
    }
}
