package com.example.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.MainActivity
import com.example.data.VencerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class PurityBoundaryService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var isBlockerActive = false
    private var isStrictMode = false
    private var lastTriggeredTime = 0L

    // Substring keywords: Block if they appear anywhere, even inside other words (e.g. pornhub)
    private val substringKeywords = listOf(
        "xvideos", "pornhub", "xnxx", "redtube", "xhamster", "youporn", "beeg", 
        "spankbang", "eporner", "onlyfans", "chaturbate", "stripchat", "cam4", 
        "livejasmin", "brasileirinhas", "pornstar", "hentai", "hentay", "sexosemparar"
    )

    // Whole-word keywords: Only block if they appear as standalone words to prevent false positives (e.g. sexta-feira, sexual)
    private val wholeWordKeywords = listOf(
        "sex", "sexo", "porn", "porno", "xxx", "pornografia", "pornography", 
        "nude", "nudez", "pussy", "dick", "foder", "fodendo", "buceta", "caralho", 
        "pica", "putas", "kengas", "gajas nuas", "gaja nua", "mulheres nuas", "mulher nua",
        "videos de sexo", "video de sexo", "filme adulto", "filmes de sexo", "videos caseiros", 
        "video caseiro", "mamar", "esporrar", "gozar", "punheta", "ejaculacao", "orgasmo"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Collect Flow to keep blocker status dynamically in sync without database queries inside onAccessibilityEvent
        serviceScope.launch {
            try {
                val db = VencerDatabase.getInstance(applicationContext)
                db.dao.getBlockerSettingsFlow().collect { settings ->
                    isBlockerActive = settings?.isBlockerEnabled ?: false
                    isStrictMode = settings?.strictMode ?: false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getStoredChildEmail(): String {
        return try {
            val prefs = getSharedPreferences("vencer_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getString("parent_child_email", "eduardo@gmail.com") ?: "eduardo@gmail.com"
        } catch (e: Exception) {
            "eduardo@gmail.com"
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val prefs = getSharedPreferences("vencer_prefs", android.content.Context.MODE_PRIVATE)
        val isPremiumUnlocked = prefs.getBoolean("is_premium_unlocked", false)
        if (!isPremiumUnlocked || !isBlockerActive) return

        // 1. Quick check on the event's direct text properties (super fast and lightweight)
        val eventTexts = event.text
        if (eventTexts != null) {
            for (t in eventTexts) {
                val tStr = t?.toString() ?: ""
                val matched = findAdultKeywordInString(tStr)
                if (matched != null) {
                    triggerBlock(matched)
                    return
                }
            }
        }

        val contentDesc = event.contentDescription?.toString()
        if (contentDesc != null) {
            val matched = findAdultKeywordInString(contentDesc)
            if (matched != null) {
                triggerBlock(matched)
                return
            }
        }

        // 2. Inspect the source node of the event
        val sourceNode = event.source
        if (sourceNode != null) {
            val matchedKeyword = findAdultKeywordTrigger(sourceNode)
            if (matchedKeyword != null) {
                sourceNode.recycle()
                triggerBlock(matchedKeyword)
                return
            }
            sourceNode.recycle()
        }

        // 3. Fallback to full active window hierarchy inspection
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            val matchedKeyword = findAdultKeywordTrigger(rootNode)
            if (matchedKeyword != null) {
                rootNode.recycle()
                triggerBlock(matchedKeyword)
                return
            }
            rootNode.recycle()
        }
    }

    private fun findAdultKeywordTrigger(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        // Check text of this node
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        
        val matchedText = findAdultKeywordInString(text)
        if (matchedText != null) return matchedText
        
        val matchedDesc = findAdultKeywordInString(contentDesc)
        if (matchedDesc != null) return matchedDesc

        // Recursively inspect children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val matched = findAdultKeywordTrigger(child)
            if (matched != null) {
                child.recycle()
                return matched
            }
            child.recycle()
        }
        return null
    }

    private fun findAdultKeywordInString(input: String): String? {
        if (input.trim().isEmpty()) return null
        val normalized = normalizeText(input)

        // A. Defeat spaces/punctuation bypasses by checking spacing-removed version (e.g. p.o.r.n.h.u.b)
        val spacingRemoved = normalized.replace(Regex("[^a-z0-9]"), "")
        for (kw in substringKeywords) {
            if (spacingRemoved.contains(kw)) {
                return kw
            }
        }

        // B. Standard substring check
        for (kw in substringKeywords) {
            if (normalized.contains(kw)) {
                return kw
            }
        }

        // C. Standalone whole-word checks to avoid false positives (like "sexta", "sexual")
        for (kw in wholeWordKeywords) {
            if (isWordMatch(normalized, kw)) {
                return kw
            }
        }

        // D. Additional strict keywords if parent strict mode is activated
        if (isStrictMode) {
            val strictSubstring = listOf("cam4", "chaturbate", "stripchat", "beeg")
            val strictWholeWords = listOf(
                "sexy", "nude", "nudez", "erotico", "erotica", "gostosas", 
                "gostosa", "playboy", "adult content", "mama", "masturbacao", 
                "masturbar", "desativar guardiao", "desativar pureza", 
                "burlar bloqueio", "adult sites"
            )
            for (kw in strictSubstring) {
                if (normalized.contains(kw) || spacingRemoved.contains(kw)) return kw
            }
            for (kw in strictWholeWords) {
                if (isWordMatch(normalized, kw)) return kw
            }
        }

        return null
    }

    private fun isWordMatch(text: String, keyword: String): Boolean {
        var index = text.indexOf(keyword)
        while (index != -1) {
            // Check start boundary
            var startOk = true
            if (index > 0) {
                val charBefore = text[index - 1]
                if (charBefore.isLetterOrDigit()) {
                    startOk = false
                }
            }
            
            // Check end boundary
            var endOk = true
            val endIndex = index + keyword.length
            if (endIndex < text.length) {
                val charAfter = text[endIndex]
                if (charAfter.isLetterOrDigit()) {
                    endOk = false
                }
            }
            
            if (startOk && endOk) {
                return true
            }
            
            index = text.indexOf(keyword, index + 1)
        }
        return false
    }

    private fun normalizeText(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace('ç', 'c')
            .replace('á', 'a')
            .replace('à', 'a')
            .replace('ã', 'a')
            .replace('â', 'a')
            .replace('é', 'e')
            .replace('ê', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ô', 'o')
            .replace('õ', 'o')
            .replace('ú', 'u')
            .replace('ü', 'u')
    }

    private fun triggerBlock(matchedKeyword: String) {
        val now = System.currentTimeMillis()
        if (now - lastTriggeredTime > 1500) { // Responsive 1.5s cooling threshold
            lastTriggeredTime = now
            
            // 1. Instantly perform the HOME global action to minimize the browser/app immediately
            performGlobalAction(GLOBAL_ACTION_HOME)
            
            // 2. Launch the recovery UI overlay of MainActivity
            triggerBlockOverlay()
            
            // 3. Save the intercept details to Parent Database Control
            val childEmail = getStoredChildEmail()
            serviceScope.launch {
                try {
                    val db = VencerDatabase.getInstance(applicationContext)
                    db.dao.insertParentLog(
                        com.example.data.ParentLog(
                            timestamp = System.currentTimeMillis(),
                            searchQueryOrUrl = "Acesso a: \"$matchedKeyword\"",
                            actionTaken = "Bloqueado",
                            isAlert = true,
                            childEmail = childEmail
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun triggerBlockOverlay() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("BLOCK_TRIGGERED", true)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
