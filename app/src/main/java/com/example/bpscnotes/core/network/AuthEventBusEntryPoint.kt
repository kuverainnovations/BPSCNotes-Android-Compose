package com.example.bpscnotes.core.network

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthEventBusEntryPoint {
    fun authEventBus(): AuthEventBus
}
