# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# CameraX
-keep class androidx.camera.** { *; }

# ML Kit Barcode
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }

# Room
-keep class fr.triquet.manyinone.data.local.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
