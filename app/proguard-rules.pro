# PulseDL Mobile ProGuard / R8 Rules
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }
-keep class com.pulsedl.mobile.data.** { *; }
-dontwarn com.yausername.**
-keepattributes *Annotation*,InnerClasses,Signature
