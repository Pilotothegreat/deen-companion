# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# FIXED: Verify Proguard rules are actually being applied

# Koin - MUST keep these
-keep class org.koin.** { *; }
-keepclassmembers class * implements org.koin.core.component.KoinComponent { *; }
-keepclassmembers class * extends org.koin.core.component.KoinComponent { *; }

# Koin Android
-keep class org.koin.android.** { *; }
-keep class org.koin.androidx.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }
-keep class * extends androidx.datastore.core.DataStore { *; }
-keepclassmembers class * implements androidx.datastore.core.DataStore { *; }

# Preferences
-keep class androidx.datastore.preferences.** { *; }
-keepclassmembers class androidx.datastore.preferences.core.Preferences$Key { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Timber
-keep class timber.log.** { *; }

# Debug BuildConfig
-dontwarn com.pilotothegreat.deencompanion.BuildConfig
