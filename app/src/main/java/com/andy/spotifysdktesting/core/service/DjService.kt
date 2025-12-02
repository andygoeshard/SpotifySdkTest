package com.andy.spotifysdktesting.core.service

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
import com.andy.spotifysdktesting.feature.spotifysdk.domain.manager.SpotifyManager
import com.andy.spotifysdktesting.feature.spotifysdk.domain.repository.SpotifyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class DjService : Service() {

    private val spotifyManager: SpotifyManager by inject()
    private val spotifyRepository: SpotifyRepository by inject()
    private val ttsManager: TtsManager by inject()
    private val aiBrain: AiMusicBrain by inject()
    private val djStateManager: DjStateManager by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var trackObservationJob: Job? = null

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
        const val ACTION_NEXT_TOP_TRACK_IA = "ACTION_NEXT_TOP_TRACK_IA"
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
            ACTION_NEXT_TOP_TRACK_IA -> triggerAiNextSongFromTopTracks()

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
            songCounter = 0
            triggerExplainCurrentSong()
        }
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
        serviceScope.launch {
            Log.d("DjService", "🎤 IA buscando siguiente canción...")
            val currentTrack = spotifyManager.currentTrackCache

            val songQuery = aiBrain.chooseNextSong("sigamos con el mood", currentTrack)

            djStateManager.updateDjText("Sugerencia de la IA: $songQuery")

            if (songQuery.isBlank()) {
                Log.w("DjService", "La IA no sugirió ninguna canción.")
                djStateManager.updateDjText("IA no pudo sugerir una canción.")
                return@launch
            }

            val trackUri = spotifyRepository.getTrackUriFromSearch(songQuery)

            if (trackUri == null) {
                Log.e("DjService", "🔴 No se encontró URI para: $songQuery")
                djStateManager.updateDjText("IA falló: No encontré '$songQuery' en Spotify.")
                speak("Lo siento, no pude encontrar esa canción en Spotify.")
                return@launch
            }

            Log.d("DjService", "✅ Reproduciendo URI: $trackUri")
            spotifyManager.playUri(trackUri)
            speak("Ahora viene '$songQuery', una excelente elección para mantener el ambiente.")
            updateNotification("DJ Activo: Siguiendo sugerencia de la IA.")
        }
    }

    private fun triggerAiNextSongFromTopTracks() {
        if (!spotifyManager.isConnected.value) {
            Log.e("DjService", "Spotify NO está conectado. Saltando acción.")
            updateNotification("DJ Inactivo: Esperando conexión...")
            return
        }

        serviceScope.launch {
            Log.d("DjService", "🎤 IA buscando siguiente canción usando Top Tracks...")

            djStateManager.updateDjText("🔍 Obteniendo Top Tracks de Spotify...")

            // 1. 🌐 OBTENER LISTA REAL de candidatos (Top Tracks)
            val recommendations = spotifyRepository.getTopTracks(limit = 10)

            if (recommendations.isEmpty()) {
                djStateManager.updateDjText("Spotify no devolvió Top Tracks. :(")
                speak("No pude obtener tracks populares. Intenta más tarde.")
                return@launch
            }

            val currentTrack = spotifyManager.currentTrackCache
            // Definimos el "mood" para que la IA sepa qué buscar en la lista
            val mood = "las canciones más populares del momento"

            djStateManager.updateDjText("🧠 IA eligiendo el mejor Top Track...")

            // 2. 🧠 LA IA ELIGE DE LA LISTA
            val nextSongInfo: AiNextSong = aiBrain.chooseFromRecommendations(mood, currentTrack, recommendations)

            // Si la IA responde con una canción vacía, asumimos error.
            if (nextSongInfo.song.isBlank()) {
                Log.e("DjService", "❌ La IA no pudo elegir una canción o el parseo falló.")
                djStateManager.updateDjText("Error: La IA no pudo seleccionar una canción.")
                return@launch
            }

            // 3. 🛡️ OBTENER URI FINAL buscando el match EXACTO en la lista original
            val selectedRecommendation = recommendations.find {
                "${it.artist} - ${it.name}".trim().equals(nextSongInfo.song.trim(), ignoreCase = true)
            }

            if (selectedRecommendation == null) {
                // Esto pasa si la IA eligió algo que no estaba en la lista (violó la restricción)
                Log.e("DjService", "🔴 La IA eligió una canción que NO estaba en la lista de candidatos: ${nextSongInfo.song}")
                djStateManager.updateDjText("Error de lógica de IA: eligió fuera de catálogo. Se salta.")
                speak("Esa canción es un temazo, pero no la encontré entre mis sugerencias, DJ. ¡Saltando!")
                return@launch
            }

            // 4. ▶️ REPRODUCIR
            Log.d("DjService", "✅ Reproduciendo Top Track URI: ${selectedRecommendation.uri}")
            spotifyManager.playUri(selectedRecommendation.uri)

            // Usamos la razón dada por la IA
            speak(nextSongInfo.reason)
            updateNotification("DJ Activo: ${nextSongInfo.reason}")
            djStateManager.updateDjText("🎶 ${nextSongInfo.reason}")
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        // 🎯 2. CREAR EL BUILDER CON EL REMOTE VIEWS PERSONALIZADO
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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