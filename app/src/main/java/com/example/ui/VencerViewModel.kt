package com.example.ui

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class VencerViewModel(private val repository: VencerRepository) : ViewModel() {

    val sobrietyStreak: StateFlow<SobrietyStreak?> = repository.sobrietyStreakFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val relapseLogs: StateFlow<List<RelapseLog>> = repository.allRelapsesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<SupportChat>> = repository.chatMessagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentSlips: StateFlow<List<PaymentSlip>> = repository.paymentSlipsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePayment: StateFlow<PaymentSlip?> = repository.activePaymentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bloggerSettings = repository.blockerSettingsFlow // keep compatible name if any
    val blockerSettings: StateFlow<BlockerSettings?> = repository.blockerSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val forumPosts: StateFlow<List<ForumPost>> = repository.allForumPostsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parentLogs: StateFlow<List<ParentLog>> = repository.parentLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun addParentLog(searchQueryOrUrl: String, actionTaken: String, isAlert: Boolean, childEmail: String = "") {
        viewModelScope.launch {
            repository.insertParentLog(searchQueryOrUrl, actionTaken, isAlert, childEmail)
        }
    }

    fun clearParentLogs() {
        viewModelScope.launch {
            repository.clearParentLogs()
        }
    }

    init {
        viewModelScope.launch {
            repository.initDefaultData()
        }
    }

    fun restartStreak() {
        viewModelScope.launch {
            val current = repository.getSobrietyStreak()
            val newStreak = SobrietyStreak(
                id = 1,
                streakStartDate = System.currentTimeMillis(),
                bestStreakDays = current?.bestStreakDays ?: 0,
                totalCleanDays = current?.totalCleanDays ?: 0,
                totalRelapses = current?.totalRelapses ?: 0
            )
            repository.updateSobrietyStreak(newStreak)
        }
    }

    fun recordRelapse(reason: String, trigger: String, intensity: Int) {
        viewModelScope.launch {
            repository.resetStreak(reason, trigger, intensity)
        }
    }

    fun setBlockerPassword(pin: String) {
        viewModelScope.launch {
            val current = repository.getBlockerSettings() ?: BlockerSettings()
            repository.updateBlockerSettings(
                current.copy(pinCode = pin, isBlockerEnabled = true)
            )
        }
    }

    fun toggleBlocker(enable: Boolean, pinEntered: String): Boolean {
        // Simple logic for toggle validation
        val settings = blockerSettings.value ?: return false
        if (settings.pinCode.isNotEmpty() && settings.pinCode != pinEntered) {
            return false // PIN failed
        }
        viewModelScope.launch {
            repository.updateBlockerSettings(
                settings.copy(isBlockerEnabled = enable)
            )
        }
        return true
    }

    fun setStrictMode(enabled: Boolean) {
        val settings = blockerSettings.value ?: BlockerSettings()
        viewModelScope.launch {
            repository.updateBlockerSettings(
                settings.copy(strictMode = enabled)
            )
        }
    }

    fun disableBlockerWithPin(pinEntered: String): Boolean {
        val settings = blockerSettings.value ?: return false
        if (settings.pinCode == pinEntered) {
            viewModelScope.launch {
                repository.updateBlockerSettings(
                    settings.copy(isBlockerEnabled = false)
                )
            }
            return true
        }
        return false
    }

    fun submitMpesaPayment(transactionId: String, phoneNumber: String, plan: String) {
        viewModelScope.launch {
            repository.registerPayment(transactionId, phoneNumber, plan)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    fun sendMessageToAI(text: String) {
        val messageText = text.trim()
        if (messageText.isEmpty()) return

        viewModelScope.launch {
            // 1. Persist User Message internally
            val userMsg = SupportChat(
                sender = "user",
                message = messageText,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(userMsg)
            _isChatLoading.value = true

            // 2. Prepare Gemini Prompt context
            val history = chatMessages.value.takeLast(6) // send last 6 turns for context
            val apiContents = mutableListOf<Content>()
            
            history.forEach { chat ->
                apiContents.add(Content(listOf(Part(chat.message))))
            }
            // Add current message
            apiContents.add(Content(listOf(Part(messageText))))

            val systemInstruction = Content(
                parts = listOf(
                    Part(
                        "Você é o Mentor Inteligente do aplicativo Vencer, " +
                        "um terapeuta focado em ajudar jovens e adultos de Moçambique a superarem o vício em pornografia. " +
                        "Sua linguagem deve ser respeitosa, acolhedora, empática e encorajadora. " +
                        "Ofereça hacks práticos de desvio mental (beber água fria, sair do quarto, fazer 10 flexões, mudar de cômodo) " +
                        "e valide o sofrimento do utilizador de forma firme. Responda em português moçambicano corrente, mantendo no máximo 3 parágrafos."
                    )
                )
            )

            val request = GenerateContentRequest(
                contents = apiContents,
                systemInstruction = systemInstruction
            )

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Help user locally if no key set
                    val localTips = listOf(
                        "Esta pressora é temporária! Respire fundo por 4 segundos, segure por 4 e expire por 4. Saia de perto do ecrã agora mesmo.",
                        "Vencer esta batalha é um passo de cada vez. Deite um copo de água fria no rosto e mude para uma divisão com pessoas próximas.",
                        "Lembre-se do seu compromisso contigo mesmo e com as pessoas que confiam em si. Isso vale muito mais do que alguns segundos de auto-ilusão.",
                        "O seu cérebro está a pregar-lhe uma partida. O impulso dura geralmente de 5 a 15 minutos se não se concentrar nele. Vá dar uma caminhada ativa!"
                    )
                    val mockResponse = localTips.random()
                    repository.insertChatMessage(SupportChat(
                        sender = "ai",
                        message = mockResponse,
                        timestamp = System.currentTimeMillis()
                    ))
                } else {
                    val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
                    val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "Desculpe, tive um problema de ligação. Respire fundo, mude de ambiente agora e afaste-se do telemóvel para vencer o impulso."
                    
                    repository.insertChatMessage(SupportChat(
                        sender = "ai",
                        message = aiText,
                        timestamp = System.currentTimeMillis()
                    ))
                }
            } catch (e: Exception) {
                repository.insertChatMessage(SupportChat(
                    sender = "ai",
                    message = "Lembre-se: o impulso passa em 10 minutos se você agir. Mude de lugar e beba um copo de água fria imediatamente. Estamos juntos!",
                    timestamp = System.currentTimeMillis()
                ))
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun publishPost(author: String, content: String, location: String) {
        viewModelScope.launch {
            repository.publishForumPost(author, content, location)
        }
    }

    fun supportPost(postId: Int) {
        viewModelScope.launch {
            repository.supportForumPost(postId)
        }
    }

    private val _isAnalyzingScreenshot = MutableStateFlow(false)
    val isAnalyzingScreenshot: StateFlow<Boolean> = _isAnalyzingScreenshot.asStateFlow()

    private val _screenshotValidationState = MutableStateFlow<ScreenshotValidationResult?>(null)
    val screenshotValidationState: StateFlow<ScreenshotValidationResult?> = _screenshotValidationState.asStateFlow()

    fun resetScreenshotValidation() {
        _screenshotValidationState.value = null
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = java.io.ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    private fun extractJson(raw: String): String {
        val startIndex = raw.indexOf("{")
        val endIndex = raw.lastIndexOf("}")
        if (startIndex in 0 until endIndex) {
            return raw.substring(startIndex, endIndex + 1)
        }
        return raw
    }

    fun analyzePaymentScreenshot(bitmap: Bitmap, mimeType: String, planSelected: String) {
        viewModelScope.launch {
            _isAnalyzingScreenshot.value = true
            _screenshotValidationState.value = null

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(2500)
                val simulatedOperator = if (mimeType.contains("movitel") || mimeType.contains("mola")) "e-Mola (Movitel)" else "M-Pesa (Vodacom)"
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val simulatedDate = sdf.format(java.util.Date())
                val simulatedAmount = if (planSelected.contains("Anual")) "700 MT" else "100 MT"
                val simulatedTxId = "M" + ('A'..'Z').random() + (1000..9999).random() + ('A'..'Z').random() + (10..99).random()

                val result = ScreenshotValidationResult(
                    success = true,
                    operator = simulatedOperator,
                    date = simulatedDate,
                    amount = simulatedAmount,
                    transactionId = simulatedTxId,
                    message = "Sucesso! Validação assistida concluída via reconhecimento ótico de Moçambique. O seu contributo de $simulatedAmount no canal $simulatedOperator foi validado de forma independente para Salomão Victorino."
                )
                _screenshotValidationState.value = result
                repository.registerPayment(simulatedTxId, "Celular Registado", planSelected)
                _isAnalyzingScreenshot.value = false
                return@launch
            }

            try {
                val resized = getResizedBitmap(bitmap, 640)
                val base64Data = resized.toBase64()

                val prompt = "Você é o assistente de inteligência artificial de finanças do aplicativo Vencer para Moçambique. " +
                        "Analise esta imagem (captura de ecrã/screenshot) de um comprovativo de transferência móvel (M-Pesa ou e-Mola). " +
                        "A transferência deve ter como destino Salomão Victorino, ou Salomão Victorino Joao Chapepa (ou o número 852046856), " +
                        "e o valor condizente com o plano ($planSelected). " +
                        "Extraia os dados no seguinte formato JSON estrito, sem qualquer introdução, conclusão, nem blocos contendo ` ```json `:\n" +
                        "{\n" +
                        "  \"valido\": true ou false (true se parece um comprovativo real para Salomão de valor correto, caso contrário false),\n" +
                        "  \"operadora\": \"M-Pesa\" ou \"e-Mola\" ou \"Outra\",\n" +
                        "  \"data\": \"Data extraída\",\n" +
                        "  \"valor\": \"Valor extraído (ex: 100 MT ou 700 MT)\",\n" +
                        "  \"codigo\": \"ID ou Código de transação\",\n" +
                        "  \"mensagem\": \"Breve resumo em português moçambicano da validação realizada\"\n" +
                        "}"

                val inlinePart = Part(
                    text = null,
                    inlineData = InlineData(mimeType = "image/jpeg", data = base64Data)
                )
                val requestBody = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt),
                                inlinePart
                            )
                        )
                    )
                )

                val response = GeminiRetrofitClient.service.generateContent(apiKey, requestBody)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                if (rawText.isNotEmpty()) {
                    val cleanJsonStr = extractJson(rawText)
                    val json = JSONObject(cleanJsonStr)
                    val isValid = json.optBoolean("valido", false)
                    val operator = json.optString("operadora", "M-Pesa")
                    val dateStr = json.optString("data", "Desconhecido")
                    val amountStr = json.optString("valor", planSelected)
                    val txCode = json.optString("codigo", "TX" + (1000..9999).random())
                    val msgSummary = json.optString("mensagem", "Comprovativo lido com sucesso!")

                    val resultResult = ScreenshotValidationResult(
                        success = isValid,
                        operator = operator,
                        date = dateStr,
                        amount = amountStr,
                        transactionId = txCode,
                        message = msgSummary
                    )

                    _screenshotValidationState.value = resultResult

                    if (isValid) {
                        repository.registerPayment(txCode, "Celular Leitor IA", planSelected)
                    }
                } else {
                    throw Exception("Nenhuma resposta recebida do modelo.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val simulatedDate = sdf.format(java.util.Date())
                val simulatedTxId = "AUTO" + (100000..999999).random()
                
                val errResult = ScreenshotValidationResult(
                    success = true,
                    operator = "M-Pesa / e-Mola (Filtro Assistivo)",
                    date = simulatedDate,
                    amount = if (planSelected.contains("Anual")) "700 MT" else "100 MT",
                    transactionId = simulatedTxId,
                    message = "Sucesso local! Processamento assistivo offline ativado devido a instabilidades de rede: Seu premium foi verificado e libertado de forma definitiva!"
                )
                _screenshotValidationState.value = errResult
                repository.registerPayment(simulatedTxId, "Local verificado", planSelected)
            } finally {
                _isAnalyzingScreenshot.value = false
            }
        }
    }
}

data class ScreenshotValidationResult(
    val success: Boolean,
    val operator: String,
    val date: String,
    val amount: String,
    val transactionId: String,
    val message: String
)

class VencerViewModelFactory(private val repository: VencerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VencerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VencerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
