package org.autojs.autojs.core.voiceinteraction

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log
import androidx.annotation.RequiresApi
import org.autojs.autojs.ui.main.drawer.DrawerFragment
import org.greenrobot.eventbus.EventBus

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * VoiceInteractionService implementation for AutoJs6.
 * This service provides access to window content similar to AccessibilityService.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VoiceInteractionServiceUsher : VoiceInteractionService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "VoiceInteractionService created")
        EventBus.getDefault().post(DrawerFragment.Companion.Event.VoiceInteractionServiceStateChangedEvent::class.java)
    }

    override fun onReady() {
        super.onReady()
        Log.d(TAG, "VoiceInteractionService ready")
        isReady = true
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.d(TAG, "VoiceInteractionService shutdown")
        instance = null
        isReady = false
        EventBus.getDefault().post(DrawerFragment.Companion.Event.VoiceInteractionServiceStateChangedEvent::class.java)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isReady = false
        Log.d(TAG, "VoiceInteractionService destroyed")
        EventBus.getDefault().post(DrawerFragment.Companion.Event.VoiceInteractionServiceStateChangedEvent::class.java)
    }
    
    /**
     * 显示 Session 并请求 AssistStructure
     * 使用 SHOW_WITH_ASSIST 标志来获取当前屏幕的 AssistStructure
     * 
     * 注意：SHOW_SOURCE_ACTIVITY (4) 标志表示请求来自 Activity
     * 这可能有助于系统更好地处理焦点
     * 
     * @param showFlags 显示标志，默认 SHOW_WITH_ASSIST | SHOW_SOURCE_ACTIVITY
     */
    fun showSessionForAssist(showFlags: Int = VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_SOURCE_ACTIVITY) {
        Log.d(TAG, "showSessionForAssist called with flags: $showFlags")
        try {
            // 使用 Bundle 传递参数，告诉 Session 这是静默请求
            val args = Bundle().apply {
                putBoolean("silent_mode", true)
                putBoolean("show_source_application", true)
            }
            showSession(args, showFlags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show session for assist", e)
        }
    }

    companion object {

        private val TAG = VoiceInteractionServiceUsher::class.java.simpleName

        @Volatile
        private var instance: VoiceInteractionServiceUsher? = null
        
        @Volatile
        var isReady: Boolean = false
            private set

        @JvmStatic
        fun getInstance(): VoiceInteractionServiceUsher? = instance

        @JvmStatic
        fun hasInstance(): Boolean = instance != null

    }

}
