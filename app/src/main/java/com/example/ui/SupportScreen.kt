package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.notification.InspirationNotificationHelper
import com.example.notification.InspirationMessage
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ForumPost
import com.example.ui.theme.*

// Helper to format timestamps locally
private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Agora mesmo"
        diff < 3600_000L -> "Há ${diff / 60_000L} min"
        diff < 86400_000L -> "Há ${diff / 3600_000L} h"
        else -> "Há ${diff / 86400_000L} d"
    }
}

// Support Article Model
data class SupportArticle(
    val title: String,
    val category: String,
    val readTime: String,
    val summary: String,
    val fullContent: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: VencerViewModel
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Artigos, 1: Dicas, 2: Fé & Alertas, 3: Fórum
    val forumPosts by viewModel.forumPosts.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("vencer_prefs", Context.MODE_PRIVATE) }
    var dailyNotificationsEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("daily_notifications", false))
    }
    var activeFeaturedMessage by remember {
        mutableStateOf<InspirationMessage?>(InspirationNotificationHelper.messages.first())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dailyNotificationsEnabled = true
            sharedPrefs.edit().putBoolean("daily_notifications", true).apply()
            InspirationNotificationHelper.scheduleDailyNotification(context)
            Toast.makeText(context, "Notificações diárias ativadas com sucesso!", Toast.LENGTH_LONG).show()
            val welcomeMsg = InspirationMessage(
                99,
                "Excelente! Agora você receberá notificações diárias com versículos e força para guiar o seu caminho à liberdade 🕊️",
                "Vencer",
                false
            )
            InspirationNotificationHelper.showInspirationNotification(context, welcomeMsg)
        } else {
            dailyNotificationsEnabled = false
            sharedPrefs.edit().putBoolean("daily_notifications", false).apply()
            Toast.makeText(context, "Permissão necessária para enviar mensagens diárias de suporte.", Toast.LENGTH_LONG).show()
        }
    }

    // Detailed Article Dialog State
    var activeArticleDetail by remember { mutableStateOf<SupportArticle?>(null) }

    val articles = remember {
        listOf(
            SupportArticle(
                title = "O Ciclo da Dopamina e o Cérebro",
                category = "Neurociência",
                readTime = "5 min",
                summary = "Compreenda como o consumo constante de pornografia sequestra os recetores de dopamina do seu cérebro, diminuindo o seu foco e força de vontade diária.",
                fullContent = """
                    A dopamina é o neurotransmissor da antecipação e da recompensa. Quando exposto a estímulos visuais hiper-estimulantes como a pornografia, o cérebro recebe descargas maciças de dopamina em níveis artificialmente elevados que nunca seriam replicados no mundo real.
                    
                    Com o tempo, para se proteger do excesso de estímulos, o cérebro reduz o número de recetores ativos de dopamina. Este fenómeno chama-se 'dessensibilização'. É por isso que você passa a sentir falta de motivação, apatia generalizada, fadiga mental crónica e perda de interesse nas pequenas alegrias da vida (comer bem, socializar com amigos ou trabalhar nos seus projetos).
                    
                    A boa notícia é que o cérebro possui neuroplasticidade: ele consegue regenerar-se por completo! Ao iniciar e proteger o seu rastreador de sobriedade de pornografia, os recetores começam a recuperar entre 2 a 3 semanas. Dentro de 90 dias, a sensibilidade dopaminérgica geralmente regressa aos níveis saudáveis, restaurando o seu foco lendário, lucidez e vitalidade emocional. Cada dia limpo é uma valiosa peça reconstruída!
                """.trimIndent(),
                color = EmeraldGreen
            ),
            SupportArticle(
                title = "Parcerias de Responsabilidade: Quebre o Segredo",
                category = "Psicofisiologia",
                readTime = "4 min",
                summary = "O vício alimenta-se de segredos e isolamento. Saiba por que partilhar a sua dor (mesmo anonimamente) desarma de imediato a urges compulsivos.",
                fullContent = """
                    A pornografia prospera na escuridão do segredo. Quando você luta sozinho, o peso da vergonha após uma recaída cria uma espiral de isolamento que o empurra novamente para o vício como forma anestésica de escapar às emoções negativas.
                    
                    Na psicologia conductual, expor os impulsos mentais a uma comunidade de suporte ou parceiro de responsabilidade retira o poder de 'sedução imediata' que o gatilho tem. Ao traduzir o impulso secreto em palavras explícitas (por exemplo, escrevendo no fórum anónimo do Vencer ou contando a um amigo genuíno), você retira a sua mística.
                    
                    Partilhar tira de cima de si a máscara de perfeição e o alivia da ansiedade mental de fingimento. Neste aplicativo, por entender que em Moçambique falar sobre isto envolve tabus gigantes, oferecemos um fórum de partilha 100% privado, sem registos ou rastreadores de IP, permitindo-lhe expressar a sua dor e ver que centenas de compatriotas travam exatamente a mesma batalha. Você não está condenado e nunca estará sozinho!
                """.trimIndent(),
                color = CalmTeal
            ),
            SupportArticle(
                title = "Como as Barreiras Digitais Salvam Vontades",
                category = "Estratégia Prática",
                readTime = "6 min",
                summary = "A força de vontade gasta-se ao longo do dia. Descubra como erguer fortificações no seu telemóvel e router para evitar recaídas fáceis.",
                fullContent = """
                    Confiar apenas na 'força de vontade' para fugir da tentação é um erro fatal de engenharia pessoal. Durante a noite, após um dia fatigante de aulas ou trabalho em Maputo, ou quando a solidão aperta, o córtex pré-frontal (responsável pela tomada de decisão lógica) encontra-se enfraquecido.
                    
                    É aqui que entram as barreiras digitais auto-impostas: elas não substituem o seu caráter, mas compram o tempo precioso de que o seu cérebro necessita para arrefecer quando o impulso ataca.
                    
                    Recomendações fundamentais para construir as suas barreiras:
                    1. Utilize o Guardião (Bloqueador Inteligente) integrado no Vencer para restringir o acesso a navegadores privados e sites perigosos.
                    2. Configure DNS Familiares: Altere as definições de DNS de rede do seu telemóvel (Definições > Ligações > DNS Privado) para 'family.adguard-dns.com'. Ele intercepta automaticamente e bloqueia pedidos a conteúdos impróprios antes que eles carreguem no ecrã.
                    3. Mantenha os seus dispositivos de ecrã grande (computadores, tablets) fora do quarto à noite. O quarto de dormir deve servir unicamente para descansar e rejuvenescer o corpo.
                """.trimIndent(),
                color = LuxuryAmber
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Header showing local privacy guarantees
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CardBackground.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                tint = EmeraldGreen,
                                contentDescription = "Segurança Máxima"
                            )
                        }
                        Column {
                            Text(
                                text = "Apoio & Solidariedade",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Text(
                                text = "Espaço 100% Confidencial",
                                fontSize = 12.sp,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sua privacidade é o nosso pilar. Todo o conteúdo, incluindo o fórum, funciona de forma segura localmente no seu dispositivo, sem servidores centrais que possam expor os seus dados pessoais ou luta contra a pornografia.",
                        fontSize = 12.sp,
                        color = SubLightText,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 2. Navigation Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val subTabs = listOf(
                    Triple(0, "Artigos", Icons.Default.MenuBook),
                    Triple(1, "Dicas Gatilhos", Icons.Default.Lightbulb),
                    Triple(2, "Fé & Alertas", Icons.Default.NotificationsActive),
                    Triple(3, "Fórum Anónimo", Icons.Default.Forum)
                )

                subTabs.forEach { (index, title, icon) ->
                    val isSelected = selectedSubTab == index
                    Card(
                        modifier = Modifier
                            .width(115.dp)
                            .clickable { selectedSubTab = index }
                            .testTag("support_pill_$index"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) EmeraldGreen else CardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) SlateBackground else SubLightText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SlateBackground else SubLightText,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Sub Tab Content
        when (selectedSubTab) {
            0 -> { // ARTICLES TAB
                items(articles) { article ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { activeArticleDetail = article }
                            .testTag("article_card_${article.title.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = article.category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = article.color
                                )
                                Text(
                                    text = "Leitura: ${article.readTime}",
                                    fontSize = 11.sp,
                                    color = SubLightText
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = article.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = article.summary,
                                fontSize = 12.sp,
                                color = SubLightText,
                                lineHeight = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Ler Artigo Inteiro",
                                    fontSize = 12.sp,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            1 -> { // PRACTICAL TRIGGERS AND TIPS TAB
                val tips = listOf(
                    Triple(
                        "1. A Regra Inabalável dos 10 Minutos",
                        "Quando a urges/tentação surgir de rompante, faça um acordo com o seu cérebro: 'Vou esperar 10 minutos completos antes de fazer qualquer escolha ruim'.",
                        "Durante esses 10 minutos, respire fundo usando a técnica Box Breathing (inspira 4s, segura 4s, expira 4s). Em 90% dos casos, o impulso perde o seu pico intenso."
                    ),
                    Triple(
                        "2. Choque Térmico de Desvio Físico",
                        "Os pensamentos obsessivos alimentam o impulso. O seu corpo físico precisa de quebrar o ciclo cerebral de imediato de forma urgente.",
                        "Vá diretamente à casa de banho e atire água completamente fria à cara ou tome um banho frio rápido de sobressalto. Isso altera a sua temperatura física e afugenta a obsessão mental."
                    ),
                    Triple(
                        "3. Modificação Radical do Ambiente",
                        "Você nunca vencerá a tentação se continuar sentado na mesma cadeira preta e escuridão que o ligou aos maus hábitos do passado.",
                        "No segundo em que o gatilho se activar, levante-se! Saia do quarto, mude de divisão, vá à rua beber um copo de água na mercearia ou fale com alguém na sua sala de estar."
                    ),
                    Triple(
                        "4. Desafio Mental de Custo Real",
                        "Lembre-se sempre de que o impulso de assistir pornografia é uma mentira passageira com custos pesados à sua vida.",
                        "A nossa mente gasta dinheiro em recargas de megas pesadas (mais de 300 MT por semana) e drena energia divina. Recorde-se: Você está no comando e é forte!"
                    )
                )

                items(tips) { (title, intro, details) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    tint = LuxuryAmber,
                                    contentDescription = "Dica"
                                )
                                Text(
                                    text = title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightText
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = intro,
                                fontSize = 12.sp,
                                color = LightText.copy(alpha = 0.9f),
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = details,
                                fontSize = 11.sp,
                                color = SubLightText,
                                lineHeight = 16.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
            2 -> { // FAITH & ALERTS TAB (Inspirations & Verses)
                // 1. Notification Toggle Config Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Alertas de Motivação & Fé",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LightText
                                        )
                                        Text(
                                            text = "Encorajamento diário às 08:00 AM",
                                            fontSize = 11.sp,
                                            color = SubLightText
                                        )
                                    }
                                }

                                Switch(
                                    checked = dailyNotificationsEnabled,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                val permissionCheck = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                )
                                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                    dailyNotificationsEnabled = true
                                                    sharedPrefs.edit().putBoolean("daily_notifications", true).apply()
                                                    InspirationNotificationHelper.scheduleDailyNotification(context)
                                                    Toast.makeText(context, "Notificações diárias ativadas!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            } else {
                                                dailyNotificationsEnabled = true
                                                sharedPrefs.edit().putBoolean("daily_notifications", true).apply()
                                                InspirationNotificationHelper.scheduleDailyNotification(context)
                                                Toast.makeText(context, "Notificações diárias ativadas!", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            dailyNotificationsEnabled = false
                                            sharedPrefs.edit().putBoolean("daily_notifications", false).apply()
                                            InspirationNotificationHelper.cancelDailyNotification(context)
                                            Toast.makeText(context, "Notificações diárias desativadas.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SlateBackground,
                                        checkedTrackColor = EmeraldGreen,
                                        uncheckedThumbColor = SubLightText,
                                        uncheckedTrackColor = CardBackground.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.testTag("notification_toggle")
                                )
                            }
                        }
                    }
                }

                // 2. On-Demand Immediate Encouragement Pill Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Pílula de Socorro Imediato ⚡",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxuryAmber
                            )
                            Text(
                                text = "Está a sentir fraqueza agora? Clique abaixo para receber uma dose imediata de força e proteção mental no seu ecrã e notificações.",
                                fontSize = 11.sp,
                                color = SubLightText,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val verses = InspirationNotificationHelper.messages.filter { it.isBibleVerse }
                                        val randomVerse = verses.random()
                                        activeFeaturedMessage = randomVerse
                                        InspirationNotificationHelper.showInspirationNotification(context, randomVerse)
                                        Toast.makeText(context, "Pílula de Fé enviada!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("get_faith_pill_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                        Text("Fé (Versículo)", fontSize = 11.sp, color = LightText, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val quotes = InspirationNotificationHelper.messages.filter { !it.isBibleVerse }
                                        val randomQuote = quotes.random()
                                        activeFeaturedMessage = randomQuote
                                        InspirationNotificationHelper.showInspirationNotification(context, randomQuote)
                                        Toast.makeText(context, "Pílula de Motivação enviada!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryAmber.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("get_motivation_pill_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, LuxuryAmber.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LuxuryAmber, modifier = Modifier.size(16.dp))
                                        Text("Motivação", fontSize = 11.sp, color = LightText, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Featured Blindagem Message Card
                activeFeaturedMessage?.let { msg ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (msg.isBibleVerse) EmeraldGreen.copy(alpha = 0.4f) else LuxuryAmber.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (msg.isBibleVerse) "🕊️ SHALOM MENSAGEM" else "🛡️ FORTALEZA DIÁRIA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.isBibleVerse) EmeraldGreen else LuxuryAmber
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (msg.isBibleVerse) EmeraldGreen.copy(alpha = 0.1f) else LuxuryAmber.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = msg.authorOrSource,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isBibleVerse) EmeraldGreen else LuxuryAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "\"${msg.text}\"",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LightText,
                                    lineHeight = 22.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "Guarde esta palavra no seu coração hoje. A sobriedade reconstrói o seu caráter divino.",
                                    fontSize = 10.sp,
                                    color = SubLightText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 4. Spiritual Armor message archives
                item {
                    Text(
                        text = "Arquivo de Blindagem Espiritual",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                items(InspirationNotificationHelper.messages) { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable {
                                activeFeaturedMessage = msg
                                Toast.makeText(context, "Carregado como Blindagem Ativa!", Toast.LENGTH_SHORT).show()
                            },
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, if (msg.isBibleVerse) EmeraldGreen.copy(alpha = 0.15f) else LuxuryAmber.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (msg.isBibleVerse) EmeraldGreen.copy(alpha = 0.1f) else LuxuryAmber.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (msg.isBibleVerse) Icons.Default.MenuBook else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (msg.isBibleVerse) EmeraldGreen else LuxuryAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (msg.isBibleVerse) "Versículo Bíblico" else "Frase de Força",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (msg.isBibleVerse) EmeraldGreen else LuxuryAmber
                                    )
                                    Text(
                                        text = msg.authorOrSource,
                                        fontSize = 10.sp,
                                        color = SubLightText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    color = LightText,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            3 -> { // ANONYMOUS FORUM TAB
                // Section Header and Input Fields
                item {
                    var postContent by remember { mutableStateOf("") }
                    var authorName by remember { mutableStateOf("") }
                    var locationName by remember { mutableStateOf("") }
                    
                    val provinces = remember {
                        listOf("Maputo", "Sofala/Beira", "Nampula", "Tete", "Zambézia", "Gaza", "Inhambane", "Cabo Delgado", "Niassa", "Manica")
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mural de Solidariedade",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Text(
                                text = "Partilhe a sua dor ou vitória localmente. O seu post é guardado de forma totalmente anónima.",
                                fontSize = 11.sp,
                                color = SubLightText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = postContent,
                                onValueChange = { postContent = it },
                                placeholder = { Text("Escreva aqui o seu testemunho ou desabafo...", fontSize = 13.sp, color = SubLightText) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .testTag("forum_post_input"),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldGreen,
                                    unfocusedBorderColor = CardBackground.copy(alpha = 0.8f),
                                    focusedTextColor = LightText,
                                    unfocusedTextColor = LightText
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = authorName,
                                    onValueChange = { authorName = it },
                                    placeholder = { Text("Apelido (ex: Guerreiro)", fontSize = 11.sp, color = SubLightText) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("forum_author_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        focusedTextColor = LightText,
                                        unfocusedTextColor = LightText
                                    )
                                )

                                OutlinedTextField(
                                    value = locationName,
                                    onValueChange = { locationName = it },
                                    placeholder = { Text("Província/Cidade", fontSize = 11.sp, color = SubLightText) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("forum_location_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldGreen,
                                        focusedTextColor = LightText,
                                        unfocusedTextColor = LightText
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic Generator Button purely for Mozambican protection and ease of use
                                TextButton(
                                    onClick = {
                                        val randomWarriors = listOf("Lutador", "Guerreiro", "Escudo", "Vencedor", "Sobriedade", "Espírito Livre")
                                        val randomLocal = provinces.random()
                                        authorName = "${randomWarriors.random()}"
                                        locationName = randomLocal
                                    },
                                    modifier = Modifier.testTag("forum_generate_alias_btn")
                                ) {
                                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(14.dp), tint = LuxuryAmber)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gerar Pseudónimo", fontSize = 10.sp, color = LuxuryAmber, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (postContent.trim().isNotEmpty()) {
                                            val finalAuthor = if (authorName.trim().isEmpty()) "Lutador Anónimo" else authorName.trim()
                                            val finalLoc = if (locationName.trim().isEmpty()) "Moçambique" else locationName.trim()
                                            viewModel.publishPost(
                                                author = finalAuthor,
                                                content = postContent,
                                                location = finalLoc
                                            )
                                            postContent = ""
                                            authorName = ""
                                            locationName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("forum_submit_btn"),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Enviar",
                                        tint = SlateBackground,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Publicar", color = SlateBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // If feed is empty
                if (forumPosts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground)
                        ) {
                            Text(
                                "Sem mensagens no mural ainda. Seja o primeiro a apoiar!",
                                color = SubLightText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(forumPosts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("forum_post_card_${post.id}"),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(EmeraldGreen, CalmTeal)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = post.author.take(1).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SlateBackground
                                            )
                                        }

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = post.author,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LightText
                                                )
                                                Text(
                                                    text = "(${post.location})",
                                                    fontSize = 11.sp,
                                                    color = EmeraldGreen,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Text(
                                                text = formatTimeAgo(post.timestamp),
                                                fontSize = 10.sp,
                                                color = SubLightText
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = post.content,
                                    fontSize = 12.sp,
                                    color = LightText,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { viewModel.supportPost(post.id) },
                                        modifier = Modifier
                                            .height(32.dp)
                                            .testTag("support_post_btn_${post.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .background(
                                                    color = EmeraldGreen.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Apoiar",
                                                tint = AlertRed,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Dar Força (${post.supportsCount})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LightText
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

    // Interactive Article overlay description
    activeArticleDetail?.let { article ->
        AlertDialog(
            onDismissRequest = { activeArticleDetail = null },
            confirmButton = {
                Button(
                    onClick = { activeArticleDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("close_article_dialog")
                ) {
                    Text("Concluir Leitura", color = SlateBackground, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Column {
                    Text(
                        text = article.category.uppercase(),
                        fontSize = 10.sp,
                        color = article.color,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Text(
                        text = article.fullContent,
                        fontSize = 13.sp,
                        color = LightText.copy(alpha = 0.9f),
                        lineHeight = 19.sp
                    )
                }
            },
            containerColor = CardBackground,
            titleContentColor = LightText,
            textContentColor = LightText
        )
    }
}
