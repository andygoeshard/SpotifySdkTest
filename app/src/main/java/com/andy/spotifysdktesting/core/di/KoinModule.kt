package com.andy.spotifysdktesting.core.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClientConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

val koinModule = module {

    // ⚙️ CONFIGURACIÓN COMÚN (Lambda reutilizable)
    // Esto evita duplicar código y soluciona el problema de scopes de 'install'
    val commonConfig: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 25_000
        }

        expectSuccess = false
    }

    // 🎯 1. CLIENTE PARA AUTENTICACIÓN (AuthClient)
    // Solo aplica la configuración común. NO tiene Auth plugin.
    single(named("AuthClient")) {
        HttpClient {
            commonConfig() // ✅ Aplicamos la config aquí
        }
    }
}