# SecureChat ProGuard Rules
# =========================
# CRITICAL: Crypto classes MUST NOT be obfuscated.
# Breaking crypto = security vulnerability.

# ── StealthX / SecureChat ─────────────────────────────────────
-keep class com.stealthx.** { *; }

# ── libsodium / lazysodium ───────────────────────────────────
-keep class com.goterl.lazysodium.** { *; }
-keep class org.libsodium.** { *; }
-dontwarn com.goterl.**
-dontwarn net.java.dev.**

# ── JNA (required by lazysodium) ─────────────────────────────
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-dontwarn com.sun.jna.**

# ── SQLCipher — JNI fields looked up by name, must not be renamed ─
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── Room ──────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Hilt ──────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.** class *
-keepclasseswithmembers class * { @dagger.hilt.* <methods>; }

# ── AIDL / IPC ────────────────────────────────────────────────
-keep class * implements android.os.IInterface { *; }
-keep class * implements android.os.IBinder { *; }

# ── Kotlin ────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ── Enums ─────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Parcelable ────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
