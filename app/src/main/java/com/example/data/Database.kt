package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "completed_tasks")
data class CompletedTask(
    @PrimaryKey val taskId: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val currentStageId: Int = 1,
    val totalSavingsJod: Double = 0.0
)

@Dao
interface JourneyDao {
    @Query("SELECT * FROM completed_tasks")
    fun getCompletedTasks(): Flow<List<CompletedTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markTaskCompleted(completedTask: CompletedTask)

    @Query("DELETE FROM completed_tasks WHERE taskId = :taskId")
    suspend fun unmarkTaskCompleted(taskId: String)

    @Query("DELETE FROM completed_tasks")
    suspend fun resetAllTasks()

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProgress(progress: UserProgress)
}

@Database(entities = [CompletedTask::class, UserProgress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val journeyDao: JourneyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "germany_journey_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class JourneyRepository(private val dao: JourneyDao) {
    val completedTasks: Flow<List<CompletedTask>> = dao.getCompletedTasks()
    val userProgress: Flow<UserProgress?> = dao.getUserProgress()

    suspend fun markTaskCompleted(taskId: String) {
        dao.markTaskCompleted(CompletedTask(taskId))
    }

    suspend fun unmarkTaskCompleted(taskId: String) {
        dao.unmarkTaskCompleted(taskId)
    }

    suspend fun resetJourney() {
        dao.resetAllTasks()
        dao.saveUserProgress(UserProgress(currentStageId = 1, totalSavingsJod = 0.0))
    }

    suspend fun saveProgress(stageId: Int, savings: Double) {
        dao.saveUserProgress(UserProgress(currentStageId = stageId, totalSavingsJod = savings))
    }
}
