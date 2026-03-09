package org.autojs.autojs.core.voiceinteraction

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import org.autojs.autojs.annotation.ScriptInterface
import org.autojs.autojs.app.GlobalAppContext
import org.autojs.autojs.util.ViewUtils
import org.autojs.autojs6.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * Tool class for managing VoiceInteractionService (Digital Assistant).
 */
class VoiceAssistantTool(private val context: Context? = null) {

    private val mApplicationContext = GlobalAppContext.get()
    private val mContext: Context
        get() = context ?: mApplicationContext
    private val mServiceNamePrefix = mApplicationContext.packageName

    @ScriptInterface
    fun launchSettings() {
        try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mApplicationContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mApplicationContext.startActivity(intent)
            } catch (e2: ActivityNotFoundException) {
                ViewUtils.showToast(mContext, R.string.text_go_to_voice_assistant_settings, true)
            }
        }
    }
    
    /**
     * 唤醒数字助理
     * 优先使用已有的 Session.show()（如果 Session 存在）
     * 其次使用 VoiceInteractionService.showSession()（如果 Service 存在且 ready）
     * 最后使用 ACTION_ASSIST Intent（会触发系统创建 Service 和 Session）
     * @return 是否成功发送唤醒请求
     */
    @ScriptInterface
    fun wakeUp(): Boolean {
        Log.d(TAG, "wakeUp called")
        
        // 方法1: 如果 Session 已存在，直接调用 show()
        val session = VoiceInteractionSessionUsher.getInstance()
        if (session != null) {
            return try {
                session.showAndRequestAssist()
                Log.d(TAG, "wakeUp: Using existing Session.show()")
                true
            } catch (e: Exception) {
                Log.e(TAG, "wakeUp: Session.show() failed", e)
                tryServiceOrIntent()
            }
        }
        
        return tryServiceOrIntent()
    }
    
    /**
     * 尝试通过 Service 或 Intent 唤醒
     */
    private fun tryServiceOrIntent(): Boolean {
        // 方法2: 如果 Service 存在且 ready，使用 showSession
        val service = VoiceInteractionServiceUsher.getInstance()
        if (service != null && VoiceInteractionServiceUsher.isReady) {
            return try {
                service.showSessionForAssist()
                Log.d(TAG, "wakeUp: Using Service.showSession()")
                true
            } catch (e: Exception) {
                Log.e(TAG, "wakeUp: Service.showSession() failed, falling back to ACTION_ASSIST", e)
                wakeUpWithIntent()
            }
        }
        
        // 方法3: 使用 ACTION_ASSIST Intent（会触发系统创建 Service 和 Session）
        Log.d(TAG, "wakeUp: Service/Session not available, using ACTION_ASSIST")
        return wakeUpWithIntent()
    }
    
    /**
     * 使用 ACTION_ASSIST Intent 唤醒数字助理
     * 注意：这会显示助理界面，获取的可能是助理界面的控件
     */
    @ScriptInterface
    fun wakeUpWithIntent(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_ASSIST)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mApplicationContext.startActivity(intent)
            Log.d(TAG, "wakeUpWithIntent: ACTION_ASSIST sent")
            true
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "wakeUpWithIntent: No assistant available", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "wakeUpWithIntent: Failed to wake up assistant", e)
            false
        }
    }
    
    /**
     * 唤醒数字助理并等待 AssistStructure
     * @param timeoutMs 超时时间（毫秒）
     * @param autoHide 获取后是否自动隐藏助理界面
     * @return 是否成功获取到 AssistStructure
     */
    @ScriptInterface
    @JvmOverloads
    fun wakeUpAndWait(timeoutMs: Long = 5000, autoHide: Boolean = true): Boolean {
        Log.d(TAG, "wakeUpAndWait called, timeout=$timeoutMs, autoHide=$autoHide")
        
        if (!serviceExists()) {
            Log.w(TAG, "wakeUpAndWait: Service not exists, please set as default assistant first")
            return false
        }
        
        // 设置自动隐藏选项
        VoiceInteractionSessionUsher.autoHideAfterAssist = autoHide
        
        // 清除旧的结构
        VoiceInteractionSessionUsher.clearAssistStructure()
        Log.d(TAG, "wakeUpAndWait: Cleared old AssistStructure")
        
        // 唤醒助理
        if (!wakeUp()) {
            Log.e(TAG, "wakeUpAndWait: wakeUp() failed")
            return false
        }
        Log.d(TAG, "wakeUpAndWait: wakeUp() succeeded, waiting for AssistStructure...")
        
        // 等待 AssistStructure
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + timeoutMs
        var lastLogTime = startTime
        
        while (SystemClock.elapsedRealtime() < endTime) {
            val structure = VoiceInteractionSessionUsher.currentAssistStructure
            if (structure != null) {
                val elapsed = SystemClock.elapsedRealtime() - startTime
                Log.d(TAG, "wakeUpAndWait: AssistStructure received in ${elapsed}ms, windowCount=${structure.windowNodeCount}")
                
                // 如果需要自动隐藏，立即隐藏 Session
                if (autoHide) {
                    hideSession()
                }
                
                return true
            }
            
            // 每500ms打印一次等待日志
            val now = SystemClock.elapsedRealtime()
            if (now - lastLogTime >= 500) {
                Log.d(TAG, "wakeUpAndWait: Still waiting... elapsed=${now - startTime}ms, session=${VoiceInteractionSessionUsher.getInstance()}, showing=${VoiceInteractionSessionUsher.isSessionShowing}")
                lastLogTime = now
            }
            
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                Log.w(TAG, "wakeUpAndWait: Interrupted")
                break
            }
        }
        
        val elapsed = SystemClock.elapsedRealtime() - startTime
        Log.w(TAG, "wakeUpAndWait: Timeout after ${elapsed}ms, structure=${VoiceInteractionSessionUsher.currentAssistStructure}")
        return VoiceInteractionSessionUsher.currentAssistStructure != null
    }
    
    /**
     * 隐藏 VoiceInteractionSession
     */
    @ScriptInterface
    fun hideSession() {
        try {
            VoiceInteractionSessionUsher.getInstance()?.hide()
            Log.d(TAG, "hideSession: Session hidden")
        } catch (e: Exception) {
            Log.e(TAG, "hideSession: Failed to hide session", e)
        }
    }
    
    /**
     * 关闭数字助理界面
     * 通过发送 HOME Intent 来关闭数字助理
     */
    @ScriptInterface
    fun dismiss() {
        try {
            // 方法1: 发送 HOME Intent
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mApplicationContext.startActivity(homeIntent)
            Log.d(TAG, "dismiss: HOME intent sent")
        } catch (e: Exception) {
            Log.e(TAG, "dismiss: Failed to dismiss assistant", e)
        }
    }
    
    /**
     * 通过返回键关闭数字助理
     * 需要无障碍服务支持
     */
    @ScriptInterface
    fun dismissByBack(): Boolean {
        val session = VoiceInteractionSessionUsher.getInstance()
        if (session != null) {
            try {
                session.hide()
                Log.d(TAG, "dismissByBack: Session hidden")
                return true
            } catch (e: Exception) {
                Log.e(TAG, "dismissByBack: Failed to hide session", e)
            }
        }
        return false
    }

    @ScriptInterface
    fun isServiceRunning(): Boolean = hasInstance() && serviceExists()

    @ScriptInterface
    fun hasInstance(): Boolean = VoiceInteractionServiceUsher.hasInstance()

    @ScriptInterface
    fun serviceExists(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = mContext.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null) {
                return roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
            }
        }
        return isDefaultAssistant()
    }

    private fun isDefaultAssistant(): Boolean {
        val assistComponent = Settings.Secure.getString(
            mApplicationContext.contentResolver,
            "assistant"
        ) ?: Settings.Secure.getString(
            mApplicationContext.contentResolver,
            "voice_interaction_service"
        ) ?: return false

        return assistComponent.contains(mServiceNamePrefix)
    }

    companion object {

        private val TAG = VoiceAssistantTool::class.java.simpleName

    }

}
