package org.autojs.autojs.service

import android.content.Context
import org.autojs.autojs.core.voiceinteraction.VoiceAssistantTool
import org.autojs.autojs.ui.main.drawer.ServiceItemHelper

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * Service layer wrapper for VoiceInteractionService (Digital Assistant).
 * Note: Digital assistant cannot be enabled/disabled via API, so toggle always launches settings.
 */
open class VoiceAssistantService(final override val context: Context) : ServiceItemHelper {

    private val mVoiceAssistantTool = VoiceAssistantTool(context)

    override val isRunning: Boolean
        get() = mVoiceAssistantTool.serviceExists() || mVoiceAssistantTool.isServiceRunning()

    override fun active(): Boolean {
        mVoiceAssistantTool.launchSettings()
        return isRunning
    }

    override fun start(): Boolean {
        mVoiceAssistantTool.launchSettings()
        return isRunning
    }

    override fun startIfNeeded() {
        if (!isRunning) {
            mVoiceAssistantTool.launchSettings()
        }
    }

    override fun stop(): Boolean {
        mVoiceAssistantTool.launchSettings()
        return !isRunning
    }

    override fun onToggleSuccess() {
        super.onToggleSuccess()
    }

}
