package devandroid.adenilton.erbazimuth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.data.model.Erb
import devandroid.adenilton.erbazimuth.data.model.LocalInteresse

@Database(
    // ATUALIZADO: Adiciona a nova entidade à lista
    entities = [Erb::class, Azimute::class, Caso::class, LocalInteresse::class],
    version = 3, // Incrementamos a versão para sinalizar uma mudança na estrutura
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
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

