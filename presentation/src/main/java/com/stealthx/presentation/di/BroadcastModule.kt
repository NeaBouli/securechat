/*
 * SecureChat — StealthX Platform
 * Copyright (C) 2026 Vendetta Labs / StealthX Platform
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.stealthx.presentation.di

import com.stealthx.features.broadcast.BroadcastManager
import com.stealthx.presentation.screens.LocalBroadcastManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BroadcastModule {
    @Binds
    @Singleton
    abstract fun bindBroadcastManager(manager: LocalBroadcastManager): BroadcastManager
}
