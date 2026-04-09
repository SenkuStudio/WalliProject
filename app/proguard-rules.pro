-keep class com.walli.wallpaper.data.api.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn org.conscrypt.**
