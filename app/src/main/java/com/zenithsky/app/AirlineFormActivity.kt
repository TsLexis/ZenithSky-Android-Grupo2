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

        val airlineId =
            intent.getIntExtra("airline_id", -1)

        setContent {
            ZenithSkyTheme {

                AirlineFormScreen(
                    airlineId = airlineId,
                    onFinish = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirlineFormScreen(
    airlineId: Int,
    onFinish: () -> Unit
) {

    var iata by remember {
        mutableStateOf("")
    }

    var airlineName by remember {
        mutableStateOf("")
    }

    var airports by remember {
        mutableStateOf<List<AirportDto>>(emptyList())
    }

    var selectedAirport by remember {
        mutableStateOf<AirportDto?>(null)
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    val editing = airlineId != -1

    LaunchedEffect(airlineId) {

        try {

            val airportResponse =
                ApiClient.airportService.getAirports()

            if (airportResponse.isSuccessful) {

                airports =
                    airportResponse.body() ?: emptyList()

            } else {

                error =
                    "No se pudieron cargar los aeropuertos"
            }

            if (editing) {

                val response =
                    ApiClient.airlineService
                        .getAirlineById(airlineId)

                if (response.isSuccessful) {

                    val airline = response.body()

                    if (airline != null) {

                        iata = airline.iata

                        airlineName =
                            airline.airlinename

                        selectedAirport =
                            airports.find {
                                it.airport_id ==
                                        airline.base_airport
                            }
                    }

                } else {

                    error =
                        "No se pudo consultar la aerolínea"
                }
            }

        } catch (e: Exception) {

            error =
                "Error de conexión: ${e.message}"
        }

        loading = false
    }

    Scaffold(
        topBar = {

            TopAppBar(

                title = {
                    Text(
                        if (editing)
                            "Editar aerolínea"
                        else
                            "Registrar aerolínea"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onFinish
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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            if (loading) {

                CircularProgressIndicator()

            } else {

                OutlinedTextField(

                    value = iata,

                    onValueChange = {

                        iata = it
                            .uppercase()
                            .filter { char ->
                                char.isLetter()
                            }
                            .take(2)
                    },

                    label = {
                        Text("Código IATA")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    supportingText = {
                        Text("${iata.length}/2 caracteres")
                    }
                )

                OutlinedTextField(

                    value = airlineName,

                    onValueChange = {
                        airlineName = it
                    },

                    label = {
                        Text(
                            "Nombre de la aerolínea"
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true
                )

                Text(
                    text = "Aeropuerto base",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Box {

                    OutlinedButton(

                        onClick = {
                            expanded = true
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(

                            selectedAirport?.let {

                                "${it.iata} - ${it.name}"

                            } ?: "Seleccionar aeropuerto"
                        )
                    }

                    DropdownMenu(

                        expanded = expanded,

                        onDismissRequest = {
                            expanded = false
                        },

                        modifier =
                            Modifier.heightIn(
                                max = 300.dp
                            )
                    ) {

                        airports.forEach { airport ->

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        "${airport.iata} - ${airport.name}"
                                    )
                                },

                                onClick = {

                                    selectedAirport =
                                        airport

                                    expanded = false
                                }
                            )
                        }
                    }
                }

                error?.let {

                    Text(
                        text = it,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                Button(

                    onClick = {

                        if (
                            iata.length != 2 ||
                            airlineName.isBlank() ||
                            selectedAirport == null
                        ) {

                            error = when {

                                iata.length != 2 ->
                                    "El código IATA debe tener exactamente 2 caracteres."

                                airlineName.isBlank() ->
                                    "Ingresa el nombre de la aerolínea."

                                selectedAirport == null ->
                                    "Selecciona un aeropuerto base."

                                else ->
                                    "Completa todos los campos."
                            }

                        } else {

                            scope.launch {

                                saving = true
                                error = null

                                try {

                                    val airline =
                                        AirlineDto(

                                            airline_id =
                                                if (editing)
                                                    airlineId
                                                else
                                                    0,

                                            iata =
                                                iata.trim(),

                                            airlinename =
                                                airlineName.trim(),

                                            base_airport =
                                                selectedAirport!!
                                                    .airport_id,

                                            base_airport_iata =
                                                selectedAirport!!
                                                    .iata,

                                            base_airport_name =
                                                selectedAirport!!
                                                    .name
                                        )

                                    val response =
                                        if (editing) {

                                            ApiClient.airlineService
                                                .updateAirline(
                                                    airlineId,
                                                    airline
                                                )

                                        } else {

                                            ApiClient.airlineService
                                                .createAirline(
                                                    airline
                                                )
                                        }

                                    if (response.isSuccessful) {

                                        onFinish()

                                    } else {

                                        error =
                                            when (response.code()) {

                                                409 ->
                                                    "No se puede guardar porque la aerolínea ya existe."

                                                400 ->
                                                    "Los datos de la aerolínea no son válidos."

                                                else ->
                                                    "No se pudo guardar. Código: ${response.code()}"
                                            }
                                    }

                                } catch (e: Exception) {

                                    error =
                                        "No se pudo conectar con el servidor."
                                }

                                saving = false
                            }
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled = !saving
                ) {

                    Text(

                        when {

                            saving ->
                                "Guardando..."

                            editing ->
                                "Actualizar"

                            else ->
                                "Registrar"
                        }
                    )
                }

                OutlinedButton(

                    onClick = onFinish,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("Cancelar")
                }
            }
        }
    }
}