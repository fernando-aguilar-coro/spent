# Spent App ProGuard / R8 Rules

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Room Database
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.TypeConverter {
    <init>(...);
    public <methods>;
}

# App Entities and Models (Room & In-Memory Data)
-keep class com.app.spent.data.local.entity.** { *; }
-keep class com.app.spent.data.sync.** { *; }
-keep class com.app.spent.ui.mvi.** { *; }

# Gson Serialization & Reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers enum * { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Google API Client & Google Drive REST Services
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**
-dontwarn org.apache.http.**
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# Google Play Services Auth
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.** { *; }

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Koin Dependency Injection
-keep class io.insertkoin.** { *; }
-dontwarn io.insertkoin.**