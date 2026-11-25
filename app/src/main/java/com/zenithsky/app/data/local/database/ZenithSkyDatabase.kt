package com.zenithsky.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenithsky.app.data.local.dao.AvionDestacadoDao
import com.zenithsky.app.data.local.dao.OperacionLogDao
import com.zenithsky.app.data.local.entity.AvionDestacado
import com.zenithsky.app.data.local.entity.OperacionLog

@Database(
    entities = [
        OperacionLog::class,
        AvionDestacado::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZenithSkyDatabase : RoomDatabase() {

    abstract fun operacionLogDao(): OperacionLogDao

    abstract fun avionDestacadoDao(): AvionDestacadoDao

    companion object {

        @Volatile
        private var INSTANCE: ZenithSkyDatabase? = null

        fun getDatabase(context: Context): ZenithSkyDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenithSkyDatabase::class.java,
                    "zenithsky_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}