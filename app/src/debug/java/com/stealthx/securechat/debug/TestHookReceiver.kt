package com.stealthx.securechat.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.stealthx.data.exchange.ContactExchangeManager
import com.stealthx.data.identity.PublicKeyBundleQr
import com.stealthx.data.identity.StealthXIdentity
import com.stealthx.data.repository.ContactRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class TestHookReceiver : BroadcastReceiver() {

    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var contactExchangeManager: ContactExchangeManager

    override fun onReceive(context: Context, intent: Intent) {
        val result = runCatching {
            when (intent.action) {
                ACTION_DUMP -> dumpState(context)
                ACTION_LISTEN -> {
                    StealthXIdentity.getOrCreateWithSeed(context)
                    contactExchangeManager.startListening()
                    Bundle().apply {
                        putString("status", "listening")
                        putBoolean("connected", contactExchangeManager.isConnected)
                        putBoolean("identified", contactExchangeManager.isIdentified)
                    }
                }
                ACTION_ADD -> {
                    val content = intent.getStringExtra(EXTRA_CONTENT)
                        ?: error("missing $EXTRA_CONTENT")
                    addContact(content)
                }
                else -> error("unknown action ${intent.action}")
            }
        }.fold(
            onSuccess = { it.apply { putBoolean("ok", true) } },
            onFailure = { error ->
                Bundle().apply {
                    putBoolean("ok", false)
                    putString("error", error.message ?: error::class.java.name)
                }
            }
        )
        setResultExtras(result)
        setResultData(result.toJson().toString())
    }

    private fun dumpState(context: Context): Bundle = runBlocking {
        val identity = StealthXIdentity.getOrCreateWithSeed(context)
        val bundle = StealthXIdentity.createPublicKeyBundle(context)
        val contacts = contactRepository.observeAll().first()
        Bundle().apply {
            putString("sxId", identity.raw)
            putString("qr", PublicKeyBundleQr.toQrContent(bundle))
            putInt("contactCount", contacts.size)
            putStringArray("contacts", contacts.map { it.id }.toTypedArray())
            putBoolean("connected", contactExchangeManager.isConnected)
            putBoolean("identified", contactExchangeManager.isIdentified)
        }
    }

    private fun addContact(content: String): Bundle = runBlocking {
        val bundle = PublicKeyBundleQr.fromQrContent(content).getOrThrow()
        if (contactRepository.getById(bundle.sxId) == null) {
            contactRepository.addContactBundle(bundle)
        }
        contactExchangeManager.sendExchange(bundle.sxId)
        val contacts = contactRepository.observeAll().first()
        Bundle().apply {
            putString("added", bundle.sxId)
            putInt("contactCount", contacts.size)
            putStringArray("contacts", contacts.map { it.id }.toTypedArray())
            putBoolean("connected", contactExchangeManager.isConnected)
            putBoolean("identified", contactExchangeManager.isIdentified)
        }
    }

    private companion object {
        const val ACTION_DUMP = "securechat.app.debug.DUMP"
        const val ACTION_LISTEN = "securechat.app.debug.LISTEN"
        const val ACTION_ADD = "securechat.app.debug.ADD"
        const val EXTRA_CONTENT = "content"
    }
}

private fun Bundle.toJson(): JSONObject {
    val json = JSONObject()
    for (key in keySet()) {
        when (val value = get(key)) {
            is Array<*> -> json.put(key, value.joinToString(prefix = "[", postfix = "]"))
            else -> json.put(key, value)
        }
    }
    return json
}
