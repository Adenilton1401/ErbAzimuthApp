package devandroid.adenilton.erbazimuth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import devandroid.adenilton.erbazimuth.data.model.*

@Database(
    entities = [Erb::class, Azimute::class, Caso::class, LocalInteresse::class, CellTowerCache::class],
    version = 5, // A versão pode continuar a mesma
    exportSchema = false
)
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
                    "erb_azimuth_database"
                )
                    .fallbackToDestructiveMigration()
                    // A lógica de Callback foi removida daqui
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

