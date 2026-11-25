package com.zenithsky.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenithsky.app.data.model.AirlineDto
import com.zenithsky.app.data.model.AirplaneDto
import com.zenithsky.app.data.model.AirplaneTypeDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Response
import java.io.IOException

class AirplaneListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                AirplaneListScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirplaneListScreen() {

    var airplanes by remember { mutableStateOf<List<AirplaneDto>>(emptyList()) }
    var airplaneTypes by remember { mutableStateOf<List<AirplaneTypeDto>>(emptyList()) }
    var airlines by remember { mutableStateOf<List<AirlineDto>>(emptyList()) }

    var selectedTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedAirlineId by rememberSaveable { mutableStateOf<Int?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var filtersErrorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            coroutineScope {
                val typesRequest = async { ApiClient.airplaneTypeService.getAirplaneTypes() }
                val airlinesRequest = async { ApiClient.airlineService.getAirlines() }

                val typesResponse = typesRequest.await()
                val airlinesResponse = airlinesRequest.await()

                if (typesResponse.isSuccessful) {
                    airplaneTypes = typesResponse.body().orEmpty()
                }
                if (airlinesResponse.isSuccessful) {
                    airlines = airlinesResponse.body().orEmpty()
                }
                if (!typesResponse.isSuccessful || !airlinesResponse.isSuccessful) {
                    filtersErrorMessage = "No se pudieron cargar todas las opciones de filtrado."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            filtersErrorMessage = "No se pudieron cargar las opciones de filtrado."
        }
    }

    LaunchedEffect(selectedTypeId, selectedAirlineId, reloadKey) {
        isLoading = true
        errorMessage = null
        try {
            val response = ApiClient.airplaneService.getAirplanes(
                typeId = selectedTypeId,
                airlineId = selectedAirlineId
            )
            if (response.isSuccessful) {
                airplanes = response.body().orEmpty()
            } else {
                airplanes = emptyList()
                errorMessage = serverErrorMessage(response)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            airplanes = emptyList()
            errorMessage = "No se pudo conectar con el servidor. Verifique su conexión e intente de nuevo."
        } catch (e: Exception) {
            airplanes = emptyList()
            errorMessage = "Ocurrió un error inesperado al cargar los aviones."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aviones") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AirplaneFilterDropdown(
                    label = "Tipo de avión",
                    options = airplaneTypes.map { type ->
                        type.type_id to listOfNotNull(type.identifier, type.description)
                            .joinToString(" - ")
                    },
                    selectedId = selectedTypeId,
                    onSelected = { selectedTypeId = it },
                    modifier = Modifier.weight(1f)
                )

                AirplaneFilterDropdown(
                    label = "Aerolínea",
                    options = airlines.map { airline ->
                        airline.airline_id to "${airline.airlinename} (${airline.iata})"
                    },
                    selectedId = selectedAirlineId,
                    onSelected = { selectedAirlineId = it },
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedTypeId != null || selectedAirlineId != null) {
                TextButton(
                    onClick = {
                        selectedTypeId = null
                        selectedAirlineId = null
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Limpiar filtros")
                }
            }

            filtersErrorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    errorMessage != null -> {
                        AirplaneMessageState(
                            message = errorMessage.orEmpty(),
                            isError = true,
                            onRetry = { reloadKey++ },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    airplanes.isEmpty() -> {
                        AirplaneMessageState(
                            message = if (selectedTypeId != null || selectedAirlineId != null) {
                                "No se encontraron aviones con los filtros seleccionados."
                            } else {
                                "No hay aviones registrados."
                            },
                            isError = false,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(airplanes, key = { it.airplane_id }) { airplane ->
                                AirplaneItem(airplane = airplane)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AirplaneFilterDropdown(
    label: String,
    options: List<Pair<Int, String>>,
    selectedId: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {

    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selectedId }?.second ?: "Todos"

    Column(modifier = modifier) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )

                options.forEach { (id, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelected(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AirplaneItem(airplane: AirplaneDto) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Avión #${airplane.airplane_id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text("Capacidad: ${airplane.capacity} pasajeros")
            Text("Tipo: ${airplane.identifier ?: "No disponible"}")
            Text("Descripción: ${airplane.description ?: "No disponible"}")
            Text("Aerolínea: ${airplane.airlinename ?: "No disponible"}")
        }
    }
}

@Composable
private fun AirplaneMessageState(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        if (onRetry != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Reintentar")
            }
        }
    }
}

private fun serverErrorMessage(response: Response<*>): String {
    return when (response.code()) {
        400 -> "Los filtros enviados no son válidos."
        404 -> "No se encontraron aviones."
        in 500..599 -> "Error interno del servidor. Intente más tarde."
        else -> "Error al cargar los aviones (código ${response.code()})."
    }
}
