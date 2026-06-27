package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.example.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    suspend fun getCounselorResponse(chatHistory: List<ChatMessage>): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Erro: Chave de API do Gemini não configurada nos Segredos do AI Studio."
        }

        // Map ChatMessage objects to Gemini Content objects
        // System instruction sets the role-play behavior
        val systemInstruction = Content(
            parts = listOf(Part(text = "Você é o 'Conselheiro Vencer', um assistente compassivo, empático, profissional e motivador especializado em ajudar jovens e adultos em Moçambique a vencer a dependência em pornografia e focar no crescimento pessoal. Use termos adequados, encorajadores, evite julgamentos e dê conselhos práticos de sobriedade e mindfulness."))
        )

        val contents = chatHistory.map { msg ->
            Content(
                parts = listOf(Part(text = msg.text))
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = systemInstruction
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Desculpe, não consegui processar uma resposta neste momento."
        } catch (e: Exception) {
            e.printStackTrace()
            "Erro de conexão: Certifique-se de que está ligado à Internet. Detalhes: ${e.localizedMessage}"
        }
    }

    suspend fun verifyPaymentScreenshot(bitmap: Bitmap, targetNumber: String, targetName: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return """{"isValid":false,"reason":"Erro: Chave de API do Gemini não configurada nos Segredos do AI Studio."}"""
        }

        val base64Image = bitmap.toBase64()

        val prompt = """
            Você é o validador automático do aplicativo Vencer Premium em Moçambique.
            Analise cuidadosamente o comprovativo de pagamento (screenshot/imagem) anexo.
            
            O objetivo é verificar se o usuário fez um pagamento real para liberar o aplicativo.
            Critérios obrigatórios:
            1. O número de celular de destino deve ser o número de recebimento cadastrado: $targetNumber OU o nome do beneficiário deve conter "$targetName" (ou variações como Salomão Vitorino Chapepa).
            2. A operadora de envio deve ser reconhecida: M-Pesa (Vodacom) ou e-Mola (mcel/Tmcel).
            3. O valor pago deve ser de 150 MT (ou 150 meticais).
            4. O comprovativo deve ser recente (por exemplo, data do ano de 2026, ou que pareça um comprovativo real e atual).
            
            Extraia o máximo de informações possíveis, incluindo:
            - Código de transação (Transaction ID / Ref)
            - Data e Hora de pagamento
            - Operadora (M-Pesa ou e-Mola)
            - Valor (Amount)
            - Beneficiário (Beneficiary)
            
            Retorne EXCLUSIVAMENTE um objeto JSON válido, sem aspas triplas de markdown (como ```json) ou qualquer outro texto explicativo fora do JSON. A estrutura DEVE ser exatamente esta:
            {
              "isValid": true ou false,
              "transactionId": "Código ou Ref extraído",
              "dateTime": "Data e Hora extraída",
              "operator": "M-Pesa" ou "e-Mola" ou "Desconhecido",
              "amount": "Valor extraído",
              "beneficiary": "Beneficiário extraído",
              "reason": "Sua justificativa da decisão (se é válido ou por que foi rejeitado)"
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(temperature = 0.2f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "{\"isValid\":false,\"reason\":\"Nenhuma resposta do modelo.\"}"
        } catch (e: Exception) {
            e.printStackTrace()
            "{\"isValid\":false,\"reason\":\"Erro na API do Gemini: ${e.localizedMessage}\"}"
        }
    }
}

fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
