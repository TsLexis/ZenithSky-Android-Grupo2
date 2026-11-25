package com.zenithsky.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zenithsky.app.data.local.entity.AvionDestacado
import kotlinx.coroutines.flow.Flow

@Dao
interface AvionDestacadoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(avion: AvionDestacado)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(aviones: List<AvionDestacado>)

    @Query("SELECT * FROM avion_destacado")
    fun obtenerTodos(): Flow<List<AvionDestacado>>

    @Query("SELECT * FROM avion_destacado WHERE id = :id")
    suspend fun obtenerPorId(id: Int): AvionDestacado?

    @Query("DELETE FROM avion_destacado")
    suspend fun eliminarTodos()

    @Query("DELETE FROM avion_destacado WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}