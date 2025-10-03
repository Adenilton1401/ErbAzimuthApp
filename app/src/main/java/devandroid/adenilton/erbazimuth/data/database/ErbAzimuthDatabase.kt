package devandroid.adenilton.erbazimuth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Erb

@Database(entities = [Erb::class, Azimute::class], version = 1, exportSchema = false)
abstract class ErbAzimuthDatabase : RoomDatabase() {

    abstract fun erbAzimuthDao(): ErbAzimuthDao

    companion object {
        @Volatile
        private var INSTANCE: ErbAzimuthDatabase? = null

        fun getDatabase(context: Context): ErbAzimuthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ErbAzimuthDatabase::class.java,
                    "erbazimuth_database"
                )
                    .fallbackToDestructiveMigration() // Política de migração simples para desenvolvimento
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}