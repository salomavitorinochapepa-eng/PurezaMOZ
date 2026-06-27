package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.theme.VencerTheme
import com.example.ui.theme.CardBackground
import com.example.ui.theme.SoftAlert
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: VencerViewModel by viewModels()

    // Flag to trigger the immediate intercept overlay
    private var showBlockOverlayState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if we were launched by the Accessibility Service Blocker
        checkBlockTrigger(intent)

        setContent {
            VencerTheme {
                var currentRole by remember { mutableStateOf<String?>(null) }
                var currentTab by remember { mutableStateOf(0) } // 0: Tracker, 1: Counselor, 2: Resources
                val sobrietyLog by viewModel.sobrietyLog.collectAsState()
                val isTrialExpired by viewModel.isTrialExpired.collectAsState()
                var showEarlyPaywall by remember { mutableStateOf(false) }
                
                var showBlockOverlay by showBlockOverlayState

                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentRole == null) {
                        RoleSelectionScreen(
                            onRoleSelected = { role ->
                                currentRole = role
                            }
                        )
                    } else if (currentRole == "parent") {
                        ParentModeScreen(
                            viewModel = viewModel,
                            onBack = {
                                currentRole = null
                            },
                            onUpgradeClick = {
                                showEarlyPaywall = true
                            }
                        )
                    } else {
                        // USER PORTAL
                        if (isTrialExpired) {
                            PremiumPaywallScreen(
                                viewModel = viewModel,
                                onBypass = {
                                    viewModel.checkTrialStatus()
                                }
                            )
                        } else if (showEarlyPaywall) {
                            PremiumPaywallScreen(
                                viewModel = viewModel,
                                onBypass = {
                                    showEarlyPaywall = false
                                    viewModel.checkTrialStatus()
                                },
                                onClose = {
                                    showEarlyPaywall = false
                                }
                            )
                        } else {
                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        modifier = Modifier.navigationBarsPadding(),
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Timer, contentDescription = "Sobriedade") },
                                            label = { Text("Sobriedade") },
                                            selected = currentTab == 0,
                                            onClick = { currentTab = 0 }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.SupportAgent, contentDescription = "Conselheiro") },
                                            label = { Text("Conselheiro") },
                                            selected = currentTab == 1,
                                            onClick = { currentTab = 1 }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.ImportContacts, contentDescription = "Recursos") },
                                            label = { Text("Recursos") },
                                            selected = currentTab == 2,
                                            onClick = { currentTab = 2 }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.FamilyRestroom, contentDescription = "Guardião") },
                                            label = { Text("Guardião") },
                                            selected = false,
                                            onClick = { currentRole = "parent" }
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentTab) {
                                        0 -> UserDashboardScreen(
                                             onUpgradeClick = { showEarlyPaywall = true },
                                            viewModel = viewModel,
                                            onTalkToCounselor = { currentTab = 1 }
                                        )
                                        1 -> SupportScreen(
                                            viewModel = viewModel,
                                            onBack = { currentTab = 0 }
                                        )
                                        2 -> MozambiqueResourcesScreen()
                                    }
                                }
                            }
                        }
                    }

                    // CRITICAL ACCESSIBILITY OVERLAY
                    AnimatedVisibility(
                        visible = showBlockOverlay,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        BlockOverlayScreen(
                            onDismiss = {
                                showBlockOverlay = false
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkBlockTrigger(intent)
    }

    private fun checkBlockTrigger(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("BLOCK_TRIGGERED", false)) {
            showBlockOverlayState.value = true
        }
    }
}

@Composable
fun UserDashboardScreen(
    viewModel: VencerViewModel,
    onTalkToCounselor: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val sobrietyLog by viewModel.sobrietyLog.collectAsState()
    val isPremiumUnlocked by viewModel.isPremiumUnlocked.collectAsState()
    var currentStreakDays by remember { mutableStateOf(0L) }
    var currentStreakHours by remember { mutableStateOf(0L) }
    var currentStreakMinutes by remember { mutableStateOf(0L) }

    // Dynamic timer ticker
    LaunchedEffect(sobrietyLog) {
        while (true) {
            val start = sobrietyLog?.startDateTimestamp ?: System.currentTimeMillis()
            val diffMs = System.currentTimeMillis() - start
            if (diffMs > 0) {
                currentStreakDays = TimeUnit.MILLISECONDS.toDays(diffMs)
                currentStreakHours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
                currentStreakMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60
            } else {
                currentStreakDays = 0
                currentStreakHours = 0
                currentStreakMinutes = 0
            }
            delay(30000) // Update every 30 seconds
        }
    }

    // Helper delay function
    suspend fun delay(timeMs: Long) {
        kotlinx.coroutines.delay(timeMs)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Minha Jornada de Sobriedade",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Dia após dia, construindo uma mente livre e pura.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        if (!isPremiumUnlocked) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ativar Vencer Premium",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Aproveite o seu teste gratuito de 2 dias! Se preferir, pode efetuar a ativação da licença de 30 dias (150 MT) agora mesmo via M-Pesa ou e-Mola sem esperar que o teste expire.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = onUpgradeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pagar Licença Pro (150 MT)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Circular Streak Counter Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currentStreakDays",
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (currentStreakDays == 1L) "DIA LIMPO" else "DIAS LIMPOS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TimeSegment(label = "Horas", value = currentStreakHours)
                        Text(
                            ":",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        TimeSegment(label = "Minutos", value = currentStreakMinutes)
                    }
                }
            }
        }

        // Action Buttons Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.startSobriety() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar Novo Contador")
                    }

                    Button(
                        onClick = { viewModel.recordRelapse() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftAlert)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Registar Recaída (Reiniciar)", color = Color.White)
                    }
                }
            }
        }

        // Daily Motivation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Pensamento do Dia",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = sobrietyLog?.motivationPhrase ?: "Você é mais forte do que seus desejos!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Counselor promo Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Precisa de ajuda agora?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Converse em tempo real com o Conselheiro IA e receba técnicas imediatas de contenção.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Button(
                        onClick = onTalkToCounselor,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Conversar IA", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSegment(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = String.format("%02d", value),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MozambiqueResourcesScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Linhas de Apoio e Recursos",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Se precisar de ajuda profissional especializada em Moçambique, ligue para estes serviços gratuitos:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        item {
            ResourceContactCard(
                title = "Gabinete de Apoio à Mulher e Criança (Violência/Psicológico)",
                number = "Ligue 116",
                description = "Linha fala criança - Apoio psicossocial, denúncias e aconselhamento confidencial gratuito em Moçambique."
            )
        }

        item {
            ResourceContactCard(
                title = "Serviço Nacional de Saúde (Apoio Psicológico)",
                number = "Ligue 119",
                description = "Apoio médico e psicológico geral coordenado pelo Ministério da Saúde (MISAU)."
            )
        }

        item {
            ResourceContactCard(
                title = "Mente Sã Moçambique (Aconselhamento)",
                number = "Ligue (+258) 84 411 9119",
                description = "Organização moçambicana dedicada ao suporte em saúde mental e terapias cognitivas."
            )
        }
    }
}

@Composable
fun ResourceContactCard(
    title: String,
    number: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = number,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// CRITICAL OVERLAY INTERCEPT SCREEN
@Composable
fun BlockOverlayScreen(
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF111111) // High contrast pure dark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DoNotDisturb,
                contentDescription = null,
                tint = SoftAlert,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GUARDIÃO VENCER",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SoftAlert,
                letterSpacing = 2.sp
            )

            Text(
                text = "CONTEÚDO ADULTO INTERCETADO!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            Text(
                text = "O Filtro de Pureza intercetou e bloqueou o acesso a conteúdo de teor impróprio para proteger a sua sobriedade.",
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Não se renda ao impulso!",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Um desejo dura apenas alguns minutos. Faça 3 respirações profundas ou ligue para o suporte.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Voltar ao Painel Seguro", color = Color.Black)
            }
        }
    }
}
