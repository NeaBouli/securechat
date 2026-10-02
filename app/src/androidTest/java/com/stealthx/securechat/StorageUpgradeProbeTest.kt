package com.stealthx.securechat

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stealthx.data.ChameleonDatabase
import com.stealthx.data.identity.StealthXIdentity
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Two-phase dependency-upgrade probe for persisted secrets (SQLCipher database and
 * EncryptedSharedPreferences identity). Skipped unless `storageUpgradePhase` is passed:
 *
 *  1. build/install the PREVIOUS dependency set, run with `-e storageUpgradePhase write`
 *  2. `adb install -r` the NEW dependency set, run with `-e storageUpgradePhase read`
 *
 * Only for emulators: it writes into the app's real identity store and database.
 */
@RunWith(AndroidJUnit4::class)
class StorageUpgradeProbeTest {
    private val phase: String? =
        InstrumentationRegistry.getArguments().getString("storageUpgradePhase")
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val marker = File(context.filesDir, "storage-upgrade-probe.txt")
    private val passphrase = "storage-upgrade-probe-passphrase".toByteArray(Charsets.UTF_8)

    @Test
    fun persistedSecretsSurviveDependencyUpgrade() {
        assumeTrue("storageUpgradePhase not requested", phase == "write" || phase == "read")
        if (phase == "write") write() else read()
    }

    private fun write() {
        val identity = StealthXIdentity.getOrCreateWithSeed(context)
        val database = ChameleonDatabase.build(context, passphrase)
        database.openHelper.writableDatabase.apply {
            execSQL("CREATE TABLE IF NOT EXISTS storage_upgrade_probe (id INTEGER PRIMARY KEY, value TEXT NOT NULL)")
            execSQL("DELETE FROM storage_upgrade_probe")
            execSQL("INSERT INTO storage_upgrade_probe (id, value) VALUES (1, ?)", arrayOf(identity.raw))
        }
        database.close()
        marker.writeText(identity.raw)
    }

    private fun read() {
        val expected = marker.readText()
        val identity = StealthXIdentity.get(context)
        assertNotNull("identity must stay readable after the upgrade", identity)
        assertEquals(expected, identity!!.raw)

        val database = ChameleonDatabase.build(context, passphrase)
        database.openHelper.readableDatabase
            .query("SELECT value FROM storage_upgrade_probe WHERE id = 1").use { cursor ->
                assertTrue("SQLCipher probe row must stay readable", cursor.moveToFirst())
                assertEquals(expected, cursor.getString(0))
            }
        database.close()
    }
}
