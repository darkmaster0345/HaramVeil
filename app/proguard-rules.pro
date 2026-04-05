# ONNX Runtime - Keep all classes and native methods
-keep class com.microsoft.onnxruntime.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# ML Kit - Keep all classes
-keep class com.google.mlkit.** { *; }

# TensorFlow Lite - Keep all classes and native methods (if used)
-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.Interpreter { *; }

# Room Database - Keep entities, DAOs, and database classes
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# DataStore - Keep preferences classes
-keep class androidx.datastore.** { *; }

# Preserve model files in assets
-keepassets **/*.onnx
-keepassets **/*.tflite
-keepassets **/*.txt

# For Kotlin reflection if used
-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses

# Preserve JDK internal classes that ONNX might use
-keepclassmembers class sun.misc.Unsafe { *; }

# For protobuf which ONNX Runtime depends on
-keep class com.google.protobuf.** { *; }

# For Gson (if used, though not in current code)
-keep class com.google.gson.** { *; }

# For WorkManager
-keep class androidx.work.** { *; }

# For AndroidX
-keep class androidx.** { *; }

# For Navigation Component
-keep class androidx.navigation.** { *; }

# For Coroutines
-keep class kotlinx.coroutines.** { *; }

# For Lifecycle
-keep class androidx.lifecycle.** { *; }

# For ViewModel
-keep class androidx.lifecycle.ViewModel { *; }

# Keep R class
-keepclassmembers class **.R$* { public static <fields>; }

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
