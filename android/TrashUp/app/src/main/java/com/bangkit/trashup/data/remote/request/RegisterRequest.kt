package com.bangkit.trashup.data.remote.request

data class RegisterRequest(
    val name_user: String,
    val email_user: String,
    val password_user: String
)