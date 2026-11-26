package com.zenithsky.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenithsky.app.data.local.dao.AirlineDao
import com.zenithsky.app.data.local.dao.AvionDestacadoDao
import com.zenithsky.app.data.local.dao.OperacionLogDao
import com.zenithsky.app.data.local.entity.AirlineLocal
import com.zenithsky.app.data.local.entity.AvionDestacado
import com.zenithsky.app.data.local.entity.OperacionLog

@Database(
    entities = [
        OperacionLog::class,
        AvionDestacado::class,
        AirlineLocal::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZenithSkyDatabase : RoomDatabase() {

    abstract fun operacionLogDao(): OperacionLogDao

    abstract fun avionDestacadoDao(): AvionDestacadoDao

    abstract fun airlineDao(): AirlineDao

    companion object {

        @Volatile
        private var INSTANCE: ZenithSkyDatabase? = null

        fun getDatabase(context: Context): ZenithSkyDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZenithSkyDatabase::class.java,
                    "zenithsky_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}