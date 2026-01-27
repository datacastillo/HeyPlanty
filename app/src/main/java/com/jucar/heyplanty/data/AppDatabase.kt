package com.jucar.heyplanty.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jucar.heyplanty.domain.Planta

// Definimos qué tablas tiene la base de datos y la versión
@Database(entities = [Planta::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantaDao(): PlantaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Si ya existe la base de datos, la devolvemos; si no, la creamos
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "heyplanty_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}