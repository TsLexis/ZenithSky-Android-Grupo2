package com.zenithsky.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zenithsky.app.data.model.AirlineDto
import com.zenithsky.app.data.model.AirplaneDto
import com.zenithsky.app.data.model.AirplaneTypeDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

private const val TAG = "AirplaneListActivity"

// Desde Android 17 (API 37) el acceso a la red local (incluido 10.0.2.2 del emulador) requiere este permiso
private const val LOCAL_NETWORK_PERMISSION = Manifest.permission.ACCESS_LOCAL_NETWORK
private const val LOCAL_NETWORK_PERMISSION_SDK = 37

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

    val context = LocalContext.current
    var hasNetworkPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK ||
                ContextCompat.checkSelfPermission(context, LOCAL_NETWORK_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.w(TAG, "Permiso ACCESS_LOCAL_NETWORK denegado")
        }
        hasNetworkPermission = granted
    }
    val formLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadKey++
        }
    }

    LaunchedEffect(Unit) {
        if (!hasNetworkPermission) {
            permissionLauncher.launch(LOCAL_NETWORK_PERMISSION)
        }
    }

    LaunchedEffect(hasNetworkPermission) {
        if (!hasNetworkPermission) return@LaunchedEffect
        filtersErrorMessage = null
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
                    Log.e(
                        TAG,
                        "Error HTTP en filtros: tipos=${typesResponse.code()}, aerolíneas=${airlinesResponse.code()}"
                    )
                    filtersErrorMessage = "No se pudieron cargar todas las opciones de filtrado."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar las opciones de filtrado", e)
            filtersErrorMessage = "No se pudieron cargar las opciones de filtrado."
        }
    }

    var airplanePendingDelete by remember { mutableStateOf<AirplaneDto?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun deleteAirplane(airplane: AirplaneDto) {
        if (isDeleting) return
        isDeleting = true
        scope.launch {
            val message = try {
                val response = ApiClient.airplaneService.deleteAirplane(airplane.airplane_id)
                if (response.isSuccessful) {
                    airplanes = airplanes.filterNot { it.airplane_id == airplane.airplane_id }
                    reloadKey++
                    "Avión eliminado correctamente"
                } else {
                    Log.e(
                        TAG,
                        "Error HTTP al eliminar avión ${airplane.airplane_id}: ${response.code()} " +
                            "${response.errorBody()?.string()?.take(500)}"
                    )
                    if (response.code() == 404) {
                        reloadKey++
                    }
                    deleteAirplaneErrorMessage(response.code())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error de E/S al eliminar avión ${airplane.airplane_id}", e)
                "No se pudo conectar con el servidor. El avión no fue eliminado."
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al eliminar avión ${airplane.airplane_id}", e)
                "Ocurrió un error inesperado al eliminar el avión."
            }
            isDeleting = false
            airplanePendingDelete = null
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(selectedTypeId, selectedAirlineId, reloadKey, hasNetworkPermission) {
        if (!hasNetworkPermission) return@LaunchedEffect
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
                Log.e(TAG, "Error HTTP al cargar aviones: ${response.code()}")
                airplanes = emptyList()
                errorMessage = serverErrorMessage(response)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error de E/S al cargar aviones", e)
            airplanes = emptyList()
            errorMessage = "No se pudo conectar con el servidor. Verifique su conexión e intente de nuevo."
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al cargar aviones", e)
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
        },
        floatingActionButton = {
            if (hasNetworkPermission) {
                ExtendedFloatingActionButton(
                    onClick = {
                        formLauncher.launch(Intent(context, AirplaneFormActivity::class.java))
                    }
                ) {
                    Text("Agregar avión")
                }
            }
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
                    !hasNetworkPermission -> {
                        AirplaneMessageState(
                            message = "Se necesita el permiso de acceso a la red local para conectarse con el servidor. " +
                                "Si lo denegó definitivamente, actívelo en Ajustes > Aplicaciones > ZenithSky > Permisos.",
                            isError = true,
                            actionLabel = "Conceder permiso",
                            onAction = { permissionLauncher.launch(LOCAL_NETWORK_PERMISSION) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    errorMessage != null -> {
                        AirplaneMessageState(
                            message = errorMessage.orEmpty(),
                            isError = true,
                            actionLabel = "Reintentar",
                            onAction = { reloadKey++ },
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
                            contentPadding = PaddingValues(bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(airplanes, key = { it.airplane_id }) { airplane ->
                                AirplaneItem(
                                    airplane = airplane,
                                    onEdit = {
                                        formLauncher.launch(
                                            Intent(context, AirplaneFormActivity::class.java)
                                                .putExtra(AirplaneFormActivity.EXTRA_AIRPLANE_ID, airplane.airplane_id)
                                        )
                                    },
                                    onDelete = { airplanePendingDelete = airplane }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    airplanePendingDelete?.let { airplane ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) airplanePendingDelete = null },
            title = { Text("Eliminar avión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Avión #${airplane.airplane_id} - ${airplane.identifier ?: "Tipo no disponible"}, " +
                            "${airplane.capacity} pasajeros",
                        fontWeight = FontWeight.Bold
                    )
                    Text("¿Está seguro de que desea eliminar este avión? Esta acción no se puede deshacer.")
                    if (isDeleting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Eliminando...")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { deleteAirplane(airplane) },
                    enabled = !isDeleting
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { airplanePendingDelete = null },
                    enabled = !isDeleting
                ) {
                    Text("Cancelar")
                }
            }
        )
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
fun AirplaneItem(
    airplane: AirplaneDto,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {

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

            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    onEdit?.let {
                        TextButton(onClick = it) {
                            Text("Editar")
                        }
                    }
                    onDelete?.let {
                        TextButton(onClick = it) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AirplaneMessageState(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
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

        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

private fun deleteAirplaneErrorMessage(code: Int): String {
    return when (code) {
        404 -> "El avión ya no existe. La lista se actualizará."
        409 -> "El servidor no permitió eliminar el avión porque está relacionado con otros datos."
        in 500..599 -> "Error interno del servidor. El avión no fue eliminado."
        else -> "No se pudo eliminar el avión (código $code)."
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
