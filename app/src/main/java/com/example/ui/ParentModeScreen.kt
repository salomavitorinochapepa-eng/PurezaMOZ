package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ParentLog
import com.example.notification.InspirationNotificationHelper
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*


// Define ChildProfile at the top of the file
data class ChildProfile(
    val name: String,
    val email: String,
    val screenTimeLimit: Float = 2.0f,
    val currentScreenTime: Float = 0.5f,
    val autoFilterEnabled: Boolean = true,
    val blockExceeded: Boolean = true
) {
    fun toSerializedString(): String {
        return "$name;;$email;;$screenTimeLimit;;$currentScreenTime;;$autoFilterEnabled;;$blockExceeded"
    }

    companion object {
        fun fromSerializedString(str: String): ChildProfile? {
            return try {
                val parts = str.split(";;")
                if (parts.size >= 6) {
                    ChildProfile(
                        name = parts[0],
                        email = parts[1],
                        screenTimeLimit = parts[2].toFloatOrNull() ?: 2.0f,
                        currentScreenTime = parts[3].toFloatOrNull() ?: 0.5f,
                        autoFilterEnabled = parts[4].toBooleanStrictOrNull() ?: true,
                        blockExceeded = parts[5].toBooleanStrictOrNull() ?: true
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentModeScreen(
    viewModel: VencerViewModel,
    onSwitchProfile: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vencer_prefs", Context.MODE_PRIVATE) }
    
    // Load list of child profiles from preferences or defaults
    var childProfiles by remember {
        val saved = sharedPrefs.getString("parent_child_profiles", null)
        val list = if (saved != null) {
            saved.split("##").mapNotNull { ChildProfile.fromSerializedString(it) }
        } else emptyList()
        
        mutableStateOf(
            if (list.isNotEmpty()) list else listOf(
                ChildProfile("Eduardo", "eduardo@gmail.com", 2.0f, 1.8f, true, true),
                ChildProfile("Sara", "sara@gmail.com", 3.0f, 0.5f, true, true)
            )
        )
    }

    // Currently selected child email
    var selectedChildEmail by remember {
        mutableStateOf(sharedPrefs.getString("parent_selected_child_email", "eduardo@gmail.com") ?: "eduardo@gmail.com")
    }

    // Helper to find index or active child
    val activeChild = childProfiles.find { it.email == selectedChildEmail } ?: childProfiles.firstOrNull() ?: ChildProfile("Eduardo", "eduardo@gmail.com")

    // Bind current child specific parameters
    val automaticFilterEnabled = activeChild.autoFilterEnabled
    val blockWhenLimitExceeded = activeChild.blockExceeded
    val screenTimeLimitHours = activeChild.screenTimeLimit
    val childScreenTimeHours = activeChild.currentScreenTime
    val childEmail = activeChild.email

    val saveChildProfiles: (List<ChildProfile>) -> Unit = { list ->
        childProfiles = list
        val serialized = list.joinToString("##") { it.toSerializedString() }
        sharedPrefs.edit().putString("parent_child_profiles", serialized).apply()
    }

    val updateActiveChild: (ChildProfile) -> Unit = { updated ->
        val newList = childProfiles.map {
            if (it.email == updated.email) updated else it
        }
        saveChildProfiles(newList)
        // Keep single backward-compatible keys in sync as well
        sharedPrefs.edit()
            .putBoolean("parent_auto_filter", updated.autoFilterEnabled)
            .putBoolean("parent_lock_limit", updated.blockExceeded)
            .putFloat("parent_screen_time_limit", updated.screenTimeLimit)
            .putFloat("parent_child_screen_time", updated.currentScreenTime)
            .putString("parent_child_email", updated.email)
            .putString("parent_selected_child_email", updated.email)
            .apply()
        
        // Force recomposition update
        selectedChildEmail = updated.email
    }

    val addChild: (String, String, Float) -> Boolean = { name, email, limit ->
        if (name.isBlank() || email.isBlank()) false
        else if (childProfiles.any { it.email.lowercase() == email.lowercase() }) false
        else {
            val newChild = ChildProfile(name, email, limit, 0.0f, true, true)
            val newList = childProfiles + newChild
            saveChildProfiles(newList)
            updateActiveChild(newChild)
            true
        }
    }

    val removeChild: (ChildProfile) -> Unit = { target ->
        val newList = childProfiles.filter { it.email != target.email }
        if (newList.isNotEmpty()) {
            saveChildProfiles(newList)
            if (selectedChildEmail == target.email) {
                val nextActive = newList.first()
                updateActiveChild(nextActive)
            }
        } else {
            Toast.makeText(context, "Deve manter pelo menos um filho configurado.", Toast.LENGTH_SHORT).show()
        }
    }

    // Custom blocked keywords persistence
    var customKeywordsString by remember {
        mutableStateOf(sharedPrefs.getString("parent_custom_keywords", "jogos, facebook, apostas") ?: "")
    }

    // Parent database records
    val parentLogs by viewModel.parentLogs.collectAsStateWithLifecycle()

    // Screen tabs: 0: Painel de Visão Geral, 1: Histórico de Apanhados, 2: Regras de Bloqueio
    var currentSubTab by remember { mutableStateOf(0) }

    // Simulator input State
    var simulationInputText by remember { mutableStateOf("") }
    var newKeywordInputText by remember { mutableStateOf("") }

    // Function to show real parent alerts and populate DB
    val triggerParentAlert: (String, String) -> Unit = { query, type ->
        val alertMessage = "O seu filho (${activeChild.name}) tentou aceder a conteúdo impróprio: \"$query\""
        InspirationNotificationHelper.showParentAlertNotification(context, alertMessage)
        viewModel.addParentLog(
            searchQueryOrUrl = query,
            actionTaken = "Bloqueado & Alerta Enviado",
            isAlert = true,
            childEmail = childEmail
        )
        Toast.makeText(context, "🚨 Alerta Real de Conteúdo +18 Activado para ${activeChild.name}!", Toast.LENGTH_LONG).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LuxuryAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = LuxuryAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Vencer Parental",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Text(
                                text = "Controlo & Proteção de Menores",
                                fontSize = 10.sp,
                                color = SubLightText
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = onSwitchProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                        border = BorderStroke(1.dp, SubLightText.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                            .testTag("switch_to_recovery_profile_button"),
                        shape = RoundedCornerShape(8.dp)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBackground)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Horizontal scrollable Tab options for Parent screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(0, "Painel Ativo", Icons.Default.Dashboard),
                    Triple(1, "Histórico Net", Icons.Default.History),
                    Triple(2, "Restrições", Icons.Default.Settings)
                ).forEach { (index, title, icon) ->
                    val isSelected = currentSubTab == index
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentSubTab = index }
                            .testTag("parent_tab_pill_$index"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else CardBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) EmeraldGreen else Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) EmeraldGreen else SubLightText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LightText else SubLightText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Main Contents switching based on sub tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("parent_main_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (currentSubTab) {
                    0 -> { // GENERAL PARENTAL DASHBOARD
                        // 1. Connection status card - now replaced with Management of multiple children
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "👥 Gestão de Filhos (${childProfiles.size})",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LightText
                                            )
                                            Text(
                                                text = "Configure e bloqueie conteúdo adulto individualmente.",
                                                fontSize = 10.sp,
                                                color = SubLightText
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(EmeraldGreen.copy(alpha = 0.1f), CircleShape)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "REAL-TIME SYNC",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Children list profiles
                                    Text(
                                        text = "Toque num filho para alternar o painel e as regras ativas:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = LuxuryAmber,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        childProfiles.forEach { profile ->
                                            val isSelected = profile.email == selectedChildEmail
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        updateActiveChild(profile)
                                                        Toast.makeText(context, "Painel focado em: ${profile.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .testTag("child_profile_item_${profile.name}"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.08f) else SlateBackground.copy(alpha = 0.5f)
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isSelected) EmeraldGreen else Color.Transparent
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) EmeraldGreen.copy(alpha = 0.2f) else SubLightText.copy(alpha = 0.15f)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isSelected) Icons.Default.ChildCare else Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint = if (isSelected) EmeraldGreen else SubLightText,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        Column {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = profile.name,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = LightText
                                                                )
                                                                if (isSelected) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(EmeraldGreen, RoundedCornerShape(4.dp))
                                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "FOCADO",
                                                                            fontSize = 7.sp,
                                                                            fontWeight = FontWeight.ExtraBold,
                                                                            color = SlateBackground
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            Text(
                                                                text = profile.email,
                                                                fontSize = 10.sp,
                                                                color = SubLightText
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.End,
                                                            modifier = Modifier.padding(end = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Ecrã: ${profile.currentScreenTime}h / ${profile.screenTimeLimit}h",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = LightText
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (profile.autoFilterEnabled) Icons.Default.Security else Icons.Default.Warning,
                                                                    contentDescription = null,
                                                                    tint = if (profile.autoFilterEnabled) EmeraldGreen else AlertRed,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                                Text(
                                                                    text = if (profile.autoFilterEnabled) "Filtro +18 ON" else "Filtro +18 OFF",
                                                                    fontSize = 9.sp,
                                                                    color = if (profile.autoFilterEnabled) EmeraldGreen else AlertRed
                                                                )
                                                            }
                                                        }

                                                        // Delete button if profiles count > 1
                                                        if (childProfiles.size > 1) {
                                                            IconButton(
                                                                onClick = {
                                                                    removeChild(profile)
                                                                    Toast.makeText(context, "Perfil de ${profile.name} removido.", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Remover",
                                                                    tint = AlertRed.copy(alpha = 0.8f),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Divider(color = SubLightText.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Add child form section
                                    var showAddChildForm by remember { mutableStateOf(false) }

                                    if (!showAddChildForm) {
                                        Button(
                                            onClick = { showAddChildForm = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                                            border = BorderStroke(1.dp, LuxuryAmber.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_toggle_add_child_form"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                tint = LuxuryAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Adicionar Novo Filho", fontSize = 12.sp, color = LightText, fontWeight = FontWeight.SemiBold)
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SlateBackground.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, SubLightText.copy(alpha = 0.2f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Registar Novo Filho",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LuxuryAmber
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))

                                                var newNameInput by remember { mutableStateOf("") }
                                                var newEmailInput by remember { mutableStateOf("") }
                                                var newLimitInput by remember { mutableStateOf(2.0f) }

                                                OutlinedTextField(
                                                    value = newNameInput,
                                                    onValueChange = { newNameInput = it },
                                                    label = { Text("Nome do Filho", fontSize = 11.sp) },
                                                    placeholder = { Text("Ex: Lucas", fontSize = 11.sp, color = SubLightText) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(55.dp)
                                                        .testTag("add_child_name_input"),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        unfocusedBorderColor = SubLightText.copy(alpha = 0.3f),
                                                        focusedBorderColor = EmeraldGreen,
                                                        focusedTextColor = LightText,
                                                        unfocusedTextColor = LightText
                                                    ),
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))

                                                OutlinedTextField(
                                                    value = newEmailInput,
                                                    onValueChange = { newEmailInput = it },
                                                    label = { Text("E-mail do Filho", fontSize = 11.sp) },
                                                    placeholder = { Text("lucas@gmail.com", fontSize = 11.sp, color = SubLightText) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(55.dp)
                                                        .testTag("add_child_email_input"),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        unfocusedBorderColor = SubLightText.copy(alpha = 0.3f),
                                                        focusedBorderColor = EmeraldGreen,
                                                        focusedTextColor = LightText,
                                                        unfocusedTextColor = LightText
                                                    ),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                    singleLine = true
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Limite de ecrã: ${newLimitInput.toInt()}h diárias",
                                                        fontSize = 11.sp,
                                                        color = LightText
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        listOf(1f, 2f, 3f, 4f).forEach { hour ->
                                                            Card(
                                                                modifier = Modifier
                                                                    .clickable { newLimitInput = hour },
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = if (newLimitInput == hour) EmeraldGreen else SlateBackground
                                                                ),
                                                                border = BorderStroke(0.5.dp, SubLightText.copy(alpha = 0.3f)),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = "${hour.toInt()}h",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (newLimitInput == hour) SlateBackground else LightText,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = { showAddChildForm = false },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Cancelar", fontSize = 11.sp, color = LightText)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            if (newNameInput.trim().isNotEmpty() && newEmailInput.trim().isNotEmpty()) {
                                                                val succ = addChild(newNameInput.trim(), newEmailInput.trim(), newLimitInput)
                                                                if (succ) {
                                                                    Toast.makeText(context, "Filho '${newNameInput}' adicionado e focado!", Toast.LENGTH_SHORT).show()
                                                                    showAddChildForm = false
                                                                } else {
                                                                    Toast.makeText(context, "E-mail duplicado ou dados inválidos.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            } else {
                                                                Toast.makeText(context, "Por favor preencha todos os campos.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Gravar", fontSize = 11.sp, color = SlateBackground, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ao registar e focar num filho, todas as interceptações efetuadas pelo serviço e simulações do painel serão geradas e rotuladas especificamente para este filho.",
                                        fontSize = 10.sp,
                                        color = SubLightText
                                    )
                                }
                            }
                        }

                        // 2. Kid's Screen Time Usage Gauge Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Tempo de Ecrã Diário do Filho",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Custom Circular Meter Representation
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(130.dp)
                                    ) {
                                        // Background indicator circle
                                        CircularProgressIndicator(
                                            progress = 1f,
                                            modifier = Modifier.size(120.dp),
                                            color = SlateBackground,
                                            strokeWidth = 10.dp
                                        )
                                        // Progress color dynamically goes yellow/red if close or exceeding limit
                                        val ratio = childScreenTimeHours / screenTimeLimitHours
                                        val progressColor = when {
                                            ratio >= 1.0f -> AlertRed
                                            ratio >= 0.8f -> LuxuryAmber
                                            else -> EmeraldGreen
                                        }
                                        
                                        CircularProgressIndicator(
                                            progress = ratio.coerceAtMost(1f),
                                            modifier = Modifier.size(120.dp),
                                            color = progressColor,
                                            strokeWidth = 10.dp
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = String.format("%.1f h", childScreenTimeHours),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = LightText
                                            )
                                            Text(
                                                text = "Limite: ${screenTimeLimitHours.toInt()}h",
                                                fontSize = 11.sp,
                                                color = SubLightText
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Quick increase simulator for testing
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (childScreenTimeHours >= screenTimeLimitHours) "🚨 Limite diário excedido!" else "Dentro do limite autorizado",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (childScreenTimeHours >= screenTimeLimitHours) AlertRed else EmeraldGreen
                                        )

                                        // Button to simulate adding screen time
                                        Button(
                                            onClick = {
                                                val newHour = (childScreenTimeHours + 0.5f)
                                                val updated = activeChild.copy(currentScreenTime = newHour)
                                                updateActiveChild(updated)
                                                if (newHour >= screenTimeLimitHours && blockWhenLimitExceeded) {
                                                    Toast.makeText(context, "Limite atingido! Ecrã de ${activeChild.name} bloqueado remotamente.", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateBackground),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("+30 mins (Simular)", fontSize = 10.sp, color = LightText)
                                        }
                                    }

                                    // Reset simulation time button
                                    if (childScreenTimeHours > 0f) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Reiniciar tempo simulado",
                                            fontSize = 11.sp,
                                            color = SubLightText,
                                            modifier = Modifier
                                                .clickable {
                                                    val updatedProfile = activeChild.copy(currentScreenTime = 0.5f)
                                                    updateActiveChild(updatedProfile)
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Interactive Web activity simulated input
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, LuxuryAmber.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Simulador Inteligente do Filho 📱",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuxuryAmber
                                    )
                                    Text(
                                        text = "Digite abaixo um termo de pesquisa ou site (ex: 'equação matemática' ou 'porno de graça') para ver as restrições, alertas imediatos de 18+ e registo no painel.",
                                        fontSize = 11.sp,
                                        color = SubLightText,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = simulationInputText,
                                        onValueChange = { simulationInputText = it },
                                        placeholder = { Text("Pesquisa no Google ou site...", fontSize = 13.sp, color = SubLightText) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                            .testTag("child_sim_text_input"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = SubLightText.copy(alpha = 0.4f),
                                            focusedBorderColor = LuxuryAmber,
                                            focusedTextColor = LightText,
                                            unfocusedTextColor = LightText
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (simulationInputText.trim().isNotEmpty()) {
                                                // Run check
                                                val input = simulationInputText.trim().lowercase(Locale.ROOT)
                                                val adultKeywords = listOf("porno", "sexo", "pornhub", "xxx", "xvideos", "xnxx", "hentai", "brasileirinhas")
                                                val containAdult = adultKeywords.any { input.contains(it) } || 
                                                        customKeywordsString.split(",").map { it.trim().lowercase(Locale.ROOT) }.any { it.isNotEmpty() && input.contains(it) }
                                                
                                                if (automaticFilterEnabled && containAdult) {
                                                    triggerParentAlert(simulationInputText, "Adulto")
                                                } else {
                                                    viewModel.addParentLog(simulationInputText, "Seguro", false, childEmail = childEmail)
                                                    Toast.makeText(context, "Conteúdo seguro consultado!", Toast.LENGTH_SHORT).show()
                                                }
                                                simulationInputText = ""
                                            }
                                        })
                                    )

                                    Button(
                                        onClick = {
                                            if (simulationInputText.trim().isNotEmpty()) {
                                                val input = simulationInputText.trim().lowercase(Locale.ROOT)
                                                val adultKeywords = listOf("porno", "sexo", "pornhub", "xxx", "xvideos", "xnxx", "hentai", "brasileirinhas")
                                                val containAdult = adultKeywords.any { input.contains(it) } || 
                                                        customKeywordsString.split(",").map { it.trim().lowercase(Locale.ROOT) }.any { it.isNotEmpty() && input.contains(it) }
                                                
                                                if (automaticFilterEnabled && containAdult) {
                                                    triggerParentAlert(simulationInputText, "Adulto")
                                                } else {
                                                    viewModel.addParentLog(simulationInputText, "Seguro", false, childEmail = childEmail)
                                                    Toast.makeText(context, "Consultado com segurança!", Toast.LENGTH_SHORT).show()
                                                }
                                                simulationInputText = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryAmber),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_run_child_simulation"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Simular Acesso do Filho", fontSize = 12.sp, color = SlateBackground, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 4. Quick Stats summary info
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Alertas (+18) Hoje", fontSize = 10.sp, color = SubLightText)
                                        Text(
                                            text = "${parentLogs.count { it.isAlert }}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AlertRed,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Acessos Seguros", fontSize = 10.sp, color = SubLightText)
                                        Text(
                                            text = "${parentLogs.count { !it.isAlert }}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldGreen,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // LOGS HISTORY LIST (Discovering kid's internet views)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Atividades de Internet Detetadas (${parentLogs.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )

                                Text(
                                    text = "Limpar Tudo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.clearParentLogs()
                                            Toast.makeText(context, "Histórico limpo!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (parentLogs.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterFrames,
                                            contentDescription = null,
                                            tint = SubLightText,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Nenhum histórico registado ainda.",
                                            fontSize = 13.sp,
                                            color = LightText,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Use o Simulador no painel principal ou configure as chaves de monitoramento.",
                                            fontSize = 11.sp,
                                            color = SubLightText,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(parentLogs) { log ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = 0.5.dp,
                                        color = if (log.isAlert) AlertRed.copy(alpha = 0.3f) else EmeraldGreen.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (log.isAlert) AlertRed.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (log.isAlert) Icons.Default.ErrorOutline else Icons.Default.CheckCircleOutline,
                                                contentDescription = null,
                                                tint = if (log.isAlert) AlertRed else EmeraldGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (log.isAlert) "ALERTA (+18)" else "Acesso Seguro",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (log.isAlert) AlertRed else EmeraldGreen
                                                    )
                                                    if (log.childEmail.isNotEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(SubLightText.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = log.childEmail,
                                                                fontSize = 9.sp,
                                                                color = SubLightText,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                                                Text(
                                                    text = sdf.format(Date(log.timestamp)),
                                                    fontSize = 10.sp,
                                                    color = SubLightText
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = log.searchQueryOrUrl,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = LightText,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Ação: ${log.actionTaken}",
                                                fontSize = 10.sp,
                                                color = SubLightText,
                                                fontWeight = FontWeight.Light
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // RESTRICTIONS & PARENT SETTINGS
                        item {
                            Text(
                                text = "Configurações de Regra & Filtros",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // 1. Automatic 18+ Content Filter Toggle
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Bloqueador Automático +18",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LightText
                                        )
                                        Text(
                                            text = "Integra e bloqueia mais de 15,000 mil domínios conhecidos pornô em português.",
                                            fontSize = 11.sp,
                                            color = SubLightText,
                                            modifier = Modifier.padding(top = 2.dp, end = 12.dp)
                                        )
                                    }
                                    
                                    Switch(
                                        checked = automaticFilterEnabled,
                                        onCheckedChange = { isChecked ->
                                            val updated = activeChild.copy(autoFilterEnabled = isChecked)
                                            updateActiveChild(updated)
                                            Toast.makeText(context, if (isChecked) "Filtro Ativado para ${activeChild.name}!" else "Filtro desativado.", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SlateBackground,
                                            checkedTrackColor = EmeraldGreen
                                        ),
                                        modifier = Modifier.testTag("parent_toggle_block_adult")
                                    )
                                }
                            }
                        }

                        // 2. Limit Stepper Slider Configuration
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Configurar Limite de Ecrã",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText
                                    )
                                    Text(
                                        text = "Defina o número de horas permitidas por dia no smartphone do seu filho.",
                                        fontSize = 11.sp,
                                        color = SubLightText,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${screenTimeLimitHours.toInt()} Horas Diárias",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                    }

                                    Slider(
                                        value = screenTimeLimitHours,
                                        onValueChange = { newValue ->
                                            /* auto sync */
                                            val updated = activeChild.copy(screenTimeLimit = newValue)
                                             updateActiveChild(updated)
                                        },
                                        valueRange = 1f..8f,
                                        steps = 6,
                                        colors = SliderDefaults.colors(
                                            thumbColor = EmeraldGreen,
                                            activeTrackColor = EmeraldGreen,
                                            inactiveTrackColor = SlateBackground
                                        ),
                                        modifier = Modifier.testTag("parent_screen_time_slider")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bloquear ao atinjir o Limite",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = LightText
                                            )
                                            Text(
                                                text = "Restringe o ecrã do filho se o tempo passar as horas limites.",
                                                fontSize = 10.sp,
                                                color = SubLightText
                                            )
                                        }

                                        Switch(
                                            checked = blockWhenLimitExceeded,
                                            onCheckedChange = { isChecked ->
                                                /* sync */
                                                val updated = activeChild.copy(blockExceeded = isChecked)
                                                updateActiveChild(updated)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = SlateBackground,
                                                checkedTrackColor = EmeraldGreen
                                            ),
                                            modifier = Modifier.testTag("parent_toggle_lock_exceeded")
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Custom Blacklist / Keywords manager
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CardBackground)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Restringir Palavras Customizadas",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText
                                    )
                                    Text(
                                        text = "Adicione palavras ou redes sociais que deseja proibir que seu filho procure (ex: facebook, tinder).",
                                        fontSize = 11.sp,
                                        color = SubLightText,
                                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newKeywordInputText,
                                            onValueChange = { newKeywordInputText = it },
                                            placeholder = { Text("palavra-chave...", fontSize = 12.sp, color = SubLightText) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("new_keyword_text_input"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = SubLightText.copy(alpha = 0.3f),
                                                focusedBorderColor = EmeraldGreen,
                                                focusedTextColor = LightText,
                                                unfocusedTextColor = LightText
                                            ),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                        )

                                        Button(
                                            onClick = {
                                                val cleanVal = newKeywordInputText.trim().lowercase(Locale.ROOT)
                                                if (cleanVal.isNotEmpty()) {
                                                    val parts = customKeywordsString.split(",")
                                                        .map { it.trim().lowercase(Locale.ROOT) }
                                                        .filter { it.isNotEmpty() }
                                                        .toMutableList()
                                                    
                                                    if (!parts.contains(cleanVal)) {
                                                        parts.add(cleanVal)
                                                    }
                                                    customKeywordsString = parts.joinToString(", ")
                                                    sharedPrefs.edit().putString("parent_custom_keywords", customKeywordsString).apply()
                                                    newKeywordInputText = ""
                                                    Toast.makeText(context, "Palavra-chave bloqueada!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                            modifier = Modifier
                                                .height(50.dp)
                                                .testTag("btn_add_restr_keyword")
                                        ) {
                                            Text("Bloquear", fontSize = 12.sp, color = SlateBackground, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Lista Ativa:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LightText
                                    )

                                    // Display list items as horizontal wrap rows
                                    val keywordsList = customKeywordsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    if (keywordsList.isEmpty()) {
                                        Text(
                                            text = "Nenhuma restrição especial.",
                                            fontSize = 11.sp,
                                            color = SubLightText,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            keywordsList.forEach { kw ->
                                                SuggestionChip(
                                                    onClick = {
                                                        // Remove it on click
                                                        val updated = keywordsList.toMutableList().apply { remove(kw) }
                                                        customKeywordsString = updated.joinToString(", ")
                                                        sharedPrefs.edit().putString("parent_custom_keywords", customKeywordsString).apply()
                                                        Toast.makeText(context, "Desbloqueado: $kw", Toast.LENGTH_SHORT).show()
                                                    },
                                                    label = {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(text = kw, fontSize = 11.sp, color = LightText)
                                                            Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(12.dp), tint = AlertRed)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = SlateBackground
                                                    ),
                                                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
