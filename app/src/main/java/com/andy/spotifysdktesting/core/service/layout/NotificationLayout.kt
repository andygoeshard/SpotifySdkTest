package com.andy.spotifysdktesting.core.service.layout

import android.content.Context
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground), // Usa un icono relevante aquí
                contentDescription = "DJ Icon",
                modifier = GlanceModifier.size(40.dp)
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = "Radio :)",
                    style = androidx.glance.text.TextStyle(color = colors.primary))
            }

            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.baseline_music_note_24),
                    contentDescription = "DJ Next",
                    modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<DjNextCallback>())
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Image(
                    provider = ImageProvider(R.drawable.baseline_skip_next_24),
                    contentDescription = "Next Song",
                    modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<SkipNextCallback>())
                )
            }
        }
    }
}