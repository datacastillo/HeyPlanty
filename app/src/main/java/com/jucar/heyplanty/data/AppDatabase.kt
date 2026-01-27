package com.jucar.heyplanty.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jucar.heyplanty.domain.Planta
import com.jucar.heyplanty.domain.RiegoEvento

@Database(entities = [Planta::class, RiegoEvento::class], version = 2) // Subimos versión
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantaDao(): PlantaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "heyplanty_db"
                )
                    .fallbackToDestructiveMigration() // Limpia la DB si hay cambios de versión
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}