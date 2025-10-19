package com.example.weight_trackerapp.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * This is one row in our "weights" table.
 * We only store kilograms here (the UI can convert to pounds on the fly).
 */
@Entity(tableName = "weights")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,          // e.g., "2025-10-12"
    val weightKg: Double,      // always in kilograms
    val note: String? = null   // optional free text
)

/**
 * DAO = the handful of queries we run.
 * We keep it tiny on purpose (insert, read all, delete one).
 */
@Dao
interface WeightDao {
    // Flow means Compose can observe this and auto-refresh when the DB changes.
    @Query("SELECT * FROM weights ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WeightEntity>>

    @Insert
    suspend fun insert(entity: WeightEntity)

    @Query("DELETE FROM weights WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/**
 * The Room database. Singleton so we don't waste memory or leak contexts.
 */
@Database(entities = [WeightEntity::class], version = 1, exportSchema = false)
abstract class WeightDatabase : RoomDatabase() {
    abstract fun weightDao(): WeightDao

    companion object {
        @Volatile private var INSTANCE: WeightDatabase? = null

        fun getInstance(context: Context): WeightDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WeightDatabase::class.java,
                    "weights.db"
                ).build().also { INSTANCE = it }
            }
    }
}
