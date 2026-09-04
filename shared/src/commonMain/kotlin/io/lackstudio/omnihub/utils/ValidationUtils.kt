package io.lackstudio.omnihub.utils

object ValidationUtils {
    fun isPasswordMatchSoFar(password: String, confirmPassword: String): Boolean {
        if (confirmPassword.isEmpty()) return true
        return password.startsWith(confirmPassword)
    }

    fun validatePasswords(password: String, confirmPassword: String): String? {
        if (confirmPassword.isEmpty()) return null
        if (password != confirmPassword) {
            return "Passwords do not match"
        }
        return null
    }
}
