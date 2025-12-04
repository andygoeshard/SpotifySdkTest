package com.andy.spotifysdktesting.core.service.callback

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.andy.spotifysdktesting.core.service.DjService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class PlayPauseCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("GlanceCallback", "Play/Pause tocado.")
        val intent = Intent(context, DjService::class.java).apply {
            action = DjService.ACTION_PLAY_PAUSE
        }
        context.startService(intent)
    }
}

class SkipNextCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("GlanceCallback", "Skip Next tocado.")
        val intent = Intent(context, DjService::class.java).apply {
            action = DjService.ACTION_SKIP_NEXT
        }
        context.startService(intent)
    }
}
class DjNextCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("GlanceCallback", "DJ Next tocado.")
        val intent = Intent(context, DjService::class.java).apply {
            action = DjService.ACTION_NEXT_TRACK_IA
        }
        context.startService(intent)
    }
}