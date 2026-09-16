# Tip 127 — Live Rocket Tracker release R8 keep rules (Play money).
# Vehicle art loads via Resources.getIdentifier("vehicle_*") — do not strip classes that drive HUD.

# Android components (manifest already keeps; belt+suspenders)
-keep class com.ccos.retro.** { *; }
-keep class com.liverockettracker.** { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class **$mappings { <fields>; }

# AndroidX / Material
-keep class androidx.** { *; }
-dontwarn androidx.**
-keep class com.google.android.material.** { *; }

# org.json used for LL2 / vault / tapes
-keep class org.json.** { *; }

# BuildConfig
-keep class com.ccos.retro.BuildConfig { *; }

# Parcelable / Serializable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Native
-keepclasseswithmembernames class * {
    native <methods>;
}

# Attributes / View constructors
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Wallpaper / Service
-keep class * extends android.service.wallpaper.WallpaperService { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.app.Activity { *; }

# Line numbers for crash mapping
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile
