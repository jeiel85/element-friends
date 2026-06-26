package com.elementfriends.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "discovered_formulas")
data class DiscoveredFormulaEntity(
    @PrimaryKey val formulaId: String,
    val name: String,
    val description: String,
    val emoji: String,
    val discoveredAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "game_progress")
data class ProgressEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val experience: Int = 0,
    val cloudSyncCode: String = "",
    val lastSyncedAt: Long = 0L,
    val lastQuizSolvedDate: String = "",
    val quizStreak: Int = 0,
    val totalQuizzesSolved: Int = 0,
    val hasCompletedTutorial: Boolean = false,
    val maxCombo: Int = 0,
    val lastHintDate: String = "",
    val hintsUsedToday: Int = 0
)

@Dao
interface FormulaDao {
    @Query("SELECT * FROM discovered_formulas ORDER BY discoveredAt DESC")
    fun getAllDiscovered(): Flow<List<DiscoveredFormulaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormula(formula: DiscoveredFormulaEntity)

    @Query("SELECT COUNT(*) FROM discovered_formulas WHERE formulaId = :id")
    suspend fun exists(id: String): Int

    @Query("DELETE FROM discovered_formulas")
    suspend fun clearAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(formulas: List<DiscoveredFormulaEntity>)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM game_progress WHERE id = 1")
    fun getProgressFlow(): Flow<ProgressEntity?>

    @Query("SELECT * FROM game_progress WHERE id = 1")
    suspend fun getProgress(): ProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)
}

@Database(entities = [DiscoveredFormulaEntity::class, ProgressEntity::class], version = 5, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun formulaDao(): FormulaDao
    abstract fun progressDao(): ProgressDao
}
