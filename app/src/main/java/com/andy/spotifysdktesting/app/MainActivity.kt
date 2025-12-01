package com.andy.spotifysdktesting.app

import android.R.id.home
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.andy.spotifysdktesting.app.ui.theme.SpotifySdkTestingTheme
import com.andy.spotifysdktesting.core.navigation.presentation.screen.MainScaffold
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModel
import com.andy.spotifysdktesting.core.navigation.presentation.viewmodel.HomeViewModelIntent
import com.andy.spotifysdktesting.feature.spotifysdk.domain.handler.SpotifyAuthDeeplinkHandler
import com.andy.spotifysdktesting.feature.spotifysdk.ui.RedirectActivity
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyAuthViewModel
import com.andy.spotifysdktesting.feature.spotifysdk.ui.viewmodel.SpotifyViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.java.KoinJavaComponent.getKoin

class MainActivity : ComponentActivity() {

    private val home: HomeViewModel by lazy {
        getKoin().get<HomeViewModel>() // Obtener la instancia singleton o única
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manejar caso donde la app estaba cerrada y se abre por el link
        handleIntentIfDeeplink(intent)

        setContent {
            SpotifySdkTestingTheme {
                MainScaffold()
            }
        }
    }

    // Este método se llama cuando la app ya estaba abierta y el navegador vuelve a ella
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

