package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sobriety_streak")
data class SobrietyStreak(
    @PrimaryKey val id: Int = 1,
    val streakStartDate: Long, // timestamp
    val bestStreakDays: Int = 0,
    val totalCleanDays: Int = 0,
    val totalRelapses: Int = 0
)

@Entity(tableName = "relapse_log")
data class RelapseLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val reason: String,
    val trigger: String,
    val intensity: Int
)

@Entity(tableName = "support_chat")
data class SupportChat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val message: String,
    val timestamp: Long
)

@Entity(tableName = "payment_slip")
data class PaymentSlip(
    @PrimaryKey val transactionId: String,
    val phoneNumber: String,
    val planSelected: String, // "Mensal - 100 MT" or "Anual - 700 MT"
    val dateRegistered: Long,
    val status: String // "Pendente" or "Ativo"
)

@Entity(tableName = "blocker_settings")
data class BlockerSettings(
    @PrimaryKey val id: Int = 1,
    val pinCode: String = "", // empty if password not set
    val isBlockerEnabled: Boolean = false,
    val strictMode: Boolean = false
)

@Entity(tableName = "forum_post")
data class ForumPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author: String,
    val content: String,
    val timestamp: Long,
    val location: String = "Moçambique",
    val supportsCount: Int = 0
)

@Entity(tableName = "parent_log")
data class ParentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val searchQueryOrUrl: String,
    val actionTaken: String, // "Bloqueado", "Seguro", "Alerta"
    val isAlert: Boolean = false,
    val childEmail: String = ""
)

@Dao
interface VencerDao {
    // 1. SobrietyStreak queries
    @Query("SELECT * FROM sobriety_streak WHERE id = 1")
    fun getSobrietyStreakFlow(): Flow<SobrietyStreak?>

    @Query("SELECT * FROM sobriety_streak WHERE id = 1")
    suspend fun getSobrietyStreak(): SobrietyStreak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSobrietyStreak(streak: SobrietyStreak)

    // 2. RelapseLog queries
    @Query("SELECT * FROM relapse_log ORDER BY timestamp DESC")
    fun getAllRelapsesFlow(): Flow<List<RelapseLog>>

    @Insert
    suspend fun insertRelapseLog(log: RelapseLog)

    @Query("DELETE FROM relapse_log")
    suspend fun clearRelapseLogs()

    // 3. SupportChat queries
    @Query("SELECT * FROM support_chat ORDER BY timestamp ASC")
    fun getAllChatMessagesFlow(): Flow<List<SupportChat>>

    @Insert
    suspend fun insertChatMessage(msg: SupportChat)

    @Query("DELETE FROM support_chat")
    suspend fun clearChatMessages()

    // 4. PaymentSlip queries
    @Query("SELECT * FROM payment_slip ORDER BY dateRegistered DESC")
    fun getAllPaymentSlipsFlow(): Flow<List<PaymentSlip>>

    @Query("SELECT * FROM payment_slip WHERE status = 'Ativo' OR status = 'Aprovado' LIMIT 1")
    suspend fun getActivePayment(): PaymentSlip?

    @Query("SELECT * FROM payment_slip WHERE status = 'Ativo' OR status = 'Aprovado' LIMIT 1")
    fun getActivePaymentFlow(): Flow<PaymentSlip?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentSlip(slip: PaymentSlip)

    // 5. BlockerSettings queries
    @Query("SELECT * FROM blocker_settings WHERE id = 1")
    fun getBlockerSettingsFlow(): Flow<BlockerSettings?>

    @Query("SELECT * FROM blocker_settings WHERE id = 1")
    suspend fun getBlockerSettings(): BlockerSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockerSettings(settings: BlockerSettings)

    // 6. ForumPost queries
    @Query("SELECT * FROM forum_post ORDER BY timestamp DESC")
    fun getAllForumPostsFlow(): Flow<List<ForumPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForumPost(post: ForumPost)

    @Query("UPDATE forum_post SET supportsCount = supportsCount + 1 WHERE id = :postId")
    suspend fun supportPost(postId: Int)

    @Query("SELECT COUNT(*) FROM forum_post")
    suspend fun getForumPostsCount(): Int

    // 7. ParentLog queries
    @Query("SELECT * FROM parent_log ORDER BY timestamp DESC")
    fun getAllParentLogsFlow(): Flow<List<ParentLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentLog(log: ParentLog)

    @Query("SELECT COUNT(*) FROM parent_log")
    suspend fun getParentLogsCount(): Int

    @Query("DELETE FROM parent_log")
    suspend fun clearParentLogs()
}

@Database(
    entities = [
        SobrietyStreak::class,
        RelapseLog::class,
        SupportChat::class,
        PaymentSlip::class,
        BlockerSettings::class,
        ForumPost::class,
        ParentLog::class
    ],
    version = 4,
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
