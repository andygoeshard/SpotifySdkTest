package com.andy.spotifysdktesting.core.service.layout

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.material3.ColorProviders
import com.andy.spotifysdktesting.R
import com.andy.spotifysdktesting.core.service.callback.DjNextCallback
import com.andy.spotifysdktesting.core.service.callback.SkipNextCallback

class DjNotificationLayout: GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            DjControls()
        }
    }

    @Composable
    private fun DjControls() {
        val colors = ColorProviders(darkColorScheme())
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(8.dp)
                .appWidgetBackground()
                .background(colors.onPrimary) // Spotify Dark
                .cornerRadius(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Placeholder/Icono del DJ
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground), // Usa un icono relevante aquí
                contentDescription = "DJ Icon",
                modifier = GlanceModifier.size(40.dp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Zona de Texto (Placeholder de la Notificación)
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = "AI DJ ACTIVO",
                    style = androidx.glance.text.TextStyle(color = colors.primary)                )
                Text(
                    text = "Toca para Controles...",
                    style = androidx.glance.text.TextStyle(color = colors.secondary)
                )
            }

            // Controles
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                // Botón DJ Next Song
                Image(
                    provider = ImageProvider(R.drawable.baseline_music_note_24), // Recurso de icono
                    contentDescription = "DJ Next",
                    modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<DjNextCallback>())
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Botón Skip Next
                Image(
                    provider = ImageProvider(R.drawable.baseline_skip_next_24), // Recurso de icono
                    contentDescription = "Next Song",
                    modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<SkipNextCallback>())
                )
            }
        }
    }
}