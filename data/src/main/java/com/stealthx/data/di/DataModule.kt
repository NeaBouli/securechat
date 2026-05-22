package com.stealthx.data.di

import android.content.Context
import android.util.Base64
import com.stealthx.data.ChameleonDatabase
import com.stealthx.data.dao.ChatSessionDao
import com.stealthx.data.dao.ContactKeyDao
import com.stealthx.data.dao.IfrTierCacheDao
import com.stealthx.data.dao.MessageDao
import com.stealthx.data.repository.IfrTierRepositoryImpl
import com.stealthx.domain.repository.IfrTierRepository
import com.stealthx.domain.tier.TierGate
import com.stealthx.domain.tier.TierGateImpl
import com.stealthx.data.transport.SignalingRelayTransport
import com.stealthx.domain.transport.MessageRouter
import com.stealthx.security.KeystoreManager
import com.stealthx.transport.LocalTransport
import com.stealthx.transport.RelayTransport
import com.stealthx.transport.TransportType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): ChameleonDatabase {
        val prefs = context.getSharedPreferences("chameleon_secure", Context.MODE_PRIVATE)
        val prefKey = "db_passphrase_enc"
        val stored = prefs.getString(prefKey, null)
        val passphrase = if (stored == null) {
            val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val blob = keystoreManager.encryptBytes("chameleon_db_key_wrap", raw)
            prefs.edit().putString(prefKey, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
            raw
        } else {
            keystoreManager.decryptBytes("chameleon_db_key_wrap", Base64.decode(stored, Base64.NO_WRAP))
        }
        return ChameleonDatabase.build(context, passphrase)
    }

    @Provides
    @Singleton
    fun provideContactKeyDao(db: ChameleonDatabase): ContactKeyDao = db.contactKeyDao()

    @Provides
    @Singleton
    fun provideChatSessionDao(db: ChameleonDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: ChameleonDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideMessageRouter(
        signalingTransport: SignalingRelayTransport,
        localTransport: LocalTransport
    ): MessageRouter =
        MessageRouter(mapOf<TransportType, RelayTransport>(
            TransportType.TOR_RELAY to signalingTransport,
            TransportType.LOCAL to localTransport
        ))

    @Provides
    @Singleton
    fun provideIfrTierCacheDao(db: ChameleonDatabase): IfrTierCacheDao = db.ifrTierCacheDao()

    @Provides
    @Singleton
    fun provideIfrTierRepository(impl: IfrTierRepositoryImpl): IfrTierRepository = impl

    @Provides
    @Singleton
    fun provideTierGate(tierRepository: IfrTierRepository): TierGate =
        TierGateImpl(tierRepository)
}
