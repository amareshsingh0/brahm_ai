# Brahm AI — ProGuard / R8 rules

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **.*$serializer {
    static **.*$serializer INSTANCE;
}
-keep,includedescriptorclasses class com.bimoraai.brahm.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.bimoraai.brahm.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Sentry
-keep class io.sentry.** { *; }

# Coil
-dontwarn coil.**

# Keep model classes for JSON deserialization
-keep class com.bimoraai.brahm.core.network.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
