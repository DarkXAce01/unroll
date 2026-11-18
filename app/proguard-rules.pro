# Project-specific R8 rules. AndroidX, Compose, Coil, Play billing, ads and ML Kit ship their own.

# Projects are stored as JSON through kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class io.github.darkxace01.unroll.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
