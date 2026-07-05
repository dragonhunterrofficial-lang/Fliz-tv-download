package com.example.fliztv.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSupabase {
    private var _client: SupabaseClient? = null
    val client: SupabaseClient get() = _client ?: throw IllegalStateException("Supabase not configured. Call configure() first.")

    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    private var _supabaseUrl: String = ""
    private var _supabaseAnonKey: String = ""

    val supabaseUrl: String get() = _supabaseUrl
    val supabaseAnonKey: String get() = _supabaseAnonKey

    fun configure(url: String, anonKey: String) {
        if (_client != null) return
        _supabaseUrl = url
        _supabaseAnonKey = anonKey
        _client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            install(Auth)
            install(Postgrest)
            install(Functions)
        }
    }

    fun isConfigured(): Boolean = _client != null

    suspend fun getCurrentSession(): UserSession? {
        return try {
            client.auth.currentSessionOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
