package com.zenithsky.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenithsky.app.data.model.AirlineDto
import com.zenithsky.app.data.model.AirplaneDto
import com.zenithsky.app.data.model.AirplaneTypeDto
import com.zenithsky.app.data.network.ApiClient
import com.zenithsky.app.ui.theme.ZenithSkyTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

private const val AIRPLANE_FORM_TAG = "AirplaneFormActivity"

class AirplaneFormActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenithSkyTheme {
                AirplaneFormScreen(
                    onSaved = {
                        Toast.makeText(
                            this,
                            "Avión registrado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    },
                    onCancel = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirplaneFormScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {

    var capacityText by rememberSaveable { mutableStateOf("") }
    var selectedTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedAirlineId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showValidationErrors by rememberSaveable { mutableStateOf(false) }

    var airplaneTypes by remember { mutableStateOf<List<AirplaneTypeDto>>(emptyList()) }
    var airlines by remember { mutableStateOf<List<AirlineDto>>(emptyList()) }
    var isLoadingCatalogs by remember { mutableStateOf(true) }
    var catalogsErrorMessage by remember { mutableStateOf<String?>(null) }
    var catalogsReloadKey by remember { mutableIntStateOf(0) }

    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(catalogsReloadKey) {
        isLoadingCatalogs = true
        catalogsErrorMessage = null
        try {
            coroutineScope {
                val typesRequest = async { ApiClient.airplaneTypeService.getAirplaneTypes() }
                val airlinesRequest = async { ApiClient.airlineService.getAirlines() }

                val typesResponse = typesRequest.await()
                val airlinesResponse = airlinesRequest.await()
                val errors = mutableListOf<String>()

                if (typesResponse.isSuccessful) {
                    airplaneTypes = typesResponse.body().orEmpty()
                    if (airplaneTypes.isEmpty()) {
                        errors += "No hay tipos de avión registrados."
                    }
                } else {
                    Log.e(AIRPLANE_FORM_TAG, "Error HTTP al cargar tipos de avión: ${typesResponse.code()}")
                    errors += "No se pudieron cargar los tipos de avión (código ${typesResponse.code()})."
                }

                if (airlinesResponse.isSuccessful) {
                    airlines = airlinesResponse.body().orEmpty()
                    if (airlines.isEmpty()) {
                        errors += "No hay aerolíneas registradas."
                    }
                } else {
                    Log.e(AIRPLANE_FORM_TAG, "Error HTTP al cargar aerolíneas: ${airlinesResponse.code()}")
                    errors += "No se pudieron cargar las aerolíneas (código ${airlinesResponse.code()})."
                }

                catalogsErrorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(AIRPLANE_FORM_TAG, "Error de E/S al cargar las opciones del formulario", e)
            catalogsErrorMessage = "No se pudo conectar con el servidor para cargar las opciones del formulario."
        } catch (e: Exception) {
            Log.e(AIRPLANE_FORM_TAG, "Error inesperado al cargar las opciones del formulario", e)
            catalogsErrorMessage = "Ocurrió un error inesperado al cargar las opciones del formulario."
        } finally {
            isLoadingCatalogs = false
        }
    }

    fun submit() {
        showValidationErrors = true
        saveErrorMessage = null

        val capacity = parseCapacity(capacityText)
        val typeId = selectedTypeId
        val airlineId = selectedAirlineId
        if (capacity == null || typeId == null || airlineId == null) return

        isSaving = true
        scope.launch {
            var saved = false
            try {
                val response = ApiClient.airplaneService.createAirplane(
                    AirplaneDto(
                        airplane_id = 0,
                        capacity = capacity,
                        type_id = typeId,
                        airline_id = airlineId,
                        identifier = null,
                        description = null,
                        airlinename = null
                    )
                )
                if (response.isSuccessful) {
                    saved = true
                    onSaved()
                } else {
                    Log.e(
                        AIRPLANE_FORM_TAG,
                        "Error HTTP al crear avión: ${response.code()} ${response.errorBody()?.string()?.take(500)}"
                    )
                    saveErrorMessage = createAirplaneErrorMessage(response.code())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(AIRPLANE_FORM_TAG, "Error de E/S al crear avión", e)
                saveErrorMessage = "No se pudo conectar con el servidor. Verifique su conexión e intente de nuevo."
            } catch (e: Exception) {
                Log.e(AIRPLANE_FORM_TAG, "Error inesperado al crear avión", e)
                saveErrorMessage = "Ocurrió un error inesperado al registrar el avión."
            } finally {
                if (!saved) {
                    isSaving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar avión") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (isLoadingCatalogs) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Cargando opciones...")
                }
            }

            catalogsErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { catalogsReloadKey++ }) {
                    Text("Reintentar carga")
                }
            }

            AirplaneFormFields(
                capacityText = capacityText,
                onCapacityChange = { capacityText = it },
                capacityError = if (showValidationErrors) capacityValidationMessage(capacityText) else null,
                airplaneTypes = airplaneTypes,
                selectedTypeId = selectedTypeId,
                onTypeSelected = { selectedTypeId = it },
                typeError = if (showValidationErrors && selectedTypeId == null) {
                    "Seleccione un tipo de avión."
                } else {
                    null
                },
                airlines = airlines,
                selectedAirlineId = selectedAirlineId,
                onAirlineSelected = { selectedAirlineId = it },
                airlineError = if (showValidationErrors && selectedAirlineId == null) {
                    "Seleccione una aerolínea."
                } else {
                    null
                },
                enabled = !isSaving
            )

            saveErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = { submit() },
                enabled = !isSaving && !isLoadingCatalogs,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Guardando...")
                } else {
                    Text("Guardar avión")
                }
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
fun AirplaneFormFields(
    capacityText: String,
    onCapacityChange: (String) -> Unit,
    capacityError: String?,
    airplaneTypes: List<AirplaneTypeDto>,
    selectedTypeId: Int?,
    onTypeSelected: (Int) -> Unit,
    typeError: String?,
    airlines: List<AirlineDto>,
    selectedAirlineId: Int?,
    onAirlineSelected: (Int) -> Unit,
    airlineError: String?,
    enabled: Boolean
) {

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        OutlinedTextField(
            value = capacityText,
            onValueChange = onCapacityChange,
            label = { Text("Capacidad de pasajeros") },
            singleLine = true,
            enabled = enabled,
            isError = capacityError != null,
            supportingText = if (capacityError != null) {
                { Text(capacityError) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        AirplaneSelectorField(
            label = "Tipo de avión",
            options = airplaneTypes.map { type ->
                type.type_id to listOfNotNull(type.identifier, type.description).joinToString(" - ")
            },
            selectedId = selectedTypeId,
            onSelected = onTypeSelected,
            errorMessage = typeError,
            enabled = enabled
        )

        AirplaneSelectorField(
            label = "Aerolínea",
            options = airlines.map { airline ->
                airline.airline_id to "${airline.airlinename} (${airline.iata})"
            },
            selectedId = selectedAirlineId,
            onSelected = onAirlineSelected,
            errorMessage = airlineError,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AirplaneSelectorField(
    label: String,
    options: List<Pair<Int, String>>,
    selectedId: Int?,
    onSelected: (Int) -> Unit,
    errorMessage: String?,
    enabled: Boolean
) {

    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selectedId }?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text("Seleccione una opción") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = errorMessage != null,
            supportingText = if (errorMessage != null) {
                { Text(errorMessage) }
            } else {
                null
            },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No hay opciones disponibles") },
                    onClick = { expanded = false },
                    enabled = false
                )
            }

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

private fun parseCapacity(text: String): Int? {
    return text.trim().toIntOrNull()?.takeIf { it > 0 }
}

private fun capacityValidationMessage(text: String): String? {
    return when {
        text.isBlank() -> "La capacidad es obligatoria."
        parseCapacity(text) == null -> "La capacidad debe ser un número entero mayor que cero."
        else -> null
    }
}

private fun createAirplaneErrorMessage(code: Int): String {
    return when (code) {
        400 -> "Los datos enviados no son válidos. Revise la información e intente de nuevo."
        404 -> "El tipo de avión o la aerolínea seleccionada no existe."
        409 -> "No se pudo registrar el avión porque entra en conflicto con datos existentes."
        in 500..599 -> "Error interno del servidor. Intente más tarde."
        else -> "No se pudo registrar el avión (código $code)."
    }
}
