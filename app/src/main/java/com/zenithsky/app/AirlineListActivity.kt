
package com.zenithsky.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zenithsky.app.data.model.AirlineDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.launch

class AirlineListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                AirlineListScreen(
                    onCreateClick = {
                        startActivity(Intent(this, AirlineFormActivity::class.java))
                    },
                    onEditClick = { id ->
                        startActivity(
                            Intent(this, AirlineFormActivity::class.java)
                                .putExtra("airline_id", id)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AirlineListScreen(
    onCreateClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    var airlines by remember { mutableStateOf<List<AirlineDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var airlineToDelete by remember { mutableStateOf<AirlineDto?>(null) }

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun loadAirlines() {
        scope.launch {
            loading = true
            error = null

            try {
                val response = ApiClient.airlineService.getAirlines()

                if (response.isSuccessful) {
                    airlines = response.body() ?: emptyList()
                } else {
                    error = "Error HTTP: ${response.code()}\n${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                error = "Error: ${e.javaClass.name}\nDetalle: ${e.message}\nCausa: ${e.cause}"
            }

            loading = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadAirlines()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Aerolíneas",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text("Registrar aerolínea")
        }

        when {
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            error != null -> {
                Text(error ?: "Error desconocido")
                Button(onClick = { loadAirlines() }) {
                    Text("Reintentar")
                }
            }

            airlines.isEmpty() -> {
                Text("No hay aerolíneas registradas")
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(airlines, key = { it.airline_id }) { airline ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = airline.airlinename,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text("Código IATA: ${airline.iata}")
                                Text(
                                    "Aeropuerto base: ${airline.base_airport_name ?: "No disponible"}"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            onEditClick(airline.airline_id)
                                        }
                                    ) {
                                        Text("Editar")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            airlineToDelete = airline
                                        }
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

    airlineToDelete?.let { airline ->
        AlertDialog(
            onDismissRequest = {
                airlineToDelete = null
            },
            title = {
                Text("Eliminar aerolínea")
            },
            text = {
                Text("¿Deseas eliminar ${airline.airlinename}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val response = ApiClient.airlineService
                                    .deleteAirline(airline.airline_id)

                                if (response.isSuccessful) {
                                    airlineToDelete = null
                                    loadAirlines()
                                } else {
                                    error = "No se pudo eliminar la aerolínea: HTTP ${response.code()}"
                                    airlineToDelete = null
                                }
                            } catch (e: Exception) {
                                error = "Error: ${e.javaClass.name}\nDetalle: ${e.message}\nCausa: ${e.cause}"
                                airlineToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        airlineToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}