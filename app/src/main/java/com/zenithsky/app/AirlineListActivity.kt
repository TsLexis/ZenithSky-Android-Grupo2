package com.zenithsky.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.zenithsky.app.data.local.database.ZenithSkyDatabase
import com.zenithsky.app.data.local.entity.AirlineLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AirlineListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ZenithSkyDatabase.getDatabase(applicationContext)

        setContent {
            ZenithSkyTheme {
                AirlineListScreen(
                    database = database,
                    onBackClick = {
                        finish()
                    },
                    onCreateClick = {
                        startActivity(
                            Intent(this, AirlineFormActivity::class.java)
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirlineListScreen(
    database: ZenithSkyDatabase,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    var airlines by remember {
        mutableStateOf<List<AirlineDto>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var airlineToDelete by remember {
        mutableStateOf<AirlineDto?>(null)
    }

    var deleteError by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    fun loadAirlines() {
        scope.launch {
            loading = true
            error = null

            try {
                val response = ApiClient.airlineService.getAirlines()

                if (response.isSuccessful) {

                    val airlineList = response.body() ?: emptyList()

                    // Actualizar la lista que se muestra en pantalla
                    airlines = airlineList

                    // Convertir DTOs a entidades locales
                    val localAirlines = airlineList.map { airline ->
                        AirlineLocal(
                            id = airline.airline_id,
                            iata = airline.iata,
                            nombre = airline.airlinename,
                            aeropuertoBaseIata =
                                airline.base_airport_iata ?: "",
                            aeropuertoBaseNombre =
                                airline.base_airport_name ?: ""
                        )
                    }

                    // Guardar la copia local en Room
                    withContext(Dispatchers.IO) {
                        database.airlineDao()
                            .reemplazarTodas(localAirlines)
                    }

                } else {
                    error =
                        "Error HTTP: ${response.code()}\n${response.errorBody()?.string()}"
                }

            } catch (e: Exception) {
                error =
                    "Error: ${e.javaClass.name}\n" +
                            "Detalle: ${e.message}\n" +
                            "Causa: ${e.cause}"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Aerolíneas")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("Registrar aerolínea")
            }

            when {

                loading -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.CenterHorizontally
                        )
                    )
                }

                error != null -> {

                    Text(
                        text = error ?: "Error desconocido",
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = {
                            loadAirlines()
                        }
                    ) {
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

                        items(
                            airlines,
                            key = { it.airline_id }
                        ) { airline ->

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                )
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        text = airline.airlinename,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Text(
                                        text = "Código IATA: ${airline.iata}"
                                    )

                                    Text(
                                        text = "Aeropuerto base: ${
                                            airline.base_airport_name
                                                ?: "No disponible"
                                        }"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {

                                        OutlinedButton(
                                            onClick = {
                                                onEditClick(
                                                    airline.airline_id
                                                )
                                            }
                                        ) {
                                            Text("Editar")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                airlineToDelete = airline
                                            },
                                            colors =
                                                ButtonDefaults.outlinedButtonColors(
                                                    contentColor =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .error
                                                ),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme
                                                    .colorScheme
                                                    .error
                                                    .copy(alpha = 0.5f)
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

    airlineToDelete?.let { airline ->

        AlertDialog(
            onDismissRequest = {
                airlineToDelete = null
            },
            title = {
                Text("Eliminar aerolínea")
            },
            text = {
                Text(
                    "¿Deseas eliminar ${airline.airlinename}?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        scope.launch {

                            try {

                                val response =
                                    ApiClient.airlineService
                                        .deleteAirline(
                                            airline.airline_id
                                        )

                                if (response.isSuccessful) {

                                    airlineToDelete = null
                                    deleteError = null

                                    loadAirlines()

                                } else {

                                    deleteError = when (response.code()) {

                                        409 ->
                                            "No se puede eliminar la aerolínea porque tiene aviones asociados."

                                        404 ->
                                            "La aerolínea que intentas eliminar no existe."

                                        else ->
                                            "No se pudo eliminar la aerolínea. Código HTTP: ${response.code()}"
                                    }

                                    airlineToDelete = null
                                }

                            } catch (e: Exception) {

                                deleteError =
                                    "No se pudo conectar con el servidor."

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

    deleteError?.let { message ->

        AlertDialog(
            onDismissRequest = {
                deleteError = null
            },
            title = {
                Text("No se puede eliminar")
            },
            text = {
                Text(message)
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        deleteError = null
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
    }
}