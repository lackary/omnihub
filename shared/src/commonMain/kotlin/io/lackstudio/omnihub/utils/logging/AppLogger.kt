package io.lackstudio.omnihub.utils.logging

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig

/**
 * Global Logger wrapper for the App layer.
 * Designed for Kermit 2.0.8, supporting Lambda usage and Preview safety.
 */
object AppLog {
    // Default state: Preview safety mode.
    // StaticConfig() doesn't require any Context and prints directly to System.out/Logcat, making it Preview-safe.
    private var delegate: Logger = Logger(
        config = StaticConfig(),
        tag = "OmniFeedApp" // Default Tag
    )

    // Initialization: Called in Application.onCreate to replace with a configured Logger (e.g., with colors, Crashlytics, etc.)
    fun init(baseLogger: Logger) {
        this.delegate = baseLogger
    }

    // =========================================================
    // Core Encapsulation: Retains Function Type (() -> String)
    // =========================================================

    /**
     * Debug Log
     * @param tag (Optional) Temporary tag override
     * @param throwable (Optional) Exception/Error
     * @param message Lambda function, supporting lazy evaluation
     */
    fun d(tag: String? = null, throwable: Throwable? = null, message: () -> String) {
        getLogger(tag).d(throwable = throwable, message = message)
    }

    fun i(tag: String? = null, throwable: Throwable? = null, message: () -> String) {
        getLogger(tag).i(throwable = throwable, message = message)
    }

    fun w(tag: String? = null, throwable: Throwable? = null, message: () -> String) {
        getLogger(tag).w(throwable = throwable, message = message)
    }

    fun e(tag: String? = null, throwable: Throwable? = null, message: () -> String) {
        getLogger(tag).e(throwable = throwable, message = message)
    }

    fun v(tag: String? = null, throwable: Throwable? = null, message: () -> String) {
        getLogger(tag).v(throwable = throwable, message = message)
    }

    // Advanced: If a ViewModel prefers to hold a Logger instance (for a fixed Tag)
    // Note: Ensure this is called after init() (i.e., when the ViewModel is created) to get the correct instance.
    fun withTag(tag: String): Logger {
        return delegate.withTag(tag)
    }

    private fun getLogger(tag: String?): Logger =
        tag?.let { delegate.withTag(it) } ?: delegate
}
