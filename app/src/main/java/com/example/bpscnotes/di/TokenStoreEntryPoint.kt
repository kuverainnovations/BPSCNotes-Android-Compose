package com.example.bpscnotes.di

import com.example.bpscnotes.data.local.TokenStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint so Composables (not ViewModels) can get TokenStore
 * without requiring a ViewModel scope.
 * Usage:
 *   val tokenStore = EntryPointAccessors.fromApplication(
 *       context.applicationContext, TokenStoreEntryPoint::class.java
 *   ).tokenStore()
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TokenStoreEntryPoint {
    fun tokenStore(): TokenStore
}
