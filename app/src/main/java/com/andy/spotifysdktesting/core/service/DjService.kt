package com.andy.spotifysdktesting.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.compose
import com.andy.spotifysdktesting.R
import com.andy.spotifysdktesting.core.dj.domain.intent.DjIntent
import com.andy.spotifysdktesting.core.dj.domain.manager.DjManager
import com.andy.spotifysdktesting.core.service.layout.DjNotificationLayout
import com.andy.spotifysdktesting.core.tts.domain.manager.TtsManager
import com.andy.spotifysdktesting.core.tts.domain.model.TtsVoice
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifySdkManager
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.repository.SpotifyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

private const val TAG = "DjService"

class DjService : Service() {

    companion object {
        const val CHANNEL_ID = "DjServiceChannel"

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_EXPLAIN_TRACK = "ACTION_EXPLAIN_TRACK"
        const val ACTION_NEXT_TRACK_IA = "ACTION_NEXT_TRACK_IA"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "ACTION_SKIP_NEXT"
    }

    private val spotifySdk: SpotifySdkManager by inject()
    private val djManager: DjManager by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trackObservationJob: Job? = null
    private var lastTrackId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🟢 1. onCreate service iniciado.")
        createNotificationChannel()

        val initialNotification = createSimpleNotification("DJ inicializando...")

        startForeground(1, initialNotification)
        Log.d(TAG, "🟢 3. startForeground llamado.")
        observeDjState()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "❌ 6. onDestroy (Servicio Terminado)")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🟢 4. onStartCommand ejecutado con acción: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                observeSpotifyTracks()
                djManager.onIntent(DjIntent.Start)
                Log.d(TAG, "🟢 5. Invocando observeSpotifyTracks y DjIntent.Start")
            }
            ACTION_STOP -> {
                djManager.onIntent(DjIntent.Stop)
                trackObservationJob?.cancel()
                stopForeground(true)
                stopSelf()
            }
            ACTION_EXPLAIN_TRACK -> djManager.onIntent(DjIntent.ExplainCurrentSong)
            ACTION_NEXT_TRACK_IA -> djManager.onIntent(DjIntent.NextTrackIA)
            ACTION_PLAY_PAUSE -> spotifySdk.play()
            ACTION_SKIP_NEXT -> spotifySdk.next()
        }
        return START_STICKY
    }

    private fun observeDjState() {
        serviceScope.launch {
            djManager.state.collect { djState ->
                val text = when {
                    djState.speaking -> "🎤 Hablando: ${djState.currentText}"
                    djState.thinking -> "🤔 Pensando..."
                    else -> "🎵 Conectado a Spotify"
                }

                // 🛑 USAMOS LA FUNCIÓN SUSPEND DENTRO DE LA CORRUTINA
                val notification = createGlanceNotification(text)

                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(1, notification)
            }
        }
    }

    private fun observeSpotifyTracks() {
        trackObservationJob?.cancel()
        trackObservationJob = serviceScope.launch {
            spotifySdk.state
                .map { it.currentTrack?.id }
                .distinctUntilChanged()
                .collect { trackId ->
                    if (trackId != null && trackId != lastTrackId) {
                        lastTrackId = trackId
                        Log.d(TAG, "🎵 Cambio de track detectado")
                        djManager.onIntent(DjIntent.SpotifyTrackChanged)
                    }
                }
        }
    }

    private fun createSimpleNotification(text: String): Notification {
        val context = applicationContext

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Agrega la acción de Play/Pause (síncrona)
        val playPauseIntent = Intent(this, DjService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(this, 100, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI DJ Activo")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.baseline_play_arrow_24, "Play/Pause", playPausePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private suspend fun createGlanceNotification(text: String): Notification { // 🛑 Mantenemos 'suspend'
        val context = applicationContext
        val remoteViews = DjNotificationLayout().compose(context) // Esto requiere suspend

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val playPauseIntent = Intent(this, DjService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(this, 100, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI DJ Activo")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setCustomContentView(remoteViews) // Volvemos a usar la vista personalizada
            .setCustomBigContentView(remoteViews)
            .addAction(R.drawable.baseline_play_arrow_24, "Play/Pause", playPausePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DJ Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}