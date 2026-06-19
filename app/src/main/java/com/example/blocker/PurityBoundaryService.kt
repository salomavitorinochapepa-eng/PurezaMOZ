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

    override fun onServiceConnected() {
        super.onServiceConnected()
        refreshBlockerStatus()
    }

    private fun refreshBlockerStatus() {
        serviceScope.launch {
            try {
                val db = VencerDatabase.getInstance(applicationContext)
                val settings = db.dao.getBlockerSettings()
                isBlockerActive = settings?.isBlockerEnabled ?: false
                isStrictMode = settings?.strictMode ?: false
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
        // Refresh status dynamically
        refreshBlockerStatus()

        if (!isBlockerActive) return

        val rootNode = rootInActiveWindow ?: return

        val matchedKeyword = findAdultKeywordTrigger(rootNode)
        if (matchedKeyword != null) {
            val now = System.currentTimeMillis()
            if (now - lastTriggeredTime > 3000) { // 3s cooling threshold
                lastTriggeredTime = now
                triggerBlockOverlay()
                // Save log to Parent Database Control
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
    }

    private fun findAdultKeywordTrigger(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null

        // Check text of this node
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val combined = "$text $contentDesc".lowercase(Locale.ROOT)

        val keywords = mutableListOf(
            "xvideos", "pornhub", "xnxx", "redtube", "hentai", "sexo gratis", 
            "xxx video", "pornho", "porno", "mulheres nuas", "filme adulto",
            "videos de sexo", "brasileirinhas", "pornstar"
        )

        if (isStrictMode) {
            keywords.addAll(listOf(
                "cam4", "chaturbate", "stripchat", "onlyfans", "sexy girls", "sexy videos", "beeg", 
                "spankbang", "eporner", "livejasmin", "nude", "nudez", "erotico", "erotica", "gostosas", 
                "playboy", "adult content", "pussy", "dick", "foder", "foda", "buceta", "caralho", "pica", 
                "mama", "mamar", "masturbacao", "masturbar", "sexo", "hentay", "sexosemparar", "comogurlabloqueio",
                "desativar guardiao", "desativar pureza", "burlar bloqueio", "adult sites", "porno gratis"
            ))
        }

        for (kw in keywords) {
            if (combined.contains(kw)) {
                val displayMsg = text.ifEmpty { contentDesc }.ifEmpty { kw }
                node.recycle()
                return displayMsg
            }
        }

        // Recursively inspect children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val matched = findAdultKeywordTrigger(child)
            if (matched != null) {
                return matched
            }
        }
        return null
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
