
package com.zenithsky.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenithsky.app.data.model.AirlineDto
import com.zenithsky.app.data.model.AirportDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.launch

class AirlineFormActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val airlineId = intent.getIntExtra("airline_id", -1)

        setContent {
            ZenithSkyTheme {
                AirlineFormScreen(
                    airlineId = airlineId,
                    onFinish = { finish() }
                )
            }
        }
    }
}

@Composable
fun AirlineFormScreen(
    airlineId: Int,
    onFinish: () -> Unit
) {
    var iata by remember { mutableStateOf("") }
    var airlineName by remember { mutableStateOf("") }

    var airports by remember { mutableStateOf<List<AirportDto>>(emptyList()) }
    var selectedAirport by remember { mutableStateOf<AirportDto?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val editing = airlineId != -1

    LaunchedEffect(airlineId) {
        try {
            val airportResponse = ApiClient.airportService.getAirports()

            if (airportResponse.isSuccessful) {
                airports = airportResponse.body() ?: emptyList()
            } else {
                error = "No se pudieron cargar los aeropuertos"
            }

            if (editing) {
                val response = ApiClient.airlineService.getAirlineById(airlineId)

                if (response.isSuccessful) {
                    val airline = response.body()

                    if (airline != null) {
                        iata = airline.iata
                        airlineName = airline.airlinename
                        selectedAirport = airports.find {
                            it.airport_id == airline.base_airport
                        }
                    }
                } else {
                    error = "No se pudo consultar la aerolínea"
                }
            }
        } catch (e: Exception) {
            error = "Error de conexión: ${e.message}"
        }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (editing) "Editar aerolínea" else "Registrar aerolínea",
            style = MaterialTheme.typography.headlineMedium
        )

        if (loading) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(
                value = iata,
                onValueChange = { iata = it.uppercase() },
                label = { Text("Código IATA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = airlineName,
                onValueChange = { airlineName = it },
                label = { Text("Nombre de la aerolínea") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Aeropuerto base")

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedAirport?.let {
                            "${it.iata} - ${it.name}"
                        } ?: "Seleccionar aeropuerto"
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    airports.forEach { airport ->
                        DropdownMenuItem(
                            text = {
                                Text("${airport.iata} - ${airport.name}")
                            },
                            onClick = {
                                selectedAirport = airport
                                expanded = false
                            }
                        )
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (
                        iata.isBlank() ||
                        airlineName.isBlank() ||
                        selectedAirport == null
                    ) {
                        error = "Completa todos los campos"
                    } else {
                        scope.launch {
                            saving = true
                            error = null

                            try {
                                val airline = AirlineDto(
                                    airline_id = if (editing) airlineId else 0,
                                    iata = iata.trim(),
                                    airlinename = airlineName.trim(),
                                    base_airport = selectedAirport!!.airport_id,
                                    base_airport_iata = selectedAirport!!.iata,
                                    base_airport_name = selectedAirport!!.name
                                )

                                val response = if (editing) {
                                    ApiClient.airlineService.updateAirline(
                                        airlineId,
                                        airline
                                    )
                                } else {
                                    ApiClient.airlineService.createAirline(airline)
                                }

                                if (response.isSuccessful) {
                                    onFinish()
                                } else {
                                    error = "No se pudo guardar. Código: ${response.code()}"
                                }
                            } catch (e: Exception) {
                                error = "Error de conexión: ${e.message}"
                            }

                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text(
                    if (saving) "Guardando..."
                    else if (editing) "Actualizar" else "Registrar"
                )
            }

            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
