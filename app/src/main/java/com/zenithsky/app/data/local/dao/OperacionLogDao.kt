package com.zenithsky.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.zenithsky.app.data.local.entity.OperacionLog
import kotlinx.coroutines.flow.Flow

@Dao
interface OperacionLogDao {

    @Insert
    suspend fun insertar(log: OperacionLog)

    @Query("SELECT * FROM operacion_log ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<OperacionLog>>

    @Query("DELETE FROM operacion_log")
    suspend fun eliminarTodos()

    @Delete
    suspend fun eliminar(log: OperacionLog)
}