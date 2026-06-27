package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardBackground
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    viewModel: VencerViewModel,
    onBypass: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val mpesaNumber by viewModel.mpesaNumber.collectAsState()
    val mpesaName by viewModel.mpesaName.collectAsState()
    val isVerifying by viewModel.isVerifyingScreenshot.collectAsState()
    val rawResult by viewModel.screenshotResult.collectAsState()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var demoReceiptType by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    // Parse verification result
    var isValidPayment by remember { mutableStateOf(false) }
    var extractedTxId by remember { mutableStateOf("") }
    var extractedDateTime by remember { mutableStateOf("") }
    var extractedOperator by remember { mutableStateOf("") }
    var extractedAmount by remember { mutableStateOf("") }
    var extractedBeneficiary by remember { mutableStateOf("") }
    var failureReason by remember { mutableStateOf("") }

    LaunchedEffect(rawResult) {
        rawResult?.let { raw ->
            try {
                val cleaned = cleanJsonString(raw)
                val json = JSONObject(cleaned)
                isValidPayment = json.optBoolean("isValid", false)
                extractedTxId = json.optString("transactionId", "Desconhecido")
                extractedDateTime = json.optString("dateTime", "Desconhecida")
                extractedOperator = json.optString("operator", "Desconhecida")
                extractedAmount = json.optString("amount", "Desconhecido")
                extractedBeneficiary = json.optString("beneficiary", "Desconhecido")
                failureReason = json.optString("reason", "Erro na validação do comprovativo.")
            } catch (e: Exception) {
                e.printStackTrace()
                isValidPayment = false
                failureReason = "Erro ao ler resposta da IA de visão: O formato retornado não pôde ser lido corretamente."
            }
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            demoReceiptType = null
            selectedBitmap = getBitmapFromUri(context, uri)
            viewModel.clearScreenshotResult()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (rawResult != null && isValidPayment) {
            // Success view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(96.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PREMIUM ATIVADO!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dados do Pagamento Verificados via IA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        RowValue("Código / Ref:", extractedTxId)
                        RowValue("Operadora:", extractedOperator)
                        RowValue("Valor:", extractedAmount)
                        RowValue("Beneficiário:", extractedBeneficiary)
                        RowValue("Data/Hora:", extractedDateTime)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Obrigado pelo seu pagamento. O Vencer Premium está agora totalmente ativo! Use o bloqueador inteligente, consulte o conselheiro IA sem limites e continue a sua jornada de sobriedade pura.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.unlockPremium()
                        onBypass()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Começar a usar o Vencer Premium", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        } else {
            // Paywall screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
                    .safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onClose != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Premium Tag
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLANO PREMIUM VENCER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                Text(
                    text = "PERÍODO DE TESTE ESGOTADO",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "O seu teste de 2 dias terminou. Para continuar a proteger a sua mente e focar no seu crescimento, efetue a ativação única de apenas 150 MT via M-Pesa ou e-Mola.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Instruction cards for Vodacom M-Pesa / Tmcel e-Mola
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // M-Pesa Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "M-Pesa (Vodacom)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("852046856", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Salomão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    copyToClipboard(context, "852046856", "Número M-Pesa")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48).copy(alpha = 0.2f)),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFE11D48))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar", fontSize = 10.sp, color = Color(0xFFE11D48))
                            }
                        }
                    }

                    // e-Mola Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "e-Mola (Tmcel)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("852046856", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Salomão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    copyToClipboard(context, "852046856", "Número e-Mola")
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFF59E0B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar", fontSize = 10.sp, color = Color(0xFFF59E0B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Verification and Simulation Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Validador Automático com IA de Visão",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Carregue o comprovativo de transferência real. A nossa inteligência artificial analisará visualmente os dados da transação para ativar o premium imediatamente.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedBitmap != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "Comprovativo carregado",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Visual scanning laser line animation when verifying
                                    if (isVerifying) {
                                        val infiniteTransition = rememberInfiniteTransition(label = "scan")
                                        val yOffset by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 220f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(2000, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "yOffset"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .offset(y = yOffset.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.5f))
                                                    )
                                                )
                                        )
                                    }

                                    // Display badge indicating type
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = if (demoReceiptType != null) "Recibo Virtual ($demoReceiptType)" else "Foto da Galeria",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Sem imagem carregada",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        "Faça um envio da galeria ou use os geradores de teste abaixo.",
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Submit & Pick Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch("image/*")
                                },
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder.copy()
                            ) {
                                Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Carregar Comprovativo", fontSize = 11.sp, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    selectedBitmap?.let {
                                        viewModel.verifyPaymentWithVision(it)
                                    }
                                },
                                enabled = selectedBitmap != null && !isVerifying,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isVerifying) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analisando...", fontSize = 11.sp, color = Color.Black)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Validar IA", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Display validation failure warning if invalid and result returned
                        if (rawResult != null && !isValidPayment) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Comprovativo Recusado",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            failureReason,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulator Section
                        Text(
                            text = "Simulador Rápido (Validação Real com Recibos Virtuais)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Como o emulador não possui galeria de fotos reais com comprovativos, criamos este gerador de recibos digitais válidos/inválidos para testar o sistema de visão OCR da IA diretamente.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    demoReceiptType = "M-Pesa"
                                    viewModel.clearScreenshotResult()
                                    val nowStr = SimpleDateFormat("dd/MM/2026 HH:mm", Locale.getDefault()).format(Date())
                                    selectedBitmap = generateReceiptBitmap(
                                        context = context,
                                        operator = "M-Pesa",
                                        targetNumber = mpesaNumber,
                                        targetName = mpesaName,
                                        amount = "150.00 MT",
                                        refCode = "TXN" + (100000..999999).random().toString(),
                                        dateTime = nowStr
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("M-Pesa Válido", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    demoReceiptType = "e-Mola"
                                    viewModel.clearScreenshotResult()
                                    val nowStr = SimpleDateFormat("dd/MM/2026 HH:mm", Locale.getDefault()).format(Date())
                                    selectedBitmap = generateReceiptBitmap(
                                        context = context,
                                        operator = "e-Mola",
                                        targetNumber = mpesaNumber,
                                        targetName = mpesaName,
                                        amount = "150.00 MT",
                                        refCode = "EML" + (100000..999999).random().toString(),
                                        dateTime = nowStr
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("e-Mola Válido", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    demoReceiptType = "Inválido"
                                    viewModel.clearScreenshotResult()
                                    val nowStr = SimpleDateFormat("dd/MM/2026 HH:mm", Locale.getDefault()).format(Date())
                                    selectedBitmap = generateReceiptBitmap(
                                        context = context,
                                        operator = "Fake",
                                        targetNumber = "821111111",
                                        targetName = "Zeca Pagodinho",
                                        amount = "50.00 MT",
                                        refCode = "BAD-999",
                                        dateTime = nowStr
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text("Fake / Errado", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Traditional manual validation fallback
                TextButton(
                    onClick = {
                        viewModel.unlockPremium()
                        onBypass()
                        Toast.makeText(context, "Ativado em Modo de Demonstração!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = "Simular Ativação Manual (Bypass de Teste / Demo)",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun RowValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado!", Toast.LENGTH_SHORT).show()
}

fun cleanJsonString(raw: String): String {
    var cleaned = raw.trim()
    if (cleaned.startsWith("```json")) {
        cleaned = cleaned.substring(7)
    } else if (cleaned.startsWith("```")) {
        cleaned = cleaned.substring(3)
    }
    if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length - 3)
    }
    return cleaned.trim()
}

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun generateReceiptBitmap(
    context: Context,
    operator: String,
    targetNumber: String,
    targetName: String,
    amount: String = "150.00 MT",
    refCode: String,
    dateTime: String
): Bitmap {
    val width = 450
    val height = 650
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    // 1. Draw Background
    if (operator == "M-Pesa") {
        canvas.drawColor(android.graphics.Color.parseColor("#F3F4F6"))
        paint.color = android.graphics.Color.parseColor("#E11D48")
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = spToPx(context, 16f)
        paint.isFakeBoldText = true
        canvas.drawText("Vodacom M-Pesa Moçambique", 30f, 55f, paint)
    } else if (operator == "e-Mola") {
        canvas.drawColor(android.graphics.Color.parseColor("#FFFBEB"))
        paint.color = android.graphics.Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, paint)
        paint.color = android.graphics.Color.BLACK
        paint.textSize = spToPx(context, 16f)
        paint.isFakeBoldText = true
        canvas.drawText("Tmcel e-Mola Moçambique", 30f, 55f, paint)
    } else {
        canvas.drawColor(android.graphics.Color.parseColor("#F9FAFB"))
        paint.color = android.graphics.Color.parseColor("#4B5563")
        canvas.drawRect(0f, 0f, width.toFloat(), 90f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = spToPx(context, 16f)
        paint.isFakeBoldText = true
        canvas.drawText("Recibo de Transferência", 30f, 55f, paint)
    }

    // 2. Draw Card Body
    paint.color = android.graphics.Color.WHITE
    paint.isFakeBoldText = false
    canvas.drawRect(20f, 110f, (width - 20).toFloat(), (height - 20).toFloat(), paint)

    paint.color = if (operator == "Fake") android.graphics.Color.RED else android.graphics.Color.parseColor("#10B981")
    canvas.drawCircle((width / 2).toFloat(), 170f, 35f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = spToPx(context, 24f)
    paint.isFakeBoldText = true
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText(if (operator == "Fake") "X" else "✓", (width / 2).toFloat(), 180f, paint)

    paint.color = android.graphics.Color.BLACK
    paint.textSize = spToPx(context, 14f)
    paint.isFakeBoldText = true
    canvas.drawText(if (operator == "Fake") "PAGAMENTO FALHOU" else "TRANSFERÊNCIA REALIZADA", (width / 2).toFloat(), 245f, paint)

    paint.textAlign = android.graphics.Paint.Align.LEFT
    paint.isFakeBoldText = false
    paint.textSize = spToPx(context, 12f)

    var yPos = 310f
    val lineSpacing = 42f

    fun drawRow(label: String, valText: String) {
        paint.color = android.graphics.Color.GRAY
        canvas.drawText(label, 40f, yPos, paint)
        paint.color = android.graphics.Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText(valText, 200f, yPos, paint)
        paint.isFakeBoldText = false
        yPos += lineSpacing
    }

    if (operator == "M-Pesa") {
        drawRow("Serviço:", "M-Pesa Enviar")
        drawRow("Ref / ID:", refCode)
        drawRow("Celular Destino:", targetNumber)
        drawRow("Beneficiário:", targetName)
        drawRow("Valor Pago:", amount)
        drawRow("Data/Hora:", dateTime)
        drawRow("Estado:", "Sucesso")
    } else if (operator == "e-Mola") {
        drawRow("Serviço:", "e-Mola Enviar")
        drawRow("Ref / ID:", refCode)
        drawRow("Celular Destino:", targetNumber)
        drawRow("Titular:", targetName)
        drawRow("Valor Pago:", amount)
        drawRow("Data/Hora:", dateTime)
        drawRow("Estado:", "Sucesso")
    } else {
        drawRow("Serviço:", "Outra Carteira")
        drawRow("Ref / ID:", "BAD-REF-999")
        drawRow("Celular Destino:", "821111111")
        drawRow("Titular:", "Zeca Pagodinho")
        drawRow("Valor Pago:", "50.00 MT")
        drawRow("Data/Hora:", dateTime)
        drawRow("Estado:", "Inválido")
    }

    paint.color = android.graphics.Color.LTGRAY
    paint.textSize = spToPx(context, 9f)
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("Comprovativo Digital Oficial de Moçambique", (width / 2).toFloat(), (height - 40).toFloat(), paint)

    return bitmap
}

private fun spToPx(context: Context, sp: Float): Float {
    return sp * context.resources.displayMetrics.scaledDensity
}
