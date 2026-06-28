package com.example.bpscnotes.di

import com.example.bpscnotes.data.remote.api.CurrentAffairsApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApiServiceEntryPoint {
    fun currentAffairsApiService(): CurrentAffairsApiService
}
