# ProGuard rules

# --- Gson: keep runtime annotations + model fields (keys must survive R8) ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.example.ui.noorup.hadith.** { *; }
-keep class com.example.ui.noorup.quran.** { *; }
-keep class com.example.ui.noorup.Surah { *; }
-keep class com.example.ui.noorup.Verse { *; }
-keep class com.example.ui.noorup.FamilyMember { *; }
-keep class com.example.ui.noorup.FamilyLiveEvent { *; }

# Family Dhikr + Drive backup models are (de)serialised by Gson.
-keep class com.example.ui.noorup.family.** { *; }
-keep class com.example.ui.noorup.backup.** { *; }
# Custom tasbih models: persistence is hand-written JSON, but keep names stable anyway.
-keep class com.example.ui.noorup.TasbihDhikr { *; }
-keep class com.example.ui.noorup.TasbihStage { *; }
# Friday reflections are stored as Gson JSON: field names must survive R8 or saved entries stop loading.
-keep class com.example.ui.noorup.journey.ReflectionEntry { *; }

# ML Kit / Play-services code scanner (Family Dhikr QR scan): its init provider instantiates component
# registrars by reflection, and R8 would otherwise strip their constructors (scanner fails in release).
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class * implements com.google.android.gms.common.internal.safeparcel.SafeParcelable { *; }
-keep class com.google.mlkit.common.internal.** { *; }
-keep class com.google.mlkit.vision.common.internal.** { *; }
-keep class com.google.mlkit.vision.codescanner.** { *; }
