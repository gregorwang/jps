# SplashActivity deliberately loads these two entry points by exact JVM name so the native
# placeholder can draw before Compose classes are verified. Keep only that reflective surface.
-keep class com.animejapaneselab.nativeapp.ComposeHost {
    public static void install(androidx.activity.ComponentActivity);
}
-keep,allowoptimization class com.animejapaneselab.nativeapp.ui.LabAppKt

# Okio carries this compile-time-only annotation in its bytecode; it is never loaded at runtime.
-dontwarn javax.annotation.Nullable

# Compose, Rive and Lottie provide their own consumer rules. Do not add package-wide keeps.
