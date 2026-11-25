package com.zenithsky.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenithsky.app.data.local.database.ZenithSkyDatabase
import com.zenithsky.app.ui.theme.ZenithSkyTheme

class OperacionLogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ZenithSkyDatabase.getDatabase(applicationContext)

        setContent {
            ZenithSkyTheme {
                OperacionLogScreen(database)
            }
        }
    }
}

@Composable
fun OperacionLogScreen(database: ZenithSkyDatabase) {

    val logs by database
        .operacionLogDao()
        .obtenerTodos()
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Registro de operaciones"
        )

        LazyColumn {

            items(logs) { log ->

                Text(
                    text = "${log.operacion} - ${log.entidad}: ${log.detalle}"
                )
            }
        }
    }
}