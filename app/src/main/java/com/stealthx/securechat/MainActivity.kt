package com.stealthx.securechat

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.stealthx.data.prefs.AppPreferences
import com.stealthx.presentation.nav.StealthXNavGraph
import com.stealthx.presentation.theme.StealthXTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var prefs: AppPreferences

    private val authState = mutableStateOf<AuthState>(AuthState.Locked)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setLockedContent()
        if (prefs.biometricEnabled) {
            authenticate()
        } else {
            authState.value = AuthState.Unlocked
        }
    }

    override fun onResume() {
        super.onResume()
        if (prefs.biometricEnabled && authState.value == AuthState.Locked) {
            authenticate()
        }
    }

    private fun setLockedContent() {
        setContent {
            StealthXTheme {
                when (authState.value) {
                    AuthState.Locked -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    AuthState.Unlocked -> StealthXNavGraph()
                    AuthState.Unavailable -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Biometric unlock unavailable",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    private fun authenticate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            authState.value = AuthState.Unavailable
            finish()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authState.value = AuthState.Unlocked
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (authState.value != AuthState.Unlocked) {
                        finish()
                    }
                }

                override fun onAuthenticationFailed() {
                    authState.value = AuthState.Locked
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock SecureChat")
                .setSubtitle("Confirm your device credential to continue")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }

    private enum class AuthState {
        Locked,
        Unlocked,
        Unavailable
    }
}
