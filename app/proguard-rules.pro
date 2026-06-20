# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.tastile.android.data.model.**$$serializer { *; }
-keepclassmembers class app.tastile.android.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class app.tastile.android.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
