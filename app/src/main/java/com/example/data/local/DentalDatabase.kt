package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        TransactionEntity::class,
        AppointmentEntity::class,
        InventoryEntity::class,
        NpsSurveyEntity::class,
        PatientEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DentalDatabase : RoomDatabase() {
    abstract fun dentalDao(): DentalDao

    companion object {
        @Volatile
        private var INSTANCE: DentalDatabase? = null

        fun getDatabase(context: Context): DentalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DentalDatabase::class.java,
                    "dental_bi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
