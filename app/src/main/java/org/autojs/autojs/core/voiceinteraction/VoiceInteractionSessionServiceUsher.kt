package org.autojs.autojs.core.voiceinteraction

import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Created by SuperMonster003 on Mar 5, 2026.
 * VoiceInteractionSessionService implementation for AutoJs6.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class VoiceInteractionSessionServiceUsher : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.d(TAG, "Creating new VoiceInteractionSession")
        return VoiceInteractionSessionUsher(this)
    }

    companion object {
        private val TAG = VoiceInteractionSessionServiceUsher::class.java.simpleName
    }

}
