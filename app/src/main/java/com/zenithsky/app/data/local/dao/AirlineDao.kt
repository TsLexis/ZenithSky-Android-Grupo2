package com.zenithsky.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.zenithsky.app.data.local.entity.AirlineLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface AirlineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(airline: AirlineLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(airlines: List<AirlineLocal>)

    @Query("SELECT * FROM airline_local ORDER BY nombre")
    fun obtenerTodas(): Flow<List<AirlineLocal>>

    @Query("SELECT * FROM airline_local WHERE id = :id")
    suspend fun obtenerPorId(id: Int): AirlineLocal?

    @Query("DELETE FROM airline_local")
    suspend fun eliminarTodas()

    @Query("DELETE FROM airline_local WHERE id = :id")
    suspend fun eliminarPorId(id: Int)

    @Transaction
    suspend fun reemplazarTodas(airlines: List<AirlineLocal>) {
        eliminarTodas()
        insertarTodas(airlines)
    }
}