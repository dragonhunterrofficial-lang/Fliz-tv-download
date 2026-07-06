# ---- Compose ----
-keep class androidx.compose.** { *; }

# ---- Speed up R8: skip optimization pass ----
-dontoptimize

# ---- Kotlin metadata (R8 compat) ----
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*, InnerClasses
-keep class io.ktor.** { *; }

# ---- Media3 (ExoPlayer) ----
-keep class androidx.media3.** { *; }

# ---- Coil ----
-keep class coil.** { *; }

# ---- kotlinx.serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.fliztv.**$$serializer { *; }
-keepclassmembers class com.example.fliztv.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.fliztv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- AndroidX Security (EncryptedSharedPreferences) ----
-keep class androidx.security.** { *; }

# ---- OkHttp ----
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontnote okhttp3.**

# ---- VLC/libvlc ----
-keep class org.videolan.** { *; }

# ---- WebView / JavaScript interface ----
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---- Gson / JSON (if any) ----
-dontwarn sun.misc.**

# ---- Ktor Android compatibility ----
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ---- Supabase ----
-keep class io.github.jan.supabase.** { *; }

# ---- kotlinx.coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---- General Android ----
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ---- Keep BuildConfig ----
-keep class com.example.fliztv.BuildConfig { *; }
