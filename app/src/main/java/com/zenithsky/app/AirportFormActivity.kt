
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
import com.zenithsky.app.data.model.AirportDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.launch

class AirportFormActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val airportId = intent.getIntExtra("airport_id", -1)

        setContent {
            ZenithSkyTheme {
                AirportFormScreen(
                    airportId = airportId,
                    onFinish = { finish() }
                )
            }
        }
    }
}

@Composable
fun AirportFormScreen(
    airportId: Int,
    onFinish: () -> Unit
) {
    var iata by remember { mutableStateOf("") }
    var icao by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val editing = airportId != -1

    LaunchedEffect(airportId) {
        if (editing) {
            try {
                val response =
                    ApiClient.airportService.getAirportById(airportId)

                if (response.isSuccessful) {
                    val airport = response.body()

                    if (airport != null) {
                        iata = airport.iata
                        icao = airport.icao
                        name = airport.name
                    }
                } else {
                    error = "No se pudo consultar el aeropuerto"
                }
            } catch (e: Exception) {
                error = "Error de conexión: ${e.message}"
            }
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
            text = if (editing) "Editar aeropuerto"
            else "Registrar aeropuerto",
            style = MaterialTheme.typography.headlineMedium
        )

        if (loading) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(
                value = iata,
                onValueChange = {
                    iata = it.uppercase().take(3)
                },
                label = { Text("Código IATA") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = icao,
                onValueChange = {
                    icao = it.uppercase().take(4)
                },
                label = { Text("Código ICAO") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del aeropuerto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    when {
                        iata.length != 3 ->
                            error = "El código IATA debe tener 3 caracteres"

                        icao.length != 4 ->
                            error = "El código ICAO debe tener 4 caracteres"

                        name.isBlank() ->
                            error = "Ingresa el nombre del aeropuerto"

                        else -> {
                            scope.launch {
                                saving = true
                                error = null

                                try {
                                    val airport = AirportDto(
                                        airport_id = if (editing) airportId else 0,
                                        iata = iata.trim(),
                                        icao = icao.trim(),
                                        name = name.trim()
                                    )

                                    val response = if (editing) {
                                        ApiClient.airportService.updateAirport(
                                            airportId,
                                            airport
                                        )
                                    } else {
                                        ApiClient.airportService.createAirport(
                                            airport
                                        )
                                    }

                                    if (response.isSuccessful) {
                                        onFinish()
                                    } else {
                                        error = when (response.code()) {
                                            409 -> "No se pudo guardar: existe un conflicto con los datos"
                                            else -> "No se pudo guardar. Código: ${response.code()}"
                                        }
                                    }
                                } catch (e: Exception) {
                                    error = "Error de conexión: ${e.message}"
                                }

                                saving = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            ) {
                Text(
                    if (saving) "Guardando..."
                    else if (editing) "Actualizar"
                    else "Registrar"
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
