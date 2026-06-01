# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep app classes and methods
-keep class com.zovio.announcer.** { *; }
-keep class com.example.** { *; }

# Keep database entities
-keep class com.example.data.db.** { *; }

# Keep Room database
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * { @androidx.room.* *; }
-keep interface androidx.room.** { *; }

# Keep data store
-keep class androidx.datastore.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keepclasseswithmembernames class androidx.compose.** { *; }

# Keep AndroidX
-keep class androidx.** { *; }
-keepclasseswithmembernames class androidx.** { *; }

# Keep lifecycle
-keep class androidx.lifecycle.** { *; }

# Keep view model
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep notification listener service
-keep class com.example.service.UpiNotificationListenerService { *; }
-keep class com.example.service.** { *; }

# Keep enumerations
-keepclassmembers enum * { *; }

# Keep serializable classes
-keep class * implements java.io.Serializable { *; }

# Keep parcelable classes  
-keep class * implements android.os.Parcelable { *; }

# Keep callbacks
-keepclasseswithmembernames class * { *Callback(...); }

# Keep onClick handlers
-keepclasseswithmembernames class * { void onClick(...); }

# Remove verbose logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# Remove line numbers for production
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

