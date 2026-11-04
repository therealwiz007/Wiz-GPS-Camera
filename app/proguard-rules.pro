# Keep AndroidX CameraX and OSMDroid classes used via framework/reflection.
-keep class org.osmdroid.** { *; }
-keep class androidx.camera.** { *; }
-dontwarn org.osmdroid.**
