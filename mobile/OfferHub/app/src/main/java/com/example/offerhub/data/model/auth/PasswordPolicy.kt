package com.example.offerhub.data.model.auth

object PasswordPolicy {
    fun isValid(password: String): Boolean =
        password.length >= 8 &&
            password.any(Char::isUpperCase) &&
            password.any(Char::isDigit) &&
            password.any { !it.isLetterOrDigit() }
}
