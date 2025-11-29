package com.andy.spotifysdktesting.feature.spotifysdk.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class RedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data

        if (data != null && data.scheme == "com.andy.spotifysdktesting" && data.host == "callback") {

            val code = data.getQueryParameter("code")
            val error = data.getQueryParameter("error")

            val resultIntent = Intent().apply {
                putExtra("code", code)
                putExtra("error", error)
            }

            setResult(RESULT_OK, resultIntent)
        }

        finish()
    }
}