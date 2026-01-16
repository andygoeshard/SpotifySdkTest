package com.andy.spotifysdktesting.app

import android.R.id.home
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.andy.spotifysdktesting.app.ui.theme.SpotifySdkTestingTheme
import com.andy.spotifysdktesting.core.navigation.presentation.screen.MainScaffold
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModelIntent
import com.andy.spotifysdktesting.feature.spotifywebapi.domain.handler.SpotifyAuthDeeplinkHandler
import org.koin.java.KoinJavaComponent.getKoin

class MainActivity : ComponentActivity() {

    private val home: HomeViewModel by lazy {
        getKoin().get<HomeViewModel>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntentIfDeeplink(intent)

        setContent {
            SpotifySdkTestingTheme {
                MainScaffold()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentIfDeeplink(intent)
    }

    private fun handleIntentIfDeeplink(intent: Intent?) {
        val uri = intent?.data ?: return

        // Verificación extra para asegurar que es nuestro callback
        if (uri.scheme == "com.andy.spotifysdktesting" && uri.host == "callback") {

            // Usando tu handler existente (o extrayendo directamente)
            val code = SpotifyAuthDeeplinkHandler.extractAuthCode(uri)

            if (code != null) {
                println("MainActivity instance hash: ${System.identityHashCode(home)}")
                val intent = HomeViewModelIntent.OnSpotifyCodeReceived(code)

                // 🎯 Enviamos el Intent al único punto de entrada
                home.processIntent(intent)
            } else {
                // Opcional: Manejar el caso de que el Deep Link no traiga código,
                // lo cual podría ser otro Intent (ej. HomeViewModelIntent.LoginFailed)
            }
        } else {
            // Manejar error o cancelación (ej. uri contiene ?error=access_denied)
            val error = uri.getQueryParameter("error")
            println("Error en login de Spotify: $error")
        }
    }
}

