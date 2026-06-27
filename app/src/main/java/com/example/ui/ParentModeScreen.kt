package com.example.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ParentLog
import com.example.ui.theme.CardBackground
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentModeScreen(
    viewModel: VencerViewModel,
    onBack: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val context = LocalContext.current
    val blockerSettings by viewModel.blockerSettings.collectAsState()
    val parentLogs by viewModel.parentLogs.collectAsState()
    val isPasscodeCorrect by viewModel.isPasscodeCorrect.collectAsState()

    // Premium States
    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsState()
    val isTrialExpired by viewModel.isTrialExpired.collectAsState()
    val mpesaNumber by viewModel.mpesaNumber.collectAsState()
    val mpesaName by viewModel.mpesaName.collectAsState()

    var showPremiumDialog by remember { mutableStateOf(false) }
    var passcodeField by remember { mutableStateOf("") }
    var emailField by remember { 
        mutableStateOf(
            context.getSharedPreferences("vencer_prefs", Context.MODE_PRIVATE)
                .getString("parent_child_email", "eduardo@gmail.com") ?: "eduardo@gmail.com"
        )
    }

    var showEmailSavedMessage by remember { mutableStateOf(false) }
    
    // Premium editing states
    var editMpesaNumber by remember(mpesaNumber) { mutableStateOf(mpesaNumber) }
    var editMpesaName by remember(mpesaName) { mutableStateOf(mpesaName) }
    var showPaymentSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetPasscodeVerification()
    }

    val isPasscodeConfigured = blockerSettings != null && blockerSettings?.parentPasscode?.isNotEmpty() == true

    if (blockerSettings == null) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    } else if (!isPasscodeConfigured) {
        // PASSCODE CREATION FLOW
        var newPasscode by remember { mutableStateOf("") }
        var confirmPasscode by remember { mutableStateOf("") }
        var passcodeError by remember { mutableStateOf<String?>(null) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Criar Senha do Guardião",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Para impedir que as restrições e o bloqueador sejam desativados, crie um código PIN exclusivo de 4 a 6 dígitos.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPasscode,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            newPasscode = it
                            passcodeError = null
                        }
                    },
                    label = { Text("Novo PIN (4-6 dígitos)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            confirmPasscode = it
                            passcodeError = null
                        }
                    },
                    label = { Text("Confirmar PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                if (passcodeError != null) {
                    Text(
                        text = passcodeError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newPasscode.length < 4) {
                            passcodeError = "O PIN deve ter no mínimo 4 dígitos."
                        } else if (newPasscode != confirmPasscode) {
                            passcodeError = "Os PINs não coincidem!"
                        } else {
                            viewModel.savePasscode(newPasscode)
                            Toast.makeText(context, "Código criado com sucesso! Use o novo código para aceder.", Toast.LENGTH_LONG).show()
                            newPasscode = ""
                            confirmPasscode = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Criar e Ativar Código", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Voltar ao Painel")
                }
            }
        }
    } else if (isPasscodeCorrect != true) {
        // PASSCODE LOCK SCREEN
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Acesso Restrito ao Guardião",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Insira o código PIN de 4 dígitos para gerenciar as configurações do filtro de pureza.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = passcodeField,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 6) {
                            passcodeField = it
                        }
                    },
                    label = { Text("Código PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                if (isPasscodeCorrect == false) {
                    Text(
                        text = "PIN Incorreto! Tente novamente.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        viewModel.verifyPasscode(passcodeField)
                    },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Confirmar PIN")
                }

                TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Voltar ao Painel")
                }
            }
        }
    } else {
        // MAIN PARENT SETTINGS SCREEN
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Painel do Guardião", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Blocker Config
                item {
                    Text(
                        text = "Configurações do Bloqueador",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!isPremiumUnlocked) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Recurso Bloqueado no Teste Gratuito",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "O Guardião (Filtro ativo de pornografia) é um recurso exclusivo do Vencer Premium. Durante o teste gratuito de 2 dias você não pode ativá-lo. Pague a licença de 150 MT por 30 dias para poder ativá-lo.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showPremiumDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pagar Licença Pro (150 MT)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val isServiceEnabled = isAccessibilityServiceEnabled(context)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Filtro Ativo (Serviço de Acessibilidade)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isServiceEnabled) "Serviço Ligado e Ativo" else "Serviço Desligado nas definições do sistema",
                                        fontSize = 11.sp,
                                        color = if (isServiceEnabled) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                                Switch(
                                    checked = isAccessibilityServiceEnabled(context) && isPremiumUnlocked,
                                    onCheckedChange = {
                                        if (!isPremiumUnlocked) {
                                            showPremiumDialog = true
                                        } else {
                                            // Open Android accessibility settings
                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }

                            if (!isServiceEnabled) {
                                Button(
                                    onClick = {
                                        if (!isPremiumUnlocked) {
                                            showPremiumDialog = true
                                        } else {
                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Ligar nas Configurações do Celular", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bloqueio de Conteúdo Guardião",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Intercetar termos e sites de teor adulto.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = (blockerSettings?.isBlockerEnabled ?: false) && isPremiumUnlocked,
                                    onCheckedChange = {
                                        if (!isPremiumUnlocked) {
                                            showPremiumDialog = true
                                        } else {
                                            viewModel.updateBlockerEnabled(it)
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo Estrito (Modo Extremo)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Filtro redobrado para redes sociais e palavras suspeitas.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }
                                Switch(
                                    checked = (blockerSettings?.strictMode ?: false) && isPremiumUnlocked,
                                    onCheckedChange = {
                                        if (!isPremiumUnlocked) {
                                            showPremiumDialog = true
                                        } else {
                                            viewModel.updateStrictMode(it)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Section 2: Email alert recipient
                item {
                    Text(
                        text = "Configuração do Email de Relatório",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Email do Guardião para Alertas",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "As notificações de bloqueios e recaídas serão registadas e encaminhadas para este email.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = emailField,
                                onValueChange = { emailField = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    context.getSharedPreferences("vencer_prefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("parent_child_email", emailField)
                                        .apply()
                                    showEmailSavedMessage = true
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Guardar Email")
                            }

                            if (showEmailSavedMessage) {
                                Text(
                                    text = "Email do Guardião guardado com sucesso!",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Section: Premium & Subscription Management
                item {
                    Text(
                        text = "Gestão de Plano e Assinatura",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estado da Assinatura",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPremiumUnlocked) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isPremiumUnlocked) "PREMIUM ATIVO" else if (isTrialExpired) "TESTE EXPIRADO" else "PERÍODO DE TESTE (2 DIAS)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPremiumUnlocked) Color(0xFF065F46) else Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Configuração de Conta M-Pesa de Destino",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Abaixo, defina a conta M-Pesa que receberá as subscrições dos usuários.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editMpesaNumber,
                                onValueChange = { editMpesaNumber = it },
                                label = { Text("Número M-Pesa de Recebimento", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editMpesaName,
                                onValueChange = { editMpesaName = it },
                                label = { Text("Nome do Titular M-Pesa", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateMpesaDetails(editMpesaNumber, editMpesaName)
                                        showPaymentSavedMessage = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Guardar M-Pesa", fontSize = 11.sp, color = Color.Black)
                                }
                            }

                            if (showPaymentSavedMessage) {
                                Text(
                                    text = "Conta de recebimento M-Pesa guardada!",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                            Text(
                                text = "Ações Rápidas de Simulação (Desenvolvedor/Suporte)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.forceTrialExpiration()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    Text("Forçar Expiração", fontSize = 10.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        viewModel.resetTrial()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    Text("Reiniciar Teste", fontSize = 10.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        viewModel.unlockPremium()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    Text("Ativar Premium", fontSize = 10.sp, maxLines = 1, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Section 3: Monitoring Logs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Histórico de Intercetações",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(onClick = { viewModel.clearLogs() }) {
                            Text("Limpar Logs", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (parentLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Nenhuma intercetação registada.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "O dispositivo está limpo e seguro.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(parentLogs) { log ->
                        LogItemCard(log)
                    }
                }
            }
        }
    }

    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ativar Vencer Premium", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "O Guardião é um recurso avançado que interceta sites e termos de teor adulto/pornográfico para manter a sua mente limpa.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Esta funcionalidade requer a licença ativa do Vencer Premium por apenas 150 MT (válida por 30 dias).",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instruções de pagamento:\n1. Envie 150 MT via M-Pesa para o número:\n   -> $mpesaNumber ($mpesaName)\n2. Tire um print screen do comprovante\n3. Envie o comprovante na tela do painel do usuário.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPremiumDialog = false
                        onBack()
                        onUpgradeClick()
                    }
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pagar Licença Pro", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text("Depois")
                }
            }
        )
    }
}

@Composable
fun LogItemCard(log: ParentLog) {
    val date = Date(log.timestamp)
    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = format.format(date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.actionTaken,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.searchQueryOrUrl,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Destinatário do Alerta: ${log.childEmail}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = "${context.packageName}/com.example.blocker.PurityBoundaryService"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(expectedComponentName) || enabledServices.contains("PurityBoundaryService")
}
