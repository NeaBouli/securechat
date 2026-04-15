/*
 * Chameleon — Context-Aware Privacy OS for Android
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.ifr.verifier

import com.stealthx.ifr.IFRConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * IFRLock Smart Contract verifier — read-only eth_call only.
 *
 * SECURITY:
 * - NUR eth_call — keine Transaktionen, kein eth_sendTransaction
 * - Timeout: 10 Sekunden pro RPC Call
 * - Fallback: versucht mehrere RPC Endpoints
 * - Contract Adresse aus IFRConstants (nie doppelt hardcodiert)
 */
@Singleton
class IFRLockVerifier @Inject constructor() {

    companion object {
        private const val RPC_TIMEOUT_MS = 10_000L
    }

    /**
     * Get the locked IFR amount for a wallet address.
     *
     * @param walletAddress  EIP-55 Ethereum address
     * @return               Locked amount as BigInteger (raw, with 9 decimals)
     * @throws Exception     If all RPC endpoints fail
     */
    suspend fun getLockedAmount(walletAddress: String): BigInteger = withContext(Dispatchers.IO) {
        val function = org.web3j.abi.datatypes.Function(
            "lockedAmount",
            listOf(Address(walletAddress)),
            listOf(object : TypeReference<Uint256>() {})
        )
        val encodedFunction = FunctionEncoder.encode(function)

        for (endpoint in IFRConstants.RPC_ENDPOINTS) {
            try {
                val result = withTimeout(RPC_TIMEOUT_MS) {
                    callContract(endpoint, encodedFunction)
                }
                val decoded = FunctionReturnDecoder.decode(result, function.outputParameters)
                if (decoded.isNotEmpty()) {
                    return@withContext (decoded[0] as Uint256).value
                }
            } catch (_: Exception) {
                continue
            }
        }
        throw Exception("All RPC endpoints failed for lockedAmount($walletAddress)")
    }

    /**
     * Check if wallet has at least minAmount locked.
     *
     * @param walletAddress  EIP-55 Ethereum address
     * @param minAmount      Minimum locked amount (raw)
     * @return               true if locked >= minAmount
     */
    suspend fun isLocked(walletAddress: String, minAmount: BigInteger): Boolean = withContext(Dispatchers.IO) {
        val function = org.web3j.abi.datatypes.Function(
            "isLocked",
            listOf(Address(walletAddress), Uint256(minAmount)),
            listOf(object : TypeReference<Bool>() {})
        )
        val encodedFunction = FunctionEncoder.encode(function)

        for (endpoint in IFRConstants.RPC_ENDPOINTS) {
            try {
                val result = withTimeout(RPC_TIMEOUT_MS) {
                    callContract(endpoint, encodedFunction)
                }
                val decoded = FunctionReturnDecoder.decode(result, function.outputParameters)
                if (decoded.isNotEmpty()) {
                    return@withContext (decoded[0] as Bool).value
                }
            } catch (_: Exception) {
                continue
            }
        }
        throw Exception("All RPC endpoints failed for isLocked($walletAddress)")
    }

    private fun callContract(rpcEndpoint: String, encodedFunction: String): String {
        val web3j = Web3j.build(HttpService(rpcEndpoint))
        try {
            val transaction = Transaction.createEthCallTransaction(
                "0x0000000000000000000000000000000000000000",
                IFRConstants.IFR_LOCK_ADDRESS,
                encodedFunction
            )
            val response = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send()
            if (response.hasError()) {
                throw Exception("RPC error: ${response.error.message}")
            }
            return response.value
        } finally {
            web3j.shutdown()
        }
    }
}
