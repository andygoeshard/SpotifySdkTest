package com.andy.spotifysdktesting.core.service

import android.R.attr.name
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.compose
import androidx.media3.common.util.NotificationUtil.createNotificationChannel
import com.andy.spotifysdktesting.R
import com.andy.spotifysdktesting.core.ai.domain.AiMusicBrain
import com.andy.spotifysdktesting.core.ai.domain.model.AiNextSong
import com.andy.spotifysdktesting.core.navigation.domain.DjStateManager
import com.andy.spotifysdktesting.core.service.layout.DjNotificationLayout
import com.andy.spotifysdktesting.core.tts.domain.TtsManager
import com.andy.spotifysdktesting.core.tts.domain.TtsVoice
import com.andy.spotifysdktesting.feature.spotifysdk.domain.helper.extractIdFromUri
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

const val TAG = "DjService"

@Serializable
private data class NextTrackCache(
    val uri: String = "",
    val songName: String,
    val reason: String
)

class DjService : Service() {

    private val spotifyManager: SpotifyManager by inject()
    private val spotifyRepository: SpotifyRepository by inject()
    private val ttsManager: TtsManager by inject()
    private val aiBrain: AiMusicBrain by inject()
    private val djStateManager: DjStateManager by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var trackObservationJob: Job? = null
    private var nextTrackCache: NextTrackCache? = null

    private var songCounter = 0
    private val DJ_CYCLE_LENGTH = 3
    private var lastTrackUri: String? = null
    private val djNotificationLayout = DjNotificationLayout()
    companion object {
        const val CHANNEL_ID = "DjServiceChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_EXPLAIN_TRACK = "ACTION_EXPLAIN_TRACK"
        const val ACTION_NEXT_TRACK_IA = "ACTION_NEXT_TRACK_IA"
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "ACTION_SKIP_NEXT"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("DjService", "🎧 DJ Service Creado")
        createNotificationChannel()
        serviceScope.launch {
            startForeground(1, createNotification("DJ Activo: Inicializando..."))
        }
        observeSpotifyConnectionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> Log.d("DjService", "Servicio iniciado")
            ACTION_STOP -> stopSelf()
            ACTION_EXPLAIN_TRACK -> triggerExplainCurrentSong()
            ACTION_NEXT_TRACK_IA -> triggerAiNextSong()

            ACTION_PLAY_PAUSE -> handlePlayPause()
            ACTION_SKIP_NEXT -> handleSkipNext()
        }
        return START_STICKY
    }

    private fun handlePlayPause() {
        serviceScope.launch {
            val currentTrack = spotifyManager.currentTrackCache
            if (currentTrack?.isPaused == true) {
                spotifyManager.play()
                updateNotification("Reproducción Iniciada")
            } else {
                spotifyManager.pause()
                updateNotification("Reproducción Pausada")
            }
        }
    }

    private fun handleSkipNext() {
        serviceScope.launch {
            spotifyManager.next()
            updateNotification("Saltando Canción...")
        }
    }

    private fun observeSpotifyConnectionState() {
        serviceScope.launch {
            spotifyManager.isConnected.collectLatest { isConnected ->
                when (isConnected) {
                    true -> {
                        Log.d("DjService", "✅ Spotify CONECTADO. Iniciando observador de canciones.")
                        updateNotification("DJ Activo: Escuchando música...")
                        if (trackObservationJob?.isActive != true) {
                            trackObservationJob = observeSpotifyChanges()
                        }
                    }
                    false -> {
                        Log.e("DjService", "❌ Spotify DESCONECTADO o error.")
                        trackObservationJob?.cancel()
                        updateNotification("DJ Inactivo: Esperando conexión.")
                        djStateManager.updateDjText("Esperando conexión a Spotify...")
                    }
                }
            }

        }
    }

    private fun observeSpotifyChanges(): Job = serviceScope.launch {
        spotifyManager.getCurrentlyPlayingTrack().collectLatest { track ->
            val currentId = "${track.artistName} | ${track.trackName}"

            if (currentId.isNotBlank() && currentId != lastTrackUri) {
                Log.d("DjService", "🎵 Cambio de canción detectado: $currentId")
                lastTrackUri = currentId
                checkDjInterruption()
            }
        }
    }
    private fun checkDjInterruption() {
        songCounter++
        Log.d("DjService", "Conteo: $songCounter / $DJ_CYCLE_LENGTH")

        if (songCounter >= DJ_CYCLE_LENGTH) {
            startPreselectionJob()
            songCounter = 0
            triggerExplainCurrentSong()
        }
    }
    private fun startPreselectionJob() {
        serviceScope.launch {
            preselectNextTrack()
        }
    }
    private suspend fun preselectNextTrack() {
        Log.d(TAG, "🔄 Preseleccionando canción vía IA + Search...")
        
        val currentTrackcache = spotifyManager.currentTrackCache

        val mood = "mismo mood y genero"
        djStateManager.updateDjText("🤖 DJ pensando con IA...")

        val aiResult = aiBrain.chooseNextSong(
            mood,
            currentTrack = currentTrackcache
        )

        val cacheTrack = Json.decodeFromString<NextTrackCache>(aiResult)

        Log.d(TAG, "🤖 IA sugirió: $$aiResult")
        
        val trackCheka = spotifyRepository.searchTracks(cacheTrack.songName)

        nextTrackCache = NextTrackCache(
            uri = trackCheka.first().uri,
            songName = trackCheka.first().name,
            reason = cacheTrack.reason
        )
        
        Log.d(TAG, "✅ Preseleccionado: ${trackCheka.first().name} | Cacheado listo.")
        djStateManager.updateDjText("⏭ Preparado siguiente tema: ${trackCheka.first().name}")
    }

    private fun triggerExplainCurrentSong() {
        if (!spotifyManager.isConnected.value) {
            Log.e("DjService", "Spotify NO está conectado. Saltando explicación.")
            updateNotification("DJ Inactivo: Esperando conexión...")
            return
        }

        serviceScope.launch {
            Log.d("DjService", "🎤 DJ Preparando explicación...")

            val currentTrack = spotifyManager.currentTrackCache
            if (currentTrack == null) {
                Log.w("DjService", "No hay track actual para explicar")
                djStateManager.updateDjText("No hay canción reproduciéndose.")
                return@launch
            }

            try {
                val reason = aiBrain.describeActualSong(currentTrack)
                djStateManager.updateDjText(reason) // 🎯 ESCRIBIR ESTADO PARA LA UI
                speak(reason)

            } catch (e: Exception) {
                Log.e("DjService", "Error en IA/DJ: ${e.message}")
                djStateManager.updateDjText("Error del sistema AI.")
            }
        }
    }

    private fun triggerAiNextSong() {
        if (!spotifyManager.isConnected.value) {
            Log.e("DjService", "Spotify NO está conectado. Saltando acción.")
            updateNotification("DJ Inactivo: Esperando conexión...")
            return
        }

        serviceScope.launch {
            val cached = nextTrackCache

            if (cached != null) {
                // 🚀 ÉXITO: CACHÉ ENCONTRADA (Cambio INSTANTÁNEO)
                nextTrackCache = null // Limpiar caché

                Log.d("DjService", "✅ Reproduciendo tema cacheado: ${cached.songName}")
                spotifyManager.playUri(cached.uri)

                djStateManager.updateDjText("🎶 ${cached.reason}")
                speak(cached.reason)
                updateNotification("DJ Activo: Siguiendo sugerencia de la IA.")

                // Iniciar la pre-selección para la canción *siguiente* inmediatamente
                startPreselectionJob()

            } else {
                // ⚠️ FALLBACK: Caché vacía. Ejecutamos el preselect SINCRÓNICAMENTE y esperamos.
                Log.w("DjService", "Caché vacía. Ejecutando flujo de preselección de forma síncrona.")
                djStateManager.updateDjText("Esperando decisión de IA (flujo síncrono)...")

                preselectNextTrack()

                // Si ahora hay algo en caché, lo reproducimos
                val immediateCached = nextTrackCache
                if (immediateCached != null) {
                    // Llamada recursiva (debe resolver con caché esta vez)
                    triggerAiNextSong()
                } else {
                    djStateManager.updateDjText("Error: Falló la selección de canción. No hay caché.")
                }
            }
        }
    }


    private suspend fun speak(text: String) {
        if (text.isBlank()) return
        Log.d("DjService", "🎤 Hablando: $text")
        ttsManager.speak(text, TtsVoice("default"))
    }
    private fun updateNotification(content: String) {
        serviceScope.launch{
            val notification = createNotification(content)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1, notification)
        }
    }
    private suspend fun createNotification(content: String): Notification {
        val context = applicationContext

        val remoteViews = djNotificationLayout.compose(context)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI DJ Activo") // Título del sistema/pantalla de bloqueo
            .setContentText(content) // Texto visible en la notificación colapsada del sistema
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent) // Al tocar la notificación, abre la App
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    private fun createNotificationChannel() {
        // ... (Se mantiene igual)
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
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("DjService", "🛑 Proceso principal eliminado. Deteniendo servicio.")
        stopSelf()
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        djStateManager.clearDjText()
        Log.d("DjService", "🛑 DJ Service Destruido")
    }
}