package com.example.bpscnotes.core.update

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play In-App Updates.
 *
 * Prompts as soon as a build with a higher versionCode is live on the track the
 * user installed from. This is driven entirely by Play — no backend involvement,
 * so a release only has to be uploaded, not also registered somewhere.
 *
 * Complements, rather than replaces,
 * [com.example.bpscnotes.presentation.shared.UpdateGateDialog]. That one reads a
 * minimum version from the backend's app_settings and hard-blocks the app, which
 * is the right tool for "this build is broken, nobody may keep using it". This
 * one is the routine "a newer build exists" nudge and never blocks.
 *
 * Two flows:
 *  - FLEXIBLE (default): downloads in the background, user keeps using the app,
 *    then [onReadyToInstall] fires so the UI can offer a restart.
 *  - IMMEDIATE: blocking full-screen Play flow. Used only when the release is
 *    marked high priority (>= 4) in Play Console, or the update has been
 *    available and ignored for over 14 days.
 *
 * Nothing here throws into the app: Play is absent on some devices (sideloads,
 * non-GMS builds) and every call is best-effort.
 */
class InAppUpdateManager(private val activity: Activity) {

    companion object {
        const val REQUEST_CODE = 4711
        private const val TAG = "InAppUpdate"

        /** Play Console "update priority" at or above which we block. */
        private const val IMMEDIATE_PRIORITY = 4

        /** Days an update may sit ignored before we stop being polite. */
        private const val IMMEDIATE_STALENESS_DAYS = 14
    }

    private val manager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(activity)
    }

    /** Invoked when a FLEXIBLE update has finished downloading. */
    var onReadyToInstall: (() -> Unit)? = null

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) onReadyToInstall?.invoke()
    }

    /** Call from Activity.onCreate. */
    fun checkForUpdate() {
        runCatching {
            manager.registerListener(installListener)
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener

                    val staleDays = info.clientVersionStalenessDays() ?: 0
                    val wantsImmediate = info.updatePriority() >= IMMEDIATE_PRIORITY ||
                            staleDays >= IMMEDIATE_STALENESS_DAYS

                    val type = when {
                        wantsImmediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                            AppUpdateType.IMMEDIATE
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                            AppUpdateType.FLEXIBLE
                        else -> return@addOnSuccessListener
                    }
                    startFlow(info, type)
                }
                .addOnFailureListener { Log.d(TAG, "Update check skipped: ${it.message}") }
        }.onFailure { Log.d(TAG, "Play update unavailable: ${it.message}") }
    }

    /**
     * Call from Activity.onResume.
     *
     * An IMMEDIATE update that the user backgrounded mid-download leaves the app
     * in a partially-updated state; Play requires us to resume it. Also re-offers
     * the install prompt if a FLEXIBLE download completed while we were away.
     */
    fun resumeIfInterrupted() {
        runCatching {
            manager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startFlow(info, AppUpdateType.IMMEDIATE)
                } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    onReadyToInstall?.invoke()
                }
            }
        }
    }

    /** Applies a downloaded FLEXIBLE update. Restarts the app. */
    fun completeUpdate() {
        runCatching { manager.completeUpdate() }
    }

    fun unregister() {
        runCatching { manager.unregisterListener(installListener) }
    }

    private fun startFlow(
        info: com.google.android.play.core.appupdate.AppUpdateInfo,
        type: Int,
    ) {
        runCatching {
            @Suppress("DEPRECATION")
            manager.startUpdateFlowForResult(info, type, activity, REQUEST_CODE)
        }.onFailure { Log.d(TAG, "Could not start update flow: ${it.message}") }
    }
}
