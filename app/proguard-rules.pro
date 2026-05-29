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

# FIXED: Proguard/R8 rules to prevent stripping of Koin, DataStore, and Coroutines in release builds

# Koin DI - Prevent stripping
-keep class org.koin.** { *; }
-keepclassmembers class **$KoinComponent { *; }
-keep class * implements org.koin.core.component.KoinComponent
-keepattributes Signature, InnerClasses, KotlinMetadata

# DataStore - Prevent stripping
-keep class androidx.datastore.** { *; }
-keep class * implements androidx.datastore.core.DataStore
-keepclassmembers class **$ProtoSink { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}