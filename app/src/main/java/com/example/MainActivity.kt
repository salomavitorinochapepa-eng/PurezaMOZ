package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val blockOverlayActive = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent.getBooleanExtra("BLOCK_TRIGGERED", false)) {
            blockOverlayActive.value = true
        }

        setContent {
            MyApplicationTheme {
                VencerApp(blockOverlayActive)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("BLOCK_TRIGGERED", false)) {
            blockOverlayActive.value = true
        }
    }
}

enum class TabItem(val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    Tracker("Rastreador", Icons.Filled.History, Icons.Outlined.History),
    Blocker("Guardião", Icons.Filled.Shield, Icons.Outlined.Shield),
    Mentor("Mentor IA", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    Support("Suporte", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    Premium("Premium", Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium)
}

@Composable
fun VencerApp(
    blockOverlayActive: MutableState<Boolean>
) {
    val context = LocalContext.current
    val database = remember { VencerDatabase.getInstance(context) }
    val repository = remember { VencerRepository(database.dao) }
    val factory = remember { VencerViewModelFactory(repository) }
    val viewModel: VencerViewModel = viewModel(factory = factory)

    // Collect States
    val streak by viewModel.sobrietyStreak.collectAsStateWithLifecycle()
    val blockerSettings by viewModel.blockerSettings.collectAsStateWithLifecycle()
    val isPremium by viewModel.activePayment.collectAsStateWithLifecycle()

    val sharedPrefs = remember { context.getSharedPreferences("vencer_prefs", Context.MODE_PRIVATE) }
    var userRole by remember { mutableStateOf(sharedPrefs.getString("user_role", null)) }

    var selectedTab by remember { mutableStateOf(TabItem.Tracker) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (blockOverlayActive.value) {
            // High intensity protection overlay with breathing aid and PIN code validation
            EmergencyPortalScreen(
                blockerSettings = blockerSettings,
                onUnlockSuccess = {
                    blockOverlayActive.value = false
                }
            )
        } else {
            when (userRole) {
                null -> {
                    RoleSelectionScreen(
                        onRoleSelected = { role ->
                            userRole = role
                        }
                    )
                }
                "parent" -> {
                    ParentModeScreen(
                        viewModel = viewModel,
                        onSwitchProfile = {
                            sharedPrefs.edit().remove("user_role").apply()
                            userRole = null
                        }
                    )
                }
                else -> {
                    // The standard Multi-tab app layout
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = CardBackground,
                                tonalElevation = 8.dp,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                TabItem.values().forEach { tab ->
                                    val isSelected = selectedTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { selectedTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = EmeraldGreen,
                                            selectedTextColor = EmeraldGreen,
                                            unselectedIconColor = SubLightText,
                                            unselectedTextColor = SubLightText,
                                            indicatorColor = CardBackground.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SlateBackground)
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                TabItem.Tracker -> TrackerScreen(
                                    streak = streak,
                                    viewModel = viewModel,
                                    isPremium = isPremium != null,
                                    onGoToPremium = { selectedTab = TabItem.Premium },
                                    onSwitchRole = {
                                        sharedPrefs.edit().remove("user_role").apply()
                                        userRole = null
                                    }
                                )
                                TabItem.Blocker -> BlockerScreen(
                                    settings = blockerSettings,
                                    viewModel = viewModel,
                                    isPremium = isPremium != null,
                                    onGoToPremium = { selectedTab = TabItem.Premium }
                                )
                                TabItem.Mentor -> MentorScreen(
                                    viewModel = viewModel,
                                    isPremium = isPremium != null
                                )
                                TabItem.Support -> SupportScreen(
                                    viewModel = viewModel
                                )
                                TabItem.Premium -> PremiumScreen(
                                    viewModel = viewModel,
                                    isPremium = isPremium != null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: Tracker Screen (Rastreador)
// ==========================================
@Composable
fun TrackerScreen(
    streak: SobrietyStreak?,
    viewModel: VencerViewModel,
    isPremium: Boolean,
    onGoToPremium: () -> Unit,
    onSwitchRole: () -> Unit
) {
    var showRelapseDialog by remember { mutableStateOf(false) }
    var currentMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val startMs = streak?.streakStartDate ?: System.currentTimeMillis()
    val diffMs = maxOf(0L, currentMillis - startMs)
    val totalSeconds = diffMs / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val totalHours = totalMinutes / 60
    val hours = totalHours % 24
    val days = totalHours / 24

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CAMINHO DA PUREZA",
                            style = MaterialTheme.typography.labelMedium,
                            color = LuxuryAmber,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "A Tua Jornada",
                            style = MaterialTheme.typography.headlineSmall,
                            color = LightText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = onSwitchRole,
                        colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, SubLightText.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("switch_to_parental_from_tracker_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Mudar Perfil", fontSize = 11.sp, color = LightText)
                        }
                    }
                }
            }
        }

        // Beautiful luxury stopwatch gauge representing time clean
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow Arc
                val baseColor = EmeraldGreen
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .drawBehind {
                            drawArc(
                                color = baseColor.copy(alpha = 0.15f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx())
                            )
                            // Clean indicator ticking animation arc
                            val progressSweep = ((seconds.toFloat() / 60f) * 360f)
                            drawArc(
                                color = baseColor,
                                startAngle = -90f,
                                sweepAngle = progressSweep,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$days",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = LightText
                    )
                    Text(
                        text = if (days == 1L) "DIA LIMPO" else "DIAS LIMPOS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldGreen,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SubLightText
                    )
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.restartStreak() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("renew_streak_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Renovar", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Renovar Força", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = { showRelapseDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("report_relapse_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = "Relatar Queda")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Relatar Queda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Metric Statistics Grid (Mozambican context)
        item {
            Text(
                text = "Metas e Economias em Moçambique",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LightText,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatisticCard(
                        title = "Melhor Streak",
                        value = "${streak?.bestStreakDays ?: 0} Dias",
                        description = "Recorde histórico",
                        icon = Icons.Default.EmojiEvents,
                        color = LuxuryAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatisticCard(
                        title = "Total Acumulado",
                        value = "${streak?.totalCleanDays ?: 0} Dias",
                        description = "Soma de sobriedade",
                        icon = Icons.Default.Check,
                        color = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val gigabytesSaved = (days * 1.8f).coerceAtLeast(0f)
                    val moneySaved = (gigabytesSaved * 120).toInt() // typical 120 Meticais per GB in Mozambique data plans
                    
                    StatisticCard(
                        title = "Megas Salvos",
                        value = "${String.format("%.1f", gigabytesSaved)} GB",
                        description = "Tráfego poupado",
                        icon = Icons.Default.NetworkWifi,
                        color = CalmTeal,
                        modifier = Modifier.weight(1f)
                    )
                    StatisticCard(
                        title = "Saldo Meticais",
                        value = "$moneySaved MT",
                        description = "Economizado em dados",
                        icon = Icons.Default.Payments,
                        color = LuxuryAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (!isPremium) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoToPremium() }
                        .testTag("premium_ad_banner_tracker"),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = LuxuryAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Aderir ao Vencer Premium Pro 🌟",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = LightText
                            )
                        }
                        Text(
                            text = "Blinde a sua mente e poupe milhares de Meticais em megas de internet descarregando conteúdo indesejado. Liberte o Guardião Heurístico Absoluto!",
                            fontSize = 12.sp,
                            color = SubLightText,
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Apenas 100 MT/mês",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = LuxuryAmber
                            )
                            Text(
                                text = "Ver Benefícios & Ativar →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRelapseDialog) {
        RelapseDialog(
            onDismiss = { showRelapseDialog = false },
            onSubmit = { reason, trigger, intensity ->
                viewModel.recordRelapse(reason, trigger, intensity)
                showRelapseDialog = false
            }
        )
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    description: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = SubLightText, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 21.sp, fontWeight = FontWeight.Black, color = LightText)
            Text(text = description, fontSize = 10.sp, color = SubLightText)
        }
    }
}

@Composable
fun RelapseDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, trigger: String, intensity: Int) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("Boredom") }
    var intensity by remember { mutableFloatStateOf(5f) }

    val triggers = listOf(
        "Boredom" to "Tédio / Solidão",
        "Stress" to "Estresse / Fadiga",
        "Social" to "Redes Sociais / WhatsApp",
        "Insomnia" to "Insonia / Tarde da Noite"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Análise de Recaída (Queda)",
                fontWeight = FontWeight.Bold,
                color = LightText
            )
        },
        containerColor = CardBackground,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Fracassar faz parte do caminho. Não se culpe! Analise o seu gatilho com honestidade científica para vencer a próxima ronda.",
                    fontSize = 12.sp,
                    color = SubLightText
                )

                Text("Gatilho Principal:", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    triggers.forEach { (key, label) ->
                        val selected = trigger == key
                        FilterChip(
                            selected = selected,
                            onClick = { trigger = key },
                            label = { Text(label, fontSize = 11.sp, color = if (selected) Color.Black else LightText) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuxuryAmber
                            )
                        )
                    }
                }

                Text("Intensidade do Desejo: ${intensity.toInt()}/10", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = LuxuryAmber,
                        activeTrackColor = LuxuryAmber,
                        inactiveTrackColor = SubLightText.copy(alpha = 0.3f)
                    )
                )

                Text("Nota Opcional (O que estava a pensar?):", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text("Ex: Me senti desocupado após o trabalho...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = SubLightText.copy(alpha = 0.3f),
                        focusedTextColor = LightText,
                        unfocusedTextColor = LightText
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason, trigger, intensity.toInt()) },
                colors = ButtonDefaults.textButtonColors(contentColor = AlertRed)
            ) {
                Text("Confirmar Reset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = LightText)) {
                Text("Voltar")
            }
        }
    )
}

// ==========================================
// SCREEN 2: Blocker Settings Screen (Guardião)
// ==========================================
@Composable
fun BlockerScreen(
    settings: BlockerSettings?,
    viewModel: VencerViewModel,
    isPremium: Boolean,
    onGoToPremium: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var pinDisableInput by remember { mutableStateOf("") }

    val currentBlockerActive = settings?.isBlockerEnabled ?: false
    val hasPinSet = settings?.pinCode?.isNotEmpty() ?: false

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Blocker Title Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (currentBlockerActive) CalmTeal.copy(alpha = 0.15f) else CardBackground),
                border = BorderStroke(1.dp, if (currentBlockerActive) EmeraldGreen else CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (currentBlockerActive) EmeraldGreen else SubLightText.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (currentBlockerActive) Icons.Default.Shield else Icons.Default.ShieldMoon,
                            contentDescription = "Shield",
                            tint = if (currentBlockerActive) Color.Black else SubLightText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bloqueador Guardião v1.0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = LightText
                        )
                        Text(
                            text = if (currentBlockerActive) "Status: ATIVO & PROTEGENDO" else "Status: Inativo administrativamente",
                            fontSize = 12.sp,
                            color = if (currentBlockerActive) EmeraldGreen else SubLightText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(
                        checked = currentBlockerActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                if (hasPinSet) {
                                    viewModel.toggleBlocker(true, "")
                                } else {
                                    showPinDialog = true
                                }
                            } else {
                                if (hasPinSet) {
                                    showDisableDialog = true
                                } else {
                                    viewModel.toggleBlocker(false, "")
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldGreen
                        ),
                        modifier = Modifier.testTag("blocker_toggle")
                    )
                }
            }
        }

        if (!isPremium) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGoToPremium() }
                        .testTag("premium_ad_banner_blocker"),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = LuxuryAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Desbloquear o Guardião Absoluto 💎",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LightText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(LuxuryAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "PRO", color = LuxuryAmber, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                        Text(
                            text = "A ativação mestre adiciona inteligência heurística para interceptar 100% das buscas e burlas móveis, com guias exclusivos para PCs e routers familiares. Blinde o seu lar contra pornografia!",
                            fontSize = 12.sp,
                            color = SubLightText,
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Aderir por 100 MT/mês →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = EmeraldGreen
                            )
                        }
                    }
                }
            }
        }

        // Direct Guides to Enable Blockers
        item {
            Text("Configurações d’O Guardião", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // 1. Accessibility Service settings link
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null, tint = LuxuryAmber)
                        Text("1. Serviço de Acessibilidade", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "O serviço lê o ecrã do navegador Chrome/Firefox para detetar palavras-chave impróprias. Se encontrar, aciona o Portal do Vencer.",
                        fontSize = 12.sp,
                        color = SubLightText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                Toast.makeText(context, "Procure por 'Bloqueador Guardião Vencer' nos Serviços Descarregados", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao abrir configurações", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleGrey40),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Habilitar Acessibilidade", color = LightText, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. DNS Family Filter Protection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = CalmTeal)
                        Text("2. Bloqueio DNS de Segurança (Moçambique)", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Configure o 'DNS Privado' do seu telemóvel para um DNS familiar gratuito que filtra e proíbe 100% de pornografia nos servidores sem consumir bateria:\n\n" +
                        "• DNS Recom.: family.adguard-dns.com\n" +
                        "• DNS Cloudflare: security.cloudflare-dns.com",
                        fontSize = 12.sp,
                        color = SubLightText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent("android.settings.WRITER_PRIVATE_DNS_SETTINGS")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    Toast.makeText(context, "Vá para Ligações -> Mais Definições de Ligação -> DNS Privado", Toast.LENGTH_LONG).show()
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Abra as Definições e pesquise por 'DNS Privado'", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CalmTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Configurar DNS Privado", color = LightText, fontSize = 12.sp)
                    }
                }
            }
        }

        // PIN Keypad actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen)
                        Text("Senha de Auto-Defesa PIN", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (hasPinSet) "Código PIN definido. Se estiver sob tentação, pedir-lhe-á a senha para poder desligar o bloqueador!"
                        else "Defina uma senha de 4 dígitos. Recomendamos confiar a senha a um amigo de confiança ou familiar de Moçambique para lhe dar apoio.",
                        fontSize = 12.sp,
                        color = SubLightText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasPinSet) CardBackground else EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        border = if (hasPinSet) BorderStroke(1.dp, SubLightText.copy(alpha = 0.3f)) else null
                    ) {
                        Text(
                            text = if (hasPinSet) "Alterar Senha PIN" else "Criar Senha PIN",
                            color = if (hasPinSet) LightText else Color.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configurar PIN de Bloqueio", color = LightText) },
            containerColor = CardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Introduza um código numérico para o PIN (recomendado 4 dígitos).", fontSize = 12.sp, color = SubLightText)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LightText, focusedBorderColor = EmeraldGreen
                        ),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.isNotEmpty()) {
                            viewModel.setBlockerPassword(pinInput)
                            showPinDialog = false
                            pinInput = ""
                            Toast.makeText(context, "Senha de proteção configurada com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Salvar PIN", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Voltar") }
            }
        )
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text("Digite de Senha do Guardião", color = LightText) },
            containerColor = CardBackground,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⚠️ O desvio exige vencer a resistência mental. Digite sua senha numérica para autorizar a desativação d'O Guardião.", fontSize = 12.sp, color = SubLightText)
                    OutlinedTextField(
                        value = pinDisableInput,
                        onValueChange = { if (it.length <= 6) pinDisableInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = LightText, focusedBorderColor = AlertRed),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = viewModel.toggleBlocker(false, pinDisableInput)
                        if (success) {
                            showDisableDialog = false
                            pinDisableInput = ""
                            Toast.makeText(context, "Guardião destravado com sucesso.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Erro: Código PIN incorreto. Respire fundo, resista!", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Validar & Destravar", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) { Text("Desistir") }
            }
        )
    }
}

// ==========================================
// SCREEN 3: Mental Advisor AI Chat (Mentor IA)
// ==========================================
@Composable
fun MentorScreen(
    viewModel: VencerViewModel,
    isPremium: Boolean
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var rawText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    if (!isPremium) {
        // Feature Lock Preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = LuxuryAmber,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Conselheiro IA de Crise",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A mentoria direta alimentada pelo modelo Gemini está reservada para os subscritores para ajudar a cobrir os custos operacionais do servidor.\n\n" +
                    "Obtenha apoio emocional instantâneo ilimitado em português de Moçambique agora.",
                    fontSize = 13.sp,
                    color = SubLightText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Visual aid helper tips anyway
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("💡 Conselho de Crise do Dia:", fontWeight = FontWeight.Bold, color = LuxuryAmber, fontSize = 12.sp)
                        Text(
                            "Quando vier o impulso, o pico do desejo físico dura apenas 10 minutos. Ocupar a mente com uma tarefa física mecânica é o método mais eficaz do mundo para contornar o ciclo dopaminérgico. Beba água e saia agora do telemóvel!",
                            fontSize = 11.sp,
                            color = LightText
                        )
                    }
                }
            }
        }
    } else {
        // Full AI counseling chat screen
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CalmTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CalmTeal, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Mentor Mental IA", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
                        Text("Online em Moçambique", fontSize = 11.sp, color = EmeraldGreen)
                    }
                }
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar Conversa", tint = SubLightText)
                }
            }

            // Chat Messages log
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(40.dp))
                            Text(
                                "Conversa Vazia",
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Text(
                                "Eu sou seu assessor confidencial. Fale abertamente sobre as tentações, ansiedades ou o seu dia de luta. Tudo é anónimo e guardado estritamente offline no seu dispositivo.",
                                fontSize = 12.sp,
                                color = SubLightText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    items(messages) { chat ->
                        val isUser = chat.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUser) CalmTeal else CardBackground
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = chat.message,
                                        color = if (isUser) Color.Black else LightText,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Text(
                                "O Mentor IA está a formular pensamentos...",
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = LuxuryAmber
                            )
                        }
                    }
                }
            }

            // Quick crisis prompt shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prompts = listOf(
                    "Estou em perigo físico! (Socorro)" to "Estou prestes a cair agora mesmo, socorro!",
                    "💡 Diga um desvio físico" to "Preciso de um desvio mental físico para quebrar o impulso agora.",
                    "🧘 Respiração contra estresse" to "Como a respiração ajuda a desarmar o desejo dopaminérgico?",
                    "🙏 Palavra de Encorajamento" to "Me dê uma mensagem inspiradora para manter minha pureza hoje."
                )
                prompts.forEach { (label, pr) ->
                    AssistChip(
                        onClick = {
                            viewModel.sendMessageToAI(pr)
                        },
                        label = { Text(label, fontSize = 11.sp, color = LightText) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CardBackground
                        )
                    )
                }
            }

            // Chat text input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Fale confidencialmente com o Mentor...", fontSize = 13.sp, color = SubLightText) },
                    modifier = Modifier.weight(1f).testTag("chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightText,
                        focusedBorderColor = EmeraldGreen
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (rawText.trim().isNotEmpty()) {
                            viewModel.sendMessageToAI(rawText)
                            rawText = ""
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EmeraldGreen)
                        .testTag("send_chat_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.Black)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: Mozambican Premium Payments (Premium)
// ==========================================
@Composable
fun PremiumScreen(
    viewModel: VencerViewModel,
    isPremium: Boolean
) {
    val context = LocalContext.current
    var phoneInput by remember { mutableStateOf("") }
    var transactionInput by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("Mensal - 100 MT") }

    val slips by viewModel.paymentSlips.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingScreenshot.collectAsStateWithLifecycle()
    val validationResult by viewModel.screenshotValidationState.collectAsStateWithLifecycle()

    val contentResolver = context.contentResolver
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            try {
                selectedImageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, it)) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, it)
                }
                viewModel.resetScreenshotValidation()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao carregar o comprovativo na app.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val activePayment by viewModel.activePayment.collectAsStateWithLifecycle()
    val blockerSettingsState by viewModel.blockerSettings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Plan status header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isPremium) EmeraldGreen.copy(alpha = 0.15f) else CardBackground),
                border = BorderStroke(1.dp, if (isPremium) EmeraldGreen else SubLightText.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPremium) EmeraldGreen else LuxuryAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (isPremium) "PLANO PREMIUM ATIVO ✓" else "PREMIUM BLOQUEADO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LightText
                            )
                            Text(
                                text = if (isPremium) "Obrigado pelo seu contributo! Servidor IA desbloqueado." else "Preço solidário para infraestruturas do aplicativo",
                                fontSize = 11.sp,
                                color = SubLightText
                            )
                        }
                    }

                    if (isPremium && activePayment != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = EmeraldGreen.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", java.util.Locale.getDefault()) }
                        val startDateStr = sdf.format(java.util.Date(activePayment!!.dateRegistered))

                        val expirationMillis = activePayment!!.dateRegistered + (30L * 24 * 60 * 60 * 1000)
                        val endDateStr = sdf.format(java.util.Date(expirationMillis))

                        val diffMillis = expirationMillis - System.currentTimeMillis()
                        val daysRemaining = (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Detalhes da tua Subscrição de 30 Dias:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxuryAmber
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ativado em:",
                                    fontSize = 12.sp,
                                    color = SubLightText
                                )
                                Text(
                                    text = startDateStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LightText
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Válido até:",
                                    fontSize = 12.sp,
                                    color = SubLightText
                                )
                                Text(
                                    text = endDateStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estado / Dias Restantes:",
                                    fontSize = 12.sp,
                                    color = SubLightText
                                )
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$daysRemaining dias restantes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isPremium) {
            val isStrict = blockerSettingsState?.strictMode ?: false

            item {
                Text(
                    text = "🛡️ Suite de Proteção Absoluta Ouro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LuxuryAmber,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 1. Mobile Absolute Blocker Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("premium_strict_blocker_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isStrict) EmeraldGreen.copy(alpha = 0.08f) else CardBackground
                    ),
                    border = BorderStroke(1.dp, if (isStrict) EmeraldGreen.copy(alpha = 0.5f) else CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isStrict) EmeraldGreen else LuxuryAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Bloqueio Absoluto Mestre (+18)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = LightText
                                    )
                                    Text(
                                        text = if (isStrict) "Status: ATIVO & IMPENETRÁVEL" else "Status: Filtragem Padrão",
                                        fontSize = 11.sp,
                                        color = if (isStrict) EmeraldGreen else SubLightText
                                    )
                                }
                            }
                            Switch(
                                checked = isStrict,
                                onCheckedChange = { viewModel.setStrictMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = EmeraldGreen
                                ),
                                modifier = Modifier.testTag("premium_strict_blocker_switch")
                            )
                        }
                        Text(
                            text = "A ativação mestre expande a inteligência artificial heuristicamente no telemóvel para interceptar 100% dos termos vulgares, canais alternativos e buscas no Google de pornografia ou meios de burlar os bloqueadores. Essencial para auto-ajuda definitiva e controlo severo.",
                            fontSize = 12.sp,
                            color = SubLightText
                        )
                    }
                }
            }

            // 2. PC & Computer Blocking Guide (Windows / macOS)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pc_blocker_guide_card"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, tint = CalmTeal)
                                Text(
                                    text = "💻 Bloqueio Total no Computador",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LightText
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SubLightText
                            )
                        }
                        
                        Text(
                            text = "Guia prático para proteger computadores Windows, Mac ou computadores portáteis de forma absoluta.",
                            fontSize = 12.sp,
                            color = SubLightText
                        )

                        if (expanded) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = SubLightText.copy(alpha = 0.2f))
                            Text(
                                text = "Para bloquear pornografia na raiz do seu PC ou computador portátil:\n\n" +
                                       "1. No painel de controle do Windows ou Definições do macOS, navegue para Configurações de Rede.\n\n" +
                                       "2. Edite as propriedades de IP da sua rede IPv4 ativa.\n\n" +
                                       "3. Defina os endereços DNS para o Filtro de Família Seguro da Cloudflare:\n" +
                                       "• DNS Preferido/Primário:  1.1.1.3\n" +
                                       "• DNS Alternativo/Secundário:  1.0.0.3\n\n" +
                                       "4. Alternativa de Browser: Instale a extensão 'AdGuard' de navegador e configure o modo familiar para bloquear anúncios e conteúdo adulto inteiramente, trancando a extensão com senha.",
                                fontSize = 12.sp,
                                color = LightText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Router Level Total Network Blocking Guide
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("router_blocker_guide_card"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Router, contentDescription = null, tint = LuxuryAmber)
                                Text(
                                    text = "🌐 Bloquear Conteúdo Adulto no Router de Casa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LightText
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SubLightText
                            )
                        }
                        
                        Text(
                            text = "Blinde a sua casa por completo. Bloqueia TV, tablets, telemóveis de amigos na sua Wi-Fi automaticamente.",
                            fontSize = 12.sp,
                            color = SubLightText
                        )

                        if (expanded) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = SubLightText.copy(alpha = 0.2f))
                            Text(
                                text = "Como configurar o Roteador Wi-Fi:\n\n" +
                                       "1. Abra o navegador no telemóvel ou PC e aceda ao IP do router (geralmente 192.168.1.1 ou atrás do router).\n\n" +
                                       "2. Digite a senha do roteador (pode pesquisar o modelo online se não souber).\n\n" +
                                       "3. Procure pelas definições de DNS (normalmente na seção Configurações DHCP ou WAN Settings).\n\n" +
                                       "4. Altere os servidores DNS para os IPS da CleanBrowsing (Adult Filter):\n" +
                                       "• Primário: 185.228.168.168\n" +
                                       "• Secundário: 185.228.169.168\n\n" +
                                       "5. Salve e reinicie o Router.\n\n" +
                                       "Resultado: Qualquer dispositivo conectado ao seu Wi-Fi estará totalmente limpo e protegido do mal contra pornografia de forma permanente!",
                                fontSize = 12.sp,
                                color = LightText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 4. Safe Companion Security (Anti-Unlock)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("companion_blocker_guide_card"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = EmeraldGreen)
                                Text(
                                    text = "🔒 Bloqueio de Auto-Defesa Mútuo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = LightText
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SubLightText
                            )
                        }
                        
                        Text(
                            text = "Aprenda a trancar a sua própria capacidade de burlar ou desativar o aplicativo sob forte tentação.",
                            fontSize = 12.sp,
                            color = SubLightText
                        )

                        if (expanded) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = SubLightText.copy(alpha = 0.2f))
                            Text(
                                text = "Instruções de Auto-Defesa:\n\n" +
                                       "• Ative a senha PIN na aba 'Guardião'.\n" +
                                       "• NÃO escreva uma senha que você vai memorizar. Peça a um amigo de confiança, seu parceiro, ou ao seu pai/mãe para digitar um PIN secreto e guardar no telemóvel dele(a).\n" +
                                       "• Dessa maneira, quando o seu cérebro estiver sob forte ansiedade e tentar desativar o bloqueio por completo, o aplicativo recusará liberar o acesso sem o PIN deles.\n" +
                                       "• Este método de auto-restrição ativa salvou milhares de vidas de dependentes e protege integralmente a integridade espiritual e psicológica.",
                                fontSize = 12.sp,
                                color = LightText,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isPremium) {
            item {
                Text(
                    text = "Apoie o Projeto & Libere Recursos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )
            }

            // Importance Explanation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("premium_importance_card"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, LuxuryAmber.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = LuxuryAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Porque é Importante Assinar o Plano? 🤔",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = LightText
                            )
                        }

                        Text(
                            text = "A sua mente e o seu bem-estar espiritual e financeiro são o seu ativo mais precioso. Eis porque o Vencer Pro é uma prioridade essencial na sua caminhada de auto-ajuda:",
                            fontSize = 12.sp,
                            color = LightText,
                            lineHeight = 16.sp
                        )

                        Divider(color = SubLightText.copy(alpha = 0.15f))

                        // Reason 1
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Apoio ao Servidor e Comunidade", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightText)
                                Text("A infraestrutura do Vencer utiliza modelos avançados de Inteligência Artificial e OCR para leitura ótica de pagamentos. O seu pequeno contributo ajuda a cobrir custos dos servidores e manter o suporte ativo e acessível a milhares de jovens necessitados em Moçambique.", fontSize = 11.sp, color = SubLightText, lineHeight = 15.sp)
                            }
                        }

                        // Reason 2
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CalmTeal, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Blindagem Heurística contra Quedas", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightText)
                                Text("Não confie apenas na força de vontade sob forte tentação mental. O Guardião Heurístico Mestre atua na raiz do telemóvel, detetando e bloqueando automaticamente tentativas desesperadas de burla antes do deslize acontecer.", fontSize = 11.sp, color = SubLightText, lineHeight = 15.sp)
                            }
                        }

                        // Reason 3
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = LuxuryAmber, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Economia Brutal de Saldo M-Pesa", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightText)
                                Text("Ver ou descarregar vídeos pesados em portais adultos consome gigabytes de dados móveis (geralmente mais de 300 MT por semana nas redes como Vodacom ou Movitel). O Vencer Pro poupa a sua carteira e o seu saldo M-Pesa de forma imediata!", fontSize = 11.sp, color = SubLightText, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }

            // Core Premium Benefits Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("premium_benefits_card"),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Benefícios Exclusivos do Vencer Pro 💎",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = LightText
                            )
                        }

                        Text(
                            text = "Ao ativar o plano pago, desbloqueia imediatamente recursos que blindam toda a sua vida e dispositivos:",
                            fontSize = 12.sp,
                            color = LightText
                        )

                        Divider(color = SubLightText.copy(alpha = 0.15f))

                        val benefitsList = listOf(
                            "🛡️ Bloqueio Absoluto Mestre (+18)" to "IA Heurística expandida no telemóvel impedindo qualquer pesquisa ou termo vulgar nos motores de busca.",
                            "💻 Proteção Total no Computador" to "Acesso a tutoriais passo a passo e ficheiros DNS seguros Cloudflare de Família para blindar PCs Windows ou Mac.",
                            "🌐 Proteção do Router Familiar" to "Guia de trancamento de IP DNS familiar completo na fonte Wi-Fi para que TVs, tablets e visitas fiquem protegidas.",
                            "🤖 Mentor Inteligente IA Sem Limites" to "Interaja livremente com o Mentor especializado em terapia cognitiva para superar gatilhos e ansiedades, sem qualquer teto diário.",
                            "🤝 PIN de Auto-Defesa Mútuo" to "Capacidade de trancar a desativação do Guardião atrás de um PIN confiado ao seu padrinho/madrinha de reabilitação."
                        )

                        benefitsList.forEach { (benefitTitle, benefitDesc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 14.sp)
                                Column {
                                    Text(benefitTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LightText)
                                    Text(benefitDesc, fontSize = 11.sp, color = SubLightText, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Price Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Preços acessíveis em Meticais (MT):", fontWeight = FontWeight.Bold, color = LuxuryAmber)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Plano Mensal", color = LightText)
                            Text("100 MT / por mês", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Plano Anual (Recomendado)", color = LightText)
                            Text("700 MT / por ano", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Os megas gastos ao descarregar pornografia rondavam mais de 300 MT por semana por causa dos pesados vídeos móveis. Investir no Vencer cura sua mente e poupa carteira!",
                            fontSize = 11.sp,
                            color = SubLightText,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Mozambique payment instructions form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Subscrição e Auto-Proteção Guardião",
                            fontWeight = FontWeight.Bold,
                            color = LuxuryAmber,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "1. Efetue a transferência (M-Pesa / e-Mola) correspondente ao plano:\n" +
                            "👉 Número Vodacom: 852046856\n" +
                            "👉 Registado em nome de:\n" +
                            "Salomão Victorino Joao Chapepa\n\n" +
                            "2. Tire captura de ecrã (screenshot) e carregue-a abaixo para validação com Inteligência Artificial.",
                            fontSize = 12.sp,
                            color = LightText
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Radio select plan
                        Text("Plano a Ativar:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedPlan == "Mensal - 100 MT",
                                    onClick = { selectedPlan = "Mensal - 100 MT" },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                                )
                                Text("Mensal 100 MT", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedPlan == "Anual - 700 MT",
                                    onClick = { selectedPlan = "Anual - 700 MT" },
                                    colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                                )
                                Text("Anual 700 MT", fontSize = 12.sp)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = SubLightText.copy(alpha = 0.2f))

                        // Screenshot Section
                        Text(
                            "Passo 2: Carregar Captura de Ecrã", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 13.sp,
                            color = EmeraldGreen
                        )

                        if (selectedImageBitmap == null) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth().testTag("pick_receipt_screenshot"),
                                colors = ButtonDefaults.buttonColors(containerColor = CardBackground.copy(alpha = 0.8f)),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Escolher Imagem do Comprovativo", color = LightText, fontSize = 12.sp)
                            }
                        } else {
                            // Selected Image Preview & Operations
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview do comprovativo",
                                    modifier = Modifier
                                        .heightIn(max = 140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                    ) {
                                        Text("Substituir", fontSize = 11.sp, color = LightText)
                                    }

                                    Button(
                                        onClick = {
                                            selectedImageBitmap?.let {
                                                viewModel.analyzePaymentScreenshot(it, "image/jpeg", selectedPlan)
                                            }
                                        },
                                        modifier = Modifier.weight(1.5f).testTag("validate_screenshot_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                        enabled = !isAnalyzing
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Validar com IA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Analyzing feedback
                        if (isAnalyzing) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldGreen)
                                Text(
                                    "🔧 IA do Guardião a decodificar imagem... Verificando rede Vodacom/Movitel, destinatário Salomão, data exata e quantia de transferência.",
                                    fontSize = 11.sp,
                                    color = EmeraldGreen,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Validation result display
                        validationResult?.let { res ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (res.success) EmeraldGreen.copy(alpha = 0.12f) else AlertRed.copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (res.success) EmeraldGreen else AlertRed
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (res.success) EmeraldGreen else AlertRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (res.success) "Leitura Óptica Aprovada!" else "Erro na Validação IA",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (res.success) EmeraldGreen else AlertRed
                                        )
                                    }
                                    Text("Operadora: ${res.operator}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LightText)
                                    Text("Data Registada: ${res.date}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LightText)
                                    Text("Quantia: ${res.amount}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LightText)
                                    Text("Código Único: ${res.transactionId}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = LuxuryAmber)
                                    Text(res.message, fontSize = 11.sp, color = SubLightText, lineHeight = 15.sp)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = SubLightText.copy(alpha = 0.2f))

                        // Manual Entry Fallback / Alternative Option
                        var showManualFields by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManualFields = !showManualFields }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Opção Alternativa: Digitar Registro Manual", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SubLightText)
                            Icon(
                                imageVector = if (showManualFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SubLightText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (showManualFields) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("Seu Celular M-Pesa de Origem", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: 84XXXXXXX ou 85XXXXXXX") },
                                    modifier = Modifier.fillMaxWidth().testTag("manual_phone_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedTextColor = LightText)
                                )

                                OutlinedTextField(
                                    value = transactionInput,
                                    onValueChange = { transactionInput = it },
                                    label = { Text("Código de Transação M-Pesa", fontSize = 11.sp) },
                                    placeholder = { Text("Ex: AJ38SJD82K") },
                                    modifier = Modifier.fillMaxWidth().testTag("transaction_input"),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, focusedTextColor = LightText)
                                )

                                Button(
                                    onClick = {
                                        if (phoneInput.trim().isEmpty() || transactionInput.trim().isEmpty()) {
                                            Toast.makeText(context, "Erro: Por favor, preencha todos os campos corretamente para validação.", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.submitMpesaPayment(
                                                transactionId = transactionInput.trim(),
                                                phoneNumber = phoneInput.trim(),
                                                plan = selectedPlan
                                            )
                                            phoneInput = ""
                                            transactionInput = ""
                                            Toast.makeText(context, "Contributo Submetido! Aprovado instantaneamente pelo Líder Salomão Victorino.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("submit_payment_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                                ) {
                                    Text("Submeter & Validar Ativação", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Letter from leader Salomão
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Carta do Fundador Salomão Victorino",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryAmber
                        )
                        Text(
                            "Saudações de Moçambique!\n\n" +
                            "Obrigado por ajudar a financiar as chamadas à rede neural e manter o 'Vencer' de forma independente em Moçambique, África.\n\n" +
                            "Agora você tem acesso ilimitado à inteligência artificial do mentor e recursos de auto-proteção avançados. O vício teme as pessoas organizadas. Continue lutando de cabeça erguida todos os dias!",
                            fontSize = 13.sp,
                            color = LightText,
                            lineHeight = 18.sp
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "✓ Premium Habilitado permanente para testes.",
                            fontSize = 11.sp,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // List historic payments registered locally
        if (slips.isNotEmpty()) {
            item {
                Text("Registos de Submissões Locais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(slips) { slip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Nº Cél: ${slip.phoneNumber}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LightText)
                            Text("Código: ${slip.transactionId}", fontSize = 11.sp, color = SubLightText)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(slip.planSelected, fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            Text("Aprovado localmente ✓", fontSize = 10.sp, color = EmeraldGreen)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// OVERLAY: The Emergency Block Portal Screen
// ==========================================
@Composable
fun EmergencyPortalScreen(
    blockerSettings: BlockerSettings?,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var inputPinCode by remember { mutableStateOf("") }
    
    // Smooth Breathing pulse animations (Linear pacing)
    val breathingTransition = rememberInfiniteTransition(label = "respira_inf")
    val radiusPulse by breathingTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radiusPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Warning Icon
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = "Porn blocked",
            tint = AlertRed,
            modifier = Modifier.size(62.dp)
        )

        Text(
            text = "IMPULSO INTERCETADO!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = LightText,
            letterSpacing = 1.sp
        )

        Text(
            text = "O Guardião impediu um acesso nocivo. Isso não é um castigo, é a tua proteção. Respire agora com o assistente.",
            fontSize = 13.sp,
            color = SubLightText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // The Breathing helper visual widget
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Respiração Guiada (Técnica Soltura 4-4)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LuxuryAmber
                )

                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .drawBehind {
                            drawCircle(
                                color = EmeraldGreen.copy(alpha = 0.15f * radiusPulse),
                                radius = 70.dp.toPx() * radiusPulse
                            )
                            drawCircle(
                                color = EmeraldGreen,
                                radius = 35.dp.toPx()
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Spa, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                }

                // Breathing action text guides
                val statusString = if (radiusPulse > 1.02f) "Inspire profundamente o ar puro... ✨" else "Exile as cinzas impulsivas lentamente... 🍃"
                Text(
                    text = statusString,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Validation Form to quit the blocked screen
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Desativar e Liberar Ecrã", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Se você realmente precisa desbloquear o seu ecrã, insira a sua senha secreta do Guardião configurada. Resista um pouco mais se possível!",
                    fontSize = 11.sp,
                    color = SubLightText
                )

                OutlinedTextField(
                    value = inputPinCode,
                    onValueChange = { if (it.length <= 6) inputPinCode = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Insira a senha de auto-defesa") },
                    modifier = Modifier.fillMaxWidth().testTag("unlock_pin_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightText,
                        focusedBorderColor = AlertRed
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )

                Button(
                    onClick = {
                        val realPin = blockerSettings?.pinCode ?: ""
                        if (realPin.isEmpty() || inputPinCode == realPin) {
                            onUnlockSuccess()
                            Toast.makeText(context, "Sua sobriedade continua ativa. Siga forte!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Senha incorreta! Respire fundo e tente novamente.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("submit_unlock_pin_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Autenticar & Sair", color = LightText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
