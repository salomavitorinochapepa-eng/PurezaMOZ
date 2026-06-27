package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VencerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VencerRepository(application)

    // Blocker Settings Flow
    val blockerSettings: StateFlow<BlockerSettings?> = repository.getBlockerSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sobriety Log Flow
    val sobrietyLog: StateFlow<SobrietyLog?> = repository.getSobrietyLogFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Parent Logs Flow
    val parentLogs: StateFlow<List<ParentLog>> = repository.getAllParentLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat Messages Flow
    val chatMessages: StateFlow<List<ChatMessage>> = repository.getAllChatMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state for Counselor chat input/loading
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Passcode verification state
    private val _isPasscodeCorrect = MutableStateFlow<Boolean?>(null)
    val isPasscodeCorrect: StateFlow<Boolean?> = _isPasscodeCorrect.asStateFlow()

    // SharedPreferences for trial and premium tracking
    private val prefs = application.getSharedPreferences("vencer_prefs", android.content.Context.MODE_PRIVATE)

    private val _isPremiumUnlocked = MutableStateFlow(prefs.getBoolean("is_premium_unlocked", false))
    val isPremiumUnlocked: StateFlow<Boolean> = _isPremiumUnlocked.asStateFlow()

    private val _isTrialExpired = MutableStateFlow(false)
    val isTrialExpired: StateFlow<Boolean> = _isTrialExpired.asStateFlow()

    // Configuration for payment details
    private val _mpesaNumber = MutableStateFlow(prefs.getString("mpesa_number", "852046856") ?: "852046856")
    val mpesaNumber: StateFlow<String> = _mpesaNumber.asStateFlow()

    private val _mpesaName = MutableStateFlow(prefs.getString("mpesa_name", "Salomão") ?: "Salomão")
    val mpesaName: StateFlow<String> = _mpesaName.asStateFlow()

    private val _isVerifyingScreenshot = MutableStateFlow(false)
    val isVerifyingScreenshot: StateFlow<Boolean> = _isVerifyingScreenshot.asStateFlow()

    private val _screenshotResult = MutableStateFlow<String?>(null)
    val screenshotResult: StateFlow<String?> = _screenshotResult.asStateFlow()

    init {
        // Register first launch time if not exists
        if (!prefs.contains("first_launch_time")) {
            prefs.edit().putLong("first_launch_time", System.currentTimeMillis()).apply()
        }
        
        // Initialize settings if they don't exist
        viewModelScope.launch {
            repository.getBlockerSettings()
            repository.getSobrietyLog()
            checkTrialStatus()
        }
    }

    fun checkTrialStatus() {
        val firstLaunch = prefs.getLong("first_launch_time", System.currentTimeMillis())
        val trialDurationMs = 2L * 24 * 60 * 60 * 1000 // 2 days in milliseconds
        val expired = (System.currentTimeMillis() - firstLaunch) > trialDurationMs
        _isTrialExpired.value = expired && !_isPremiumUnlocked.value
    }

    fun unlockPremium() {
        prefs.edit().putBoolean("is_premium_unlocked", true).apply()
        _isPremiumUnlocked.value = true
        _isTrialExpired.value = false
    }

    fun lockPremium() {
        prefs.edit().putBoolean("is_premium_unlocked", false).apply()
        _isPremiumUnlocked.value = false
        checkTrialStatus()
    }

    fun resetTrial() {
        prefs.edit().putLong("first_launch_time", System.currentTimeMillis()).apply()
        prefs.edit().putBoolean("is_premium_unlocked", false).apply()
        _isPremiumUnlocked.value = false
        _isTrialExpired.value = false
    }

    fun forceTrialExpiration() {
        // Simulate trial expiration by setting first launch time to 3 days ago
        val threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000)
        prefs.edit().putLong("first_launch_time", threeDaysAgo).apply()
        prefs.edit().putBoolean("is_premium_unlocked", false).apply()
        _isPremiumUnlocked.value = false
        _isTrialExpired.value = true
    }

    fun updateMpesaDetails(number: String, name: String) {
        prefs.edit().putString("mpesa_number", number).putString("mpesa_name", name).apply()
        _mpesaNumber.value = number
        _mpesaName.value = name
    }

    fun startSobriety() {
        viewModelScope.launch {
            val current = repository.getSobrietyLog()
            repository.updateSobrietyLog(
                current.copy(
                    startDateTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun recordRelapse() {
        viewModelScope.launch {
            val current = repository.getSobrietyLog()
            repository.updateSobrietyLog(
                current.copy(
                    totalRelapses = current.totalRelapses + 1,
                    lastRelapseTimestamp = System.currentTimeMillis(),
                    startDateTimestamp = System.currentTimeMillis() // Reset streak
                )
            )
            // Also log relapse as a parent log alert
            repository.insertParentLog(
                ParentLog(
                    timestamp = System.currentTimeMillis(),
                    searchQueryOrUrl = "Recaída registada pelo usuário (Reinício de sobriedade)",
                    actionTaken = "Alerta Enviado",
                    isAlert = true,
                    childEmail = getStoredChildEmail()
                )
            )
        }
    }

    fun updateBlockerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getBlockerSettings()
            repository.updateBlockerSettings(current.copy(isBlockerEnabled = enabled))
        }
    }

    fun updateStrictMode(strict: Boolean) {
        viewModelScope.launch {
            val current = repository.getBlockerSettings()
            repository.updateBlockerSettings(current.copy(strictMode = strict))
        }
    }

    fun savePasscode(passcode: String) {
        viewModelScope.launch {
            val current = repository.getBlockerSettings()
            repository.updateBlockerSettings(current.copy(parentPasscode = passcode))
        }
    }

    fun verifyPasscode(input: String) {
        viewModelScope.launch {
            val current = repository.getBlockerSettings()
            val saved = current?.parentPasscode ?: ""
            _isPasscodeCorrect.value = if (saved.isEmpty()) {
                false
            } else {
                input == saved
            }
        }
    }

    fun resetPasscodeVerification() {
        _isPasscodeCorrect.value = null
    }

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            // 1. Add user message to DB
            val userMsg = ChatMessage(text = text, isUser = true)
            repository.insertChatMessage(userMsg)

            _isChatLoading.value = true

            // 2. Fetch full history to pass to Gemini
            val history = repository.getAllChatMessagesFlow().first()

            // 3. Get AI response
            val aiResponseText = repository.getAICounselorResponse(history)

            // 4. Add AI response to DB
            val aiMsg = ChatMessage(text = aiResponseText, isUser = false)
            repository.insertChatMessage(aiMsg)

            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatMessages()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearParentLogs()
        }
    }

    private fun getStoredChildEmail(): String {
        return try {
            val prefs = getApplication<Application>().getSharedPreferences("vencer_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getString("parent_child_email", "eduardo@gmail.com") ?: "eduardo@gmail.com"
        } catch (e: Exception) {
            "eduardo@gmail.com"
        }
    }

    fun verifyPaymentWithVision(bitmap: android.graphics.Bitmap) {
        _isVerifyingScreenshot.value = true
        _screenshotResult.value = null
        viewModelScope.launch {
            try {
                val result = com.example.data.GeminiClient.verifyPaymentScreenshot(
                    bitmap = bitmap,
                    targetNumber = _mpesaNumber.value,
                    targetName = _mpesaName.value
                )
                _screenshotResult.value = result
                
                try {
                    val json = org.json.JSONObject(result)
                    if (json.optBoolean("isValid", false)) {
                        unlockPremium()
                    }
                } catch (jsonEx: Exception) {
                    jsonEx.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _screenshotResult.value = """{"isValid":false,"reason":"Erro ao processar imagem: ${e.localizedMessage}"}"""
            } finally {
                _isVerifyingScreenshot.value = false
            }
        }
    }

    fun clearScreenshotResult() {
        _screenshotResult.value = null
    }
}
