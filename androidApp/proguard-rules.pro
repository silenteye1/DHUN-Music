# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
-dontnote kotlinx.serialization.**

-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn kotlinx.serialization.internal.ClassValueReferences
-keep class com.maxrave.simpmusic.data.model.** { *; }
-keep class com.maxrave.simpmusic.extension.AllExtKt { *; }
-keep class com.maxrave.simpmusic.extension.AllExtKt$* { *; }
-keep class com.maxrave.kotlinytmusicscraper.extension.MapExtKt$* { *; }

## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class dev.maxrave.pipepipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

# Java beans warning suppression
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

# Retrofit reflection rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp & SSL
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.Util

-keep class com.liskovsoft.** { *; }
-keep interface com.liskovsoft.** { *; }
-keep class com.eclipsesource.v8.** { *; }
-keep class com.maxrave.kotlinytmusicscraper.** { *; }

-dontwarn javax.script.**
-dontwarn jdk.dynalink.**

-keep class org.apache.commons.io.** { *; }

# YtDlp
-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keepattributes SourceFile

-dontwarn com.maxrave.data.di.loader.LoaderKt
-dontwarn com.maxrave.media3.ui.MediaPlayerViewKt

-keep class com.maxrave.data.di.loader.LoaderKt { *; }
-keep class com.maxrave.data.mapping.MappingKt { *; }
-keep class com.maxrave.data.extension.** { *; }
-keep class com.maxrave.data.di.** { *; }

-keep class org.simpmusic.lyrics.parser.** { *; }
-keep class org.simpmusic.lyrics.models.** { *; }
-keep class com.simpmusic.lyrics.parser.** { *; }

-keep class com.google.re2j.** { *; }
-dontwarn com.google.re2j.Matcher
-dontwarn com.google.re2j.Pattern

-keep class * extends androidx.room.RoomDatabase { <init>(); }

-dontwarn io.sentry.android.core.SentryLogcatAdapter
-dontwarn io.sentry.instrumentation.file.SentryFileInputStream$Factory
-dontwarn io.sentry.instrumentation.file.SentryFileOutputStream$Factory
-dontwarn io.sentry.okhttp.SentryOkHttpEventListener
-dontwarn io.sentry.okhttp.SentryOkHttpInterceptor

# ===========================================================================
# Extra Performance & R8 Full Mode Safety Rules
# ===========================================================================

# Koin Dependency Injection
-keep class io.insert.koin.** { *; }
-dontwarn io.insert.koin.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Strip Production Logs for maximum speed & zero I/O overhead
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}