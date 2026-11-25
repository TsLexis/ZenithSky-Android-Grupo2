package com.zenithsky.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlin.jvm.java

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                MainMenu(
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
                                AirlineActivity::class.java
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
                    },
                    onAirplaneTypesClick = {
                        startActivity(
                            Intent(
                                this,
                                AirplaneTypeActivity::class.java
                            )
                        )
                    }
                )
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

        Text(
            text = "ZenithSky"
        )

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