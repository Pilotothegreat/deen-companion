# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers and source file names for crash symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep @Keep annotated classes and members
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ── Koin DI ──────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * implements org.koin.core.component.KoinComponent { *; }
-keepclassmembers class * extends org.koin.core.component.KoinComponent { *; }
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }
# Keep ViewModel classes injected by Koin
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ── Room Database ─────────────────────────────────────────────────────────────
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.DataStore { *; }
-keepclassmembers class * implements androidx.datastore.core.DataStore { *; }
-keep class androidx.datastore.preferences.** { *; }
-keepclassmembers class androidx.datastore.preferences.core.Preferences$Key { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Media3 / ExoPlayer ───────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── Haze (Glassmorphism blur) ─────────────────────────────────────────────────
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# ── Timber ───────────────────────────────────────────────────────────────────
-keep class timber.log.** { *; }

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Kotlin Serialization ──────────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}

# ── App-specific keeps ────────────────────────────────────────────────────────
# Prevent R8 from stripping prayer time enums used via reflection (valueOfOrNull)
-keepclassmembers enum com.pilotothegreat.deencompanion.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
# Keep all database entities and DAOs
-keep class com.pilotothegreat.deencompanion.database.** { *; }
# Keep widget providers for RemoteViews reflection
-keep class com.pilotothegreat.deencompanion.widget.** { *; }
# Keep services
-keep class com.pilotothegreat.deencompanion.services.** { *; }

# ── Debug BuildConfig ─────────────────────────────────────────────────────────
-dontwarn com.pilotothegreat.deencompanion.BuildConfig
