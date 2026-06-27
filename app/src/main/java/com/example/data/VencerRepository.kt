package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class VencerRepository(context: Context) {
    private val db = VencerDatabase.getInstance(context)
    private val dao = db.dao

    // Blocker Settings
    fun getBlockerSettingsFlow(): Flow<BlockerSettings?> = dao.getBlockerSettingsFlow()
    
    suspend fun getBlockerSettings(): BlockerSettings {
        return dao.getBlockerSettings() ?: BlockerSettings().also {
            dao.insertBlockerSettings(it)
        }
    }

    suspend fun updateBlockerSettings(settings: BlockerSettings) {
        dao.insertBlockerSettings(settings)
    }

    // Parent Logs
    fun getAllParentLogsFlow(): Flow<List<ParentLog>> = dao.getAllParentLogsFlow()

    suspend fun insertParentLog(log: ParentLog) {
        dao.insertParentLog(log)
    }

    suspend fun clearParentLogs() {
        dao.clearParentLogs()
    }

    // Sobriety Log
    fun getSobrietyLogFlow(): Flow<SobrietyLog?> = dao.getSobrietyLogFlow()

    suspend fun getSobrietyLog(): SobrietyLog {
        return dao.getSobrietyLog() ?: SobrietyLog().also {
            dao.insertSobrietyLog(it)
        }
    }

    suspend fun updateSobrietyLog(log: SobrietyLog) {
        dao.insertSobrietyLog(log)
    }

    // Chat Messages
    fun getAllChatMessagesFlow(): Flow<List<ChatMessage>> = dao.getAllChatMessagesFlow()

    suspend fun insertChatMessage(message: ChatMessage) {
        dao.insertChatMessage(message)
    }

    suspend fun clearChatMessages() {
        dao.clearChatMessages()
    }

    // AI Counselor call
    suspend fun getAICounselorResponse(messages: List<ChatMessage>): String {
        return GeminiClient.getCounselorResponse(messages)
    }
}
