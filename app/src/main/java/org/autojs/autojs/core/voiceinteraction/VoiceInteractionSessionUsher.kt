package org.autojs.autojs.core.voiceinteraction

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * VoiceInteractionSession implementation for AutoJs6.
 * This session can access window content through AssistStructure.
 * 
 * 重要说明：
 * AssistStructure 只有在以下情况下才会被填充：
 * 1. 用户长按 Home 键触发数字助理
 * 2. 调用 show() 方法并设置 SHOW_WITH_ASSIST 标志
 * 
 * 与 AccessibilityService 不同，数字助理无法实时监听屏幕变化。
 * 
 * 回调方法说明：
 * - onHandleAssist(Bundle?, AssistStructure?, AssistContent?) - API 23+
 * - onHandleAssist(AssistState) - API 29+ (Android Q)
 * 
 * 注意：此 Session 不显示任何 UI，只用于获取 AssistStructure
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VoiceInteractionSessionUsher(context: Context) : VoiceInteractionSession(context) {

    private val mContext: Context = context

    init {
        instance = WeakReference(this)
        Log.d(TAG, "VoiceInteractionSession created, API level: ${Build.VERSION.SDK_INT}")
    }
    
    /**
     * 不创建任何 UI，返回 null 使助手界面透明/不可见
     * 这样获取到的 AssistStructure 就是目标页面的控件，而不是助手界面的控件
     */
    override fun onCreateContentView(): View? {
        Log.d(TAG, "onCreateContentView: returning null for invisible UI")
        return null
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "VoiceInteractionSession onShow, showFlags: $showFlags, args: $args")
        isSessionShowing = true
        
        // 检查是否是静默模式（由 showSessionForAssist 触发）
        val isSilentMode = args?.getBoolean("silent_mode", false) ?: false
        Log.d(TAG, "Silent mode: $isSilentMode, autoHideAfterAssist: $autoHideAfterAssist")
        
        // 如果是静默模式，设置自动隐藏标志
        // 实际隐藏会在 handleAssistStructure 收到数据后立即执行
        if (isSilentMode) {
            autoHideAfterAssist = true
        }
    }

    override fun onHide() {
        super.onHide()
        Log.d(TAG, "VoiceInteractionSession onHide")
        isSessionShowing = false
    }

    /**
     * API 23+ (Android M) 的回调方法
     * 在 API 29 以下会被调用
     */
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)
        Log.d(TAG, "onHandleAssist (API 23) - structure: $structure, windowCount: ${structure?.windowNodeCount ?: 0}")
        
        handleAssistStructure(structure, content)
    }

    /**
     * API 29+ (Android Q) 的回调方法
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        val assistStructure = state.assistStructure
        val assistContent = state.assistContent

        Log.d(TAG, "onHandleAssist (API 29) - assistStructure: $assistStructure")
        Log.d(TAG, "onHandleAssist (API 29) - windowNodeCount: ${assistStructure?.windowNodeCount ?: 0}")
        Log.d(TAG, "onHandleAssist (API 29) - isFocused: ${state.isFocused}")

        handleAssistStructure(assistStructure, assistContent)
    }

    /**
     * 处理 AssistStructure 的通用方法
     */
    private fun handleAssistStructure(structure: AssistStructure?, content: AssistContent?) {
        if (structure != null) {
            currentAssistStructure = structure
            lastAssistContent = content
            Log.d(TAG, "AssistStructure saved successfully, windowCount: ${structure.windowNodeCount}")

            // 打印结构信息用于调试
            for (i in 0 until structure.windowNodeCount) {
                val windowNode = structure.getWindowNodeAt(i)
                Log.d(TAG, "Window $i: title=${windowNode.title}, rootViewNode=${windowNode.rootViewNode}")
                windowNode.rootViewNode?.let { root ->
                    Log.d(TAG, "  rootViewNode className: ${root.className}, childCount: ${root.childCount}")
                }
            }
            
            // 立即隐藏 Session，尽量减少对前台应用的影响
            if (autoHideAfterAssist && isSessionShowing) {
                Log.d(TAG, "Immediately hiding session after receiving AssistStructure")
                hide()
            }
        } else {
            Log.w(TAG, "AssistStructure is null!")
        }
        
        assistLatch?.countDown()
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        super.onHandleScreenshot(screenshot)
        Log.d(TAG, "onHandleScreenshot - screenshot: ${screenshot?.width}x${screenshot?.height}")
        lastScreenshot = screenshot
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isSessionShowing = false
        Log.d(TAG, "VoiceInteractionSession destroyed")
    }
    
    /**
     * 显示数字助理界面并请求 AssistStructure
     * 这会触发 onHandleAssist 回调
     * 
     * 注意：使用 SHOW_SOURCE_ACTIVITY (4) 标志可以尝试保持源应用的焦点
     */
    fun showAndRequestAssist() {
        Log.d(TAG, "showAndRequestAssist called")
        try {
            // SHOW_WITH_ASSIST = 1, SHOW_WITH_SCREENSHOT = 2, SHOW_SOURCE_ACTIVITY = 4
            // 使用 SHOW_SOURCE_ACTIVITY 尝试保持原应用焦点
            val flags = SHOW_WITH_ASSIST or SHOW_SOURCE_ACTIVITY
            show(Bundle(), flags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show and request assist", e)
        }
    }
    
    /**
     * 请求 AssistStructure 并等待结果
     * @param timeoutMs 超时时间（毫秒）
     * @return 是否成功获取到 AssistStructure
     */
    fun requestAssistAndWait(timeoutMs: Long = 5000): Boolean {
        Log.d(TAG, "requestAssistAndWait called, timeout: $timeoutMs")
        assistLatch = CountDownLatch(1)
        
        return try {
            showAndRequestAssist()
            val result = assistLatch?.await(timeoutMs, TimeUnit.MILLISECONDS) ?: false
            Log.d(TAG, "requestAssistAndWait result: $result, structure: $currentAssistStructure")
            result && currentAssistStructure != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request assist and wait", e)
            false
        }
    }

    companion object {

        private val TAG = VoiceInteractionSessionUsher::class.java.simpleName

        @Volatile
        private var instance: WeakReference<VoiceInteractionSessionUsher>? = null

        @Volatile
        @JvmField
        var currentAssistStructure: AssistStructure? = null
        
        @Volatile
        @JvmField
        var lastAssistContent: AssistContent? = null
        
        @Volatile
        @JvmField
        var lastScreenshot: Bitmap? = null
        
        @Volatile
        @JvmField
        var isSessionShowing: Boolean = false
        
        @Volatile
        private var assistLatch: CountDownLatch? = null
        
        /**
         * 是否在获取到 AssistStructure 后自动隐藏
         */
        @Volatile
        @JvmField
        var autoHideAfterAssist: Boolean = true

        @JvmStatic
        fun getInstance(): VoiceInteractionSessionUsher? = instance?.get()

        @JvmStatic
        fun hasInstance(): Boolean = instance?.get() != null
        
        @JvmStatic
        fun clearAssistStructure() {
            currentAssistStructure = null
            lastAssistContent = null
            lastScreenshot = null
        }

    }

}
