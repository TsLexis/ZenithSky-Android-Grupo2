
package com.zenithsky.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenithsky.app.data.model.AirportDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.launch

class AirportListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                AirportListScreen(
                    onCreateClick = {
                        startActivity(
                            Intent(this, AirportFormActivity::class.java)
                        )
                    },
                    onEditClick = { airportId ->
                        val intent = Intent(
                            this,
                            AirportFormActivity::class.java
                        )
                        intent.putExtra("airport_id", airportId)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun AirportListScreen(
    onCreateClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    var airports by remember {
        mutableStateOf<List<AirportDto>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var airportToDelete by remember {
        mutableStateOf<AirportDto?>(null)
    }

    val scope = rememberCoroutineScope()

    suspend fun loadAirports() {
        loading = true
        error = null

        try {
            val response = ApiClient.airportService.getAirports()

            if (response.isSuccessful) {
                airports = response.body() ?: emptyList()
            } else {
                error = "No se pudieron cargar los aeropuertos. Código: ${response.code()}"
            }
        } catch (e: Exception) {
            error = "Error de conexión: ${e.message}"
        }

        loading = false
    }

    LaunchedEffect(Unit) {
        loadAirports()
    }

    ZenithNavigationScaffold(
        currentDestination = ZenithDestination.AIRPORTS,
        title = {
            Text(
                "Aeropuertos",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    ) { innerPadding, _ ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("Registrar aeropuerto")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error
                            )

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        loadAirports()
                                    }
                                },
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }

                    airports.isEmpty() -> {
                        Text(
                            text = "No hay aeropuertos registrados.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                airports,
                                key = { it.airport_id }
                            ) { airport ->

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = airport.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text("Código IATA: ${airport.iata}")
                                        Text("Código ICAO: ${airport.icao}")

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    onEditClick(airport.airport_id)
                                                }
                                            ) {
                                                Text("Editar")
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    airportToDelete = airport
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                border = BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.error.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                            ) {
                                                Text("Eliminar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    airportToDelete?.let { airport ->
        AlertDialog(
            onDismissRequest = {
                airportToDelete = null
            },
            title = {
                Text("Eliminar aeropuerto")
            },
            text = {
                Text("¿Deseas eliminar el aeropuerto ${airport.name}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val response =
                                    ApiClient.airportService.deleteAirport(
                                        airport.airport_id
                                    )

                                if (response.isSuccessful) {
                                    airportToDelete = null
                                    loadAirports()
                                } else {
                                    error = if (response.code() == 409) {
                                        "No se puede eliminar: el aeropuerto está siendo utilizado."
                                    } else {
                                        "No se pudo eliminar. Código: ${response.code()}"
                                    }
                                    airportToDelete = null
                                }
                            } catch (e: Exception) {
                                error = "Error de conexión: ${e.message}"
                                airportToDelete = null
                            }
                        }
                    }
                ) {
                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        airportToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
