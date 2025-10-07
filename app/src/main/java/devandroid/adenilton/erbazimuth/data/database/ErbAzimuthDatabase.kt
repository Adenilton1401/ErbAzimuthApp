package devandroid.adenilton.erbazimuth.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import devandroid.adenilton.erbazimuth.data.model.Azimute
import devandroid.adenilton.erbazimuth.data.model.Caso
import devandroid.adenilton.erbazimuth.data.model.Erb

@Database(
    entities = [Erb::class, Azimute::class, Caso::class],
    version = 2, // Mantemos a versão 2
    exportSchema = false // Podemos desativar, já que não usaremos a migração automática por enquanto
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
                    // --- CORREÇÃO AQUI ---
                    // Se uma migração for necessária, simplesmente apaga e recria o banco.
                    // ATENÇÃO: Isso apagará todos os dados existentes no app.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

