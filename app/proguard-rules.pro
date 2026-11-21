# Proguard rules for AgentDesk

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room SQLite & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Termux Terminal JNI and native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.agentdesk.app.termux.** { *; }
-keep class com.termux.terminal.** { *; }

# Keep Dagger / Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.Module class * { *; }

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
