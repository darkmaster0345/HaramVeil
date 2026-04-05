/*
 * HaramVeil
 * Copyright (C) 2026 HaramVeil Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.haramveil.overlay

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import com.haramveil.R
import com.haramveil.data.local.ProtectionPreferencesRepository
import com.haramveil.security.AppLockdownManager
import com.haramveil.utils.DetectionBus
import com.haramveil.utils.DetectionEvent
import com.haramveil.utils.DispatcherProvider
import com.haramveil.utils.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class VeilOverlayService : Service() {
    private val dispatcherProvider = DispatcherProvider()
    private val serviceScope = CoroutineScope(SupervisorJob() + dispatcherProvider.main)
    private val backgroundScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private lateinit var windowManager: WindowManager
    private lateinit var protectionPreferencesRepository: ProtectionPreferencesRepository
    private lateinit var appLockdownManager: AppLockdownManager
    private lateinit var veilContentRepository: VeilContentRepository

    private var collectorsStarted = false
    private var overlayViewHolder: OverlayViewHolder? = null
    private var protectionJob: Job? = null
    private var dismissFallbackJob: Job? = null
    private var activeRequest: DetectionEvent.VeilRequested? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        protectionPreferencesRepository = ProtectionPreferencesRepository(applicationContext)
        appLockdownManager = AppLockdownManager(applicationContext)
        veilContentRepository = VeilContentRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ActionStart || intent?.action == null) {
            startForeground(NotificationId, buildForegroundNotification())
            if (!collectorsStarted) {
                startCollectors()
                collectorsStarted = true
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        dismissFallbackJob?.cancel()
        protectionJob?.cancel()
        removeOverlayImmediately()
        serviceScope.cancel()
        backgroundScope.cancel()
        super.onDestroy()
    }

    private fun startCollectors() {
        serviceScope.launch {
            DetectionBus.events.collect { event ->
                when (event) {
                    is DetectionEvent.VeilRequested -> handleVeilRequested(event)
                    is DetectionEvent.WindowStateObserved -> handleWindowStateObserved(event)
                    else -> Unit
                }
            }
        }
    }

    private fun handleVeilRequested(
        request: DetectionEvent.VeilRequested,
    ) {
        activeRequest = request
        showVeil(request)
        protectionJob?.cancel()
        dismissFallbackJob?.cancel()
        protectionJob = backgroundScope.launch {
            engageProtection(request)
        }
        dismissFallbackJob = serviceScope.launch {
            delay(FallbackDismissDelayMs)
            if (activeRequest == request) {
                dismissVeil()
            }
        }
    }

    private fun handleWindowStateObserved(
        event: DetectionEvent.WindowStateObserved,
    ) {
        val currentRequest = activeRequest ?: return
        if (event.eventType != android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        if (event.packageName !in resolveHomePackages()) {
            return
        }

        DebugLog.i(applicationContext, LogTag) {
            "Launcher observed after ${currentRequest.packageName} block. Dismissing veil."
        }
        dismissVeil()
    }

    private suspend fun engageProtection(
        request: DetectionEvent.VeilRequested,
    ) {
        val settings = protectionPreferencesRepository.readSettings()
        appLockdownManager.lockApp(
            packageName = request.packageName,
            durationMs = settings.lockdownDurationMs,
        )
        val forceStopped = forceClosePackageBestEffort(request.packageName)
        navigateHome()
        DebugLog.i(applicationContext, LogTag) {
            "Veil engaged for ${request.packageName} via ${request.triggerMode}. forceStop=$forceStopped details=${request.matchDetails}"
        }
    }

    private suspend fun navigateHome() {
        withContext(dispatcherProvider.main) {
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    private fun showVeil(
        request: DetectionEvent.VeilRequested,
    ) {
        if (!android.provider.Settings.canDrawOverlays(this)) {
            DebugLog.w(applicationContext, LogTag) {
                "Overlay permission is missing. Running protection actions without the veil UI."
            }
            return
        }

        val passage = veilContentRepository.randomPassage()
        val existingViewHolder = overlayViewHolder
        if (existingViewHolder == null) {
            val createdViewHolder = createOverlayViewHolder()
            overlayViewHolder = createdViewHolder
            bindPassage(createdViewHolder, passage)
            bindSubtitle(createdViewHolder, request)
            createdViewHolder.root.alpha = 0f
            windowManager.addView(createdViewHolder.root, overlayLayoutParams())
            createdViewHolder.root.animate()
                .alpha(1f)
                .setDuration(FadeInDurationMs)
                .start()
        } else {
            bindPassage(existingViewHolder, passage)
            bindSubtitle(existingViewHolder, request)
            existingViewHolder.root.alpha = 0f
            existingViewHolder.root.animate()
                .alpha(1f)
                .setDuration(FadeInDurationMs)
                .start()
        }
    }

    private fun dismissVeil() {
        val viewHolder = overlayViewHolder ?: run {
            activeRequest = null
            return
        }

        dismissFallbackJob?.cancel()
        viewHolder.root.animate()
            .alpha(0f)
            .setDuration(FadeOutDurationMs)
            .withEndAction {
                removeOverlayImmediately()
                activeRequest = null
            }
            .start()
    }

    private fun removeOverlayImmediately() {
        val viewHolder = overlayViewHolder ?: return
        runCatching {
            windowManager.removeViewImmediate(viewHolder.root)
        }
        overlayViewHolder = null
    }

    private fun bindPassage(
        viewHolder: OverlayViewHolder,
        passage: VeilPassage,
    ) {
        viewHolder.arabicText.text = passage.arabic
        viewHolder.englishText.text = passage.english
        viewHolder.sourceText.text = buildString {
            append(passage.source)
            append(" • ")
            append(if (passage.type == VeilPassageType.AYAH) "Ayah" else "Hadith")
        }
    }

    private fun bindSubtitle(
        viewHolder: OverlayViewHolder,
        request: DetectionEvent.VeilRequested,
    ) {
        viewHolder.reasonText.text = when (request.triggerMode) {
            com.haramveil.utils.DetectionTriggerMode.MODE_1 ->
                "Mode 1 spotted high-risk content and veiled the screen."

            com.haramveil.utils.DetectionTriggerMode.MODE_2 ->
                "Mode 2 found risky text and moved you away from the app."

            com.haramveil.utils.DetectionTriggerMode.MODE_3 ->
                "Mode 3 detected unsafe visual content and blocked the app."

            com.haramveil.utils.DetectionTriggerMode.LOCKDOWN ->
                "This app is still in lockdown, so HaramVeil blocked it again."
        }
    }

    private fun createOverlayViewHolder(): OverlayViewHolder {
        val arabicTypeface = ResourcesCompat.getFont(this, R.font.amiri_regular) ?: Typeface.SERIF

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(242, 13, 27, 42))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

        val watermark = ImageView(this).apply {
            setImageResource(R.drawable.veil_geometric_pattern)
            alpha = 0.3f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        root.addView(
            watermark,
            FrameLayout.LayoutParams(dp(240), dp(240), Gravity.CENTER),
        )

        val contentStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(32), dp(28), dp(32))
        }

        val arabicText = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = arabicTypeface
            setLineSpacing(0f, 1.15f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
        }
        contentStack.addView(
            arabicText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val englishText = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#B0BEC5"))
            setLineSpacing(0f, 1.18f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
        val englishParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(18)
        }
        contentStack.addView(englishText, englishParams)

        val sourceText = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#718096"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        val sourceParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(12)
        }
        contentStack.addView(sourceText, sourceParams)

        val reasonText = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#8AA0AE"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        val reasonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(20)
        }
        contentStack.addView(reasonText, reasonParams)

        root.addView(
            contentStack,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ).apply {
                marginStart = dp(18)
                marginEnd = dp(18)
            },
        )

        return OverlayViewHolder(
            root = root,
            arabicText = arabicText,
            englishText = englishText,
            sourceText = sourceText,
            reasonText = reasonText,
        )
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = dp(28)
            }
        }

    private fun forceClosePackageBestEffort(
        packageName: String,
    ): Boolean {
        if (packageName == applicationContext.packageName) {
            return false
        }

        if (invokeHiddenForceStop(packageName)) {
            return true
        }

        if (runRootForceStop(packageName)) {
            return true
        }

        return false
    }

    private fun invokeHiddenForceStop(
        packageName: String,
    ): Boolean {
        val activityManager = getSystemService(ActivityManager::class.java) ?: return false
        return runCatching {
            val method = ActivityManager::class.java.getDeclaredMethod(
                "forceStopPackage",
                String::class.java,
            )
            method.isAccessible = true
            method.invoke(activityManager, packageName)
            true
        }.getOrDefault(false)
    }

    private fun runRootForceStop(
        packageName: String,
    ): Boolean {
        if (!File("/system/bin/su").exists() && !File("/system/xbin/su").exists()) {
            return false
        }

        return runCatching {
            val process = ProcessBuilder("su", "-c", "am force-stop $packageName")
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(RootCommandTimeoutMs, TimeUnit.MILLISECONDS)
            completed && process.exitValue() == 0
        }.getOrDefault(false)
    }

    private fun resolveHomePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .map { resolveInfo -> resolveInfo.activityInfo.packageName }
            .toSet()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = getSystemService(NotificationManager::class.java) ?: return
        if (notificationManager.getNotificationChannel(NotificationChannelId) != null) {
            return
        }

        notificationManager.createNotificationChannel(
            NotificationChannel(
                NotificationChannelId,
                getString(R.string.veil_service_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.veil_service_channel_description)
            },
        )
    }

    private fun buildForegroundNotification(): Notification =
        NotificationCompat.Builder(this, NotificationChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.veil_service_notification_title))
            .setContentText(getString(R.string.veil_service_notification_body))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private data class OverlayViewHolder(
        val root: FrameLayout,
        val arabicText: TextView,
        val englishText: TextView,
        val sourceText: TextView,
        val reasonText: TextView,
    )

    companion object {
        const val ActionStart = "com.haramveil.overlay.action.START"

        private const val LogTag = "HaramVeilVeil"
        private const val NotificationChannelId = "haramveil_veil_service"
        private const val NotificationId = 7107
        private const val FadeInDurationMs = 200L
        private const val FadeOutDurationMs = 300L
        private const val FallbackDismissDelayMs = 4_000L
        private const val RootCommandTimeoutMs = 1_500L
    }
}
