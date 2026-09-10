# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep the application entry points (also matched by the generated
# aapt rules, but kept explicit here).
-keep class dissonance.cunninglinguist.fragmentry.FragmentryApplication { *; }
-keep class dissonance.cunninglinguist.fragmentry.MainActivity { *; }

# kotlinx.serialization: preserve the generated serializers for the
# backup/migration models so enableBackup()/restore() keep round-tripping.
-keepclassmembers class dissonance.cunninglinguist.fragmentry.core.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class dissonance.cunninglinguist.fragmentry.core.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# LiteRT/TensorFlow Lite: the runtime reaches its classes through JNI and
# reflection, so R8 shrinking/renaming breaks interpreter creation. Inert
# while isMinifyEnabled=false, but guards any future minified release.
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.lite.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class dissonance.cunninglinguist.fragmentry.** {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
