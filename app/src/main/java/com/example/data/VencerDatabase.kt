package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "blocker_settings")
data class BlockerSettings(
    @PrimaryKey val id: Int = 1,
    val isBlockerEnabled: Boolean = false,
    val strictMode: Boolean = false,
    val parentPasscode: String = ""
)

@Entity(tableName = "parent_logs")
data class ParentLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val searchQueryOrUrl: String,
    val actionTaken: String,
    val isAlert: Boolean,
    val childEmail: String
)

@Entity(tableName = "sobriety_logs")
data class SobrietyLog(
    @PrimaryKey val id: Int = 1,
    val startDateTimestamp: Long = System.currentTimeMillis(),
    val totalRelapses: Int = 0,
    val lastRelapseTimestamp: Long = 0L,
    val motivationPhrase: String = "Você é mais forte do que seus desejos!"
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VencerDao {
    // Blocker Settings
    @Query("SELECT * FROM blocker_settings WHERE id = 1")
    fun getBlockerSettingsFlow(): Flow<BlockerSettings?>

    @Query("SELECT * FROM blocker_settings WHERE id = 1")
    suspend fun getBlockerSettings(): BlockerSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockerSettings(settings: BlockerSettings)

    // Parent Logs
    @Query("SELECT * FROM parent_logs ORDER BY timestamp DESC")
    fun getAllParentLogsFlow(): Flow<List<ParentLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentLog(log: ParentLog)

    @Query("DELETE FROM parent_logs")
    suspend fun clearParentLogs()

    // Sobriety Log
    @Query("SELECT * FROM sobriety_logs WHERE id = 1")
    fun getSobrietyLogFlow(): Flow<SobrietyLog?>

    @Query("SELECT * FROM sobriety_logs WHERE id = 1")
    suspend fun getSobrietyLog(): SobrietyLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSobrietyLog(log: SobrietyLog)

    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()
}

@Database(
    entities = [BlockerSettings::class, ParentLog::class, SobrietyLog::class, ChatMessage::class],
    version = 2,
    exportSchema = false
)
abstract class VencerDatabase : RoomDatabase() {
    abstract val dao: VencerDao

    companion object {
        @Volatile
        private var INSTANCE: VencerDatabase? = null

        fun getInstance(context: Context): VencerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VencerDatabase::class.java,
                    "vencer_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
