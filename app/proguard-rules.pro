# Preserve readable stack traces in Play Console crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations and Kotlin metadata needed by Compose, Lifecycle, and CameraX
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,Signature,InnerClasses,EnclosingMethod

# Keep ViewModels instantiated by ViewModelProvider / viewModel()
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep CenterCube data models, solver enums, and audio interfaces intact under R8
-keep class com.example.cube.** { *; }
-keep class com.example.audio.** { *; }
-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve AndroidX CameraX lifecycle and preview components
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Preserve Android TTS and Compose runtime reflection hooks
-keepclassmembers class com.example.MainActivity {
    public void onCreate(android.os.Bundle);
}
