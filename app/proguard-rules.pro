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

# ── WalletConnect ─────────────────────────────────────────────
-keep class com.walletconnect.** { *; }
-dontwarn com.walletconnect.**

# ── Web3j + transitive deps ──────────────────────────────────
-keep class org.web3j.** { *; }
-dontwarn org.web3j.**
-dontwarn com.google.errorprone.**
-dontwarn groovy.**
-dontwarn org.codehaus.groovy.**
-dontwarn org.apache.tuweni.**
-dontwarn org.identityconnectors.**
-dontwarn okhttp3.internal.platform.**

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
