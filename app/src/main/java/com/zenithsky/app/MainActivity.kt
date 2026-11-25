package com.zenithsky.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenithsky.app.data.model.AirlineDto
import com.zenithsky.app.data.model.AirplaneDto
import com.zenithsky.app.data.model.AirplaneTypeDto
import com.zenithsky.app.data.model.AirportDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                ZenithSkyTestScreen()
            }
        }
    }
}

@Composable
fun ZenithSkyTestScreen() {

    var airports by remember {
        mutableStateOf<List<AirportDto>>(emptyList())
    }

    var airlines by remember {
        mutableStateOf<List<AirlineDto>>(emptyList())
    }

    var airplanes by remember {
        mutableStateOf<List<AirplaneDto>>(emptyList())
    }

    var airplaneTypes by remember {
        mutableStateOf<List<AirplaneTypeDto>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        try {

            // Obtener aeropuertos
            val airportResponse =
                ApiClient.airportService.getAirports()

            if (airportResponse.isSuccessful) {
                airports = airportResponse.body() ?: emptyList()
            } else {
                error = "Error al obtener aeropuertos: ${airportResponse.code()}"
            }

            // Obtener aerolíneas
            val airlineResponse =
                ApiClient.airlineService.getAirlines()

            if (airlineResponse.isSuccessful) {
                airlines = airlineResponse.body() ?: emptyList()
            } else {
                error = "Error al obtener aerolíneas: ${airlineResponse.code()}"
            }

            // Obtener aviones
            val airplaneResponse =
                ApiClient.airplaneService.getAirplanes()

            if (airplaneResponse.isSuccessful) {
                airplanes = airplaneResponse.body() ?: emptyList()
            } else {
                error = "Error al obtener aviones: ${airplaneResponse.code()}"
            }

            // Obtener tipos de avión
            val airplaneTypeResponse =
                ApiClient.airplaneTypeService.getAirplaneTypes()

            if (airplaneTypeResponse.isSuccessful) {
                airplaneTypes =
                    airplaneTypeResponse.body() ?: emptyList()
            } else {
                error =
                    "Error al obtener tipos de avión: ${airplaneTypeResponse.code()}"
            }

        } catch (e: Exception) {

            error = "Error de conexión: ${e.message}"

        } finally {

            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        when {

            loading -> {

                CircularProgressIndicator()
            }

            error != null -> {

                Text(
                    text = error ?: "Error desconocido"
                )
            }

            else -> {

                // =========================
                // AEROPUERTOS
                // =========================

                Text(
                    text = "Aeropuertos encontrados:"
                )

                airports.forEach { airport ->

                    Text(
                        text = "${airport.iata} - ${airport.name}"
                    )
                }

                // =========================
                // AEROLÍNEAS
                // =========================

                Text(
                    text = "\nAerolíneas encontradas:"
                )

                airlines.forEach { airline ->

                    Text(
                        text = "${airline.iata} - ${airline.airlinename}"
                    )
                }

                // =========================
                // AVIONES
                // =========================

                Text(
                    text = "\nAviones encontrados:"
                )

                airplanes.forEach { airplane ->

                    Text(
                        text = "ID: ${airplane.airplane_id} - Capacidad: ${airplane.capacity}"
                    )
                }

                // =========================
                // TIPOS DE AVIÓN
                // =========================

                Text(
                    text = "\nTipos de avión encontrados:"
                )

                airplaneTypes.forEach { type ->

                    Text(
                        text = "${type.identifier} - ${type.description}"
                    )
                }
            }
        }
    }
}