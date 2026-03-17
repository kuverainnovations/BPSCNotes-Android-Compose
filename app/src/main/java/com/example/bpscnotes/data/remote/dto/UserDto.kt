package com.example.bpscnotes.data.remote.dto

data class UserDto(
    val id: String,
    val name: String,
    val mobile: String,
    val email: String?,
    val profilePic: String?,
    val coinBalance: Int,
    val isSubscribed: Boolean,
    val subscriptionExpiry: String?,
    val createdAt: String,
)