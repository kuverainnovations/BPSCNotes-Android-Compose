package com.example.bpscnotes.di

import com.example.bpscnotes.core.config.AppConfigRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint so Composables (not ViewModels) can read the admin-controlled
 * app config without a ViewModel scope — used by SecureScreen, which wraps
 * arbitrary screens and has no ViewModel of its own.
 * Usage:
 *   val appConfig = EntryPointAccessors.fromApplication(
 *       context.applicationContext, AppConfigEntryPoint::class.java
 *   ).appConfigRepository()
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppConfigEntryPoint {
    fun appConfigRepository(): AppConfigRepository
}
