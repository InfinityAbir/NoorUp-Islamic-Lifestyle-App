# ProGuard rules

# --- Gson: keep runtime annotations + model fields (keys must survive R8) ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.example.ui.noorup.hadith.** { *; }
-keep class com.example.ui.noorup.FamilyMember { *; }
-keep class com.example.ui.noorup.FamilyLiveEvent { *; }
