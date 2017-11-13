package net.temporus.depikture.utils

fun isNameValid(name: String): Boolean = name.length > 3 && name.trim() != ""

fun isEmailValid(email: String): Boolean = email.length > 6 && email.trim() != ""

fun isPasswordValid(password: String): Boolean =
        password.length > 7 && password.trim() != ""