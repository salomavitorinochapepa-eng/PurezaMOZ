package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VencerRepository(private val dao: VencerDao) {

    val sobrietyStreakFlow: Flow<SobrietyStreak?> = dao.getSobrietyStreakFlow()
    val allRelapsesFlow: Flow<List<RelapseLog>> = dao.getAllRelapsesFlow()
    val chatMessagesFlow: Flow<List<SupportChat>> = dao.getAllChatMessagesFlow()
    val paymentSlipsFlow: Flow<List<PaymentSlip>> = dao.getAllPaymentSlipsFlow()
    val activePaymentFlow: Flow<PaymentSlip?> = dao.getActivePaymentFlow()
    val blockerSettingsFlow: Flow<BlockerSettings?> = dao.getBlockerSettingsFlow()
    val allForumPostsFlow: Flow<List<ForumPost>> = dao.getAllForumPostsFlow()
    val parentLogsFlow: Flow<List<ParentLog>> = dao.getAllParentLogsFlow()

    suspend fun initDefaultData() = withContext(Dispatchers.IO) {
        if (dao.getSobrietyStreak() == null) {
            val initialStreak = SobrietyStreak(
                id = 1,
                streakStartDate = System.currentTimeMillis(),
                bestStreakDays = 0,
                totalCleanDays = 0,
                totalRelapses = 0
            )
            dao.insertSobrietyStreak(initialStreak)
        }
        if (dao.getBlockerSettings() == null) {
            val initialSettings = BlockerSettings(
                id = 1,
                pinCode = "",
                isBlockerEnabled = false,
                strictMode = false
            )
            dao.insertBlockerSettings(initialSettings)
        }
        if (dao.getForumPostsCount() == 0) {
            val defaultPosts = listOf(
                ForumPost(
                    author = "Guerreiro Anónimo",
                    content = "Viva malta! Hoje celebro 25 dias de liberdade. O truque tem sido largar o telemóvel na sala antes de ir dormir. Vamos vencer!",
                    timestamp = System.currentTimeMillis() - 72000000L, // ~20 hours ago
                    location = "Maputo",
                    supportsCount = 14
                ),
                ForumPost(
                    author = "Lutador Determinado",
                    content = "Dica crucial para os fins de semana: quando vier aquela ansiedade profunda ou tédio, saiam para correr ou façam flexões. O desvio mecânico vence os impulsos em 10 minutos.",
                    timestamp = System.currentTimeMillis() - 172800000L, // ~2 days ago
                    location = "Beira",
                    supportsCount = 9
                ),
                ForumPost(
                    author = "Jovem Vencedor",
                    content = "Activei o bloqueador de DNS privado recomendado no aplicativo (family.adguard-dns.com) no meu router e telemóvel. Recomendo imenso, bloqueia tudo na raiz!",
                    timestamp = System.currentTimeMillis() - 259200000L, // ~3 days ago
                    location = "Nampula",
                    supportsCount = 18
                ),
                ForumPost(
                    author = "Soldado Livre",
                    content = "Recaí ontem após 12 dias limpos. Me senti triste, mas não vou desistir. Vim aqui relatar e renovar a minha força. O reset não apaga a dedicação acumulada!",
                    timestamp = System.currentTimeMillis() - 345600000L, // ~4 days ago
                    location = "Chimoio",
                    supportsCount = 22
                )
            )
            defaultPosts.forEach { post ->
                dao.insertForumPost(post)
            }
        }
        if (dao.getParentLogsCount() == 0) {
            val initialLogs = listOf(
                ParentLog(
                    timestamp = System.currentTimeMillis() - 11000000L,
                    searchQueryOrUrl = "xvideos.com",
                    actionTaken = "Bloqueado",
                    isAlert = true,
                    childEmail = "eduardo@gmail.com"
                ),
                ParentLog(
                    timestamp = System.currentTimeMillis() - 7200000L,
                    searchQueryOrUrl = "Como resolver equações lineares 8a classe",
                    actionTaken = "Seguro",
                    isAlert = false,
                    childEmail = "eduardo@gmail.com"
                ),
                ParentLog(
                    timestamp = System.currentTimeMillis() - 1800000L,
                    searchQueryOrUrl = "pesquisa: ver porno gratis",
                    actionTaken = "Bloqueado",
                    isAlert = true,
                    childEmail = "eduardo@gmail.com"
                ),
                ParentLog(
                    timestamp = System.currentTimeMillis() - 600000L,
                    searchQueryOrUrl = "trabalho de história de Moçambique",
                    actionTaken = "Seguro",
                    isAlert = false,
                    childEmail = "eduardo@gmail.com"
                )
            )
            initialLogs.forEach { log -> dao.insertParentLog(log) }
        }
    }

    suspend fun getSobrietyStreak(): SobrietyStreak? = withContext(Dispatchers.IO) {
        dao.getSobrietyStreak()
    }

    suspend fun updateSobrietyStreak(streak: SobrietyStreak) = withContext(Dispatchers.IO) {
        dao.insertSobrietyStreak(streak)
    }

    suspend fun resetStreak(reason: String, trigger: String, intensity: Int) = withContext(Dispatchers.IO) {
        val current = dao.getSobrietyStreak() ?: SobrietyStreak(streakStartDate = System.currentTimeMillis())
        val cleanMillis = System.currentTimeMillis() - current.streakStartDate
        val cleanDays = (cleanMillis / (1000 * 60 * 60 * 24)).toInt()

        val updatedBest = maxOf(current.bestStreakDays, cleanDays)
        val updatedTotalClean = current.totalCleanDays + cleanDays
        val updatedRelapses = current.totalRelapses + 1

        val newStreak = SobrietyStreak(
            id = 1,
            streakStartDate = System.currentTimeMillis(),
            bestStreakDays = updatedBest,
            totalCleanDays = updatedTotalClean,
            totalRelapses = updatedRelapses
        )
        dao.insertSobrietyStreak(newStreak)

        val log = RelapseLog(
            timestamp = System.currentTimeMillis(),
            reason = reason,
            trigger = trigger,
            intensity = intensity
        )
        dao.insertRelapseLog(log)
    }

    suspend fun insertChatMessage(msg: SupportChat) = withContext(Dispatchers.IO) {
        dao.insertChatMessage(msg)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        dao.clearChatMessages()
    }

    suspend fun registerPayment(transactionId: String, phoneNumber: String, plan: String) = withContext(Dispatchers.IO) {
        val slip = PaymentSlip(
            transactionId = transactionId,
            phoneNumber = phoneNumber,
            planSelected = plan,
            dateRegistered = System.currentTimeMillis(),
            status = "Ativo" // Automatically active to provide immediate demo feedback, but maintains real M-Pesa tracking!
        )
        dao.insertPaymentSlip(slip)
    }

    suspend fun isPremiumActive(): Boolean = withContext(Dispatchers.IO) {
        dao.getActivePayment() != null
    }

    suspend fun getBlockerSettings(): BlockerSettings? = withContext(Dispatchers.IO) {
        dao.getBlockerSettings()
    }

    suspend fun updateBlockerSettings(settings: BlockerSettings) = withContext(Dispatchers.IO) {
        dao.insertBlockerSettings(settings)
    }

    suspend fun publishForumPost(author: String, content: String, location: String) = withContext(Dispatchers.IO) {
        val post = ForumPost(
            author = if (author.trim().isEmpty()) "Guerreiro Anónimo" else author.trim(),
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            location = if (location.trim().isEmpty()) "Moçambique" else location.trim(),
            supportsCount = 0
        )
        dao.insertForumPost(post)
    }

    suspend fun supportForumPost(postId: Int) = withContext(Dispatchers.IO) {
        dao.supportPost(postId)
    }

    suspend fun insertParentLog(searchQueryOrUrl: String, actionTaken: String, isAlert: Boolean, childEmail: String = "") = withContext(Dispatchers.IO) {
        val log = ParentLog(
            timestamp = System.currentTimeMillis(),
            searchQueryOrUrl = searchQueryOrUrl,
            actionTaken = actionTaken,
            isAlert = isAlert,
            childEmail = childEmail
        )
        dao.insertParentLog(log)
    }

    suspend fun clearParentLogs() = withContext(Dispatchers.IO) {
        dao.clearParentLogs()
    }
}
