package com.example.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Small helpers for consistent, gentle haptic feedback across the app.
 * Calls are safe no-ops on devices without a vibrator.
 */
object Haptics {

    /** Light, short tick - used for tab switches, chip & toggle selection. */
    fun tick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Slightly stronger tap - used for destructive / high-emphasis actions (e.g. swipe delete). */
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Success confirmation - used when a transaction is saved. */
    fun success(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
