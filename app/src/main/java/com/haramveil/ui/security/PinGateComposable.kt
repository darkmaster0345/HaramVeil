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

package com.haramveil.ui.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.haramveil.data.models.SecurityQuestion
import com.haramveil.security.PinManager
import com.haramveil.security.SecurityQuestionsManager
import com.haramveil.ui.HaramVeilScreenBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinGateComposable(
    authenticated: Boolean,
    pinManager: PinManager,
    securityQuestionsManager: SecurityQuestionsManager,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (authenticated || !pinManager.isSet()) {
        content()
        return
    }

    HaramVeilScreenBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AuthenticationAndRecoveryPanel(
                pinManager = pinManager,
                securityQuestionsManager = securityQuestionsManager,
                title = title,
                subtitle = subtitle,
                onAuthenticated = onAuthenticated,
                onCancel = null,
            )
        }
    }
}

@Composable
fun PinActionGateDialog(
    visible: Boolean,
    pinManager: PinManager,
    securityQuestionsManager: SecurityQuestionsManager,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    if (!visible) {
        return
    }

    if (!pinManager.isSet()) {
        LaunchedEffect(visible) {
            onAuthenticated()
        }
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            AuthenticationAndRecoveryPanel(
                pinManager = pinManager,
                securityQuestionsManager = securityQuestionsManager,
                title = title,
                subtitle = subtitle,
                onAuthenticated = onAuthenticated,
                onCancel = onDismiss,
            )
        }
    }
}

@Composable
private fun AuthenticationAndRecoveryPanel(
    pinManager: PinManager,
    securityQuestionsManager: SecurityQuestionsManager,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(AuthenticationStep.ENTER_PIN) }
    var pinInput by remember { mutableStateOf("") }
    var stagedNewPin by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var questionErrorMessage by remember { mutableStateOf<String?>(null) }
    var pinShakeTrigger by remember { mutableIntStateOf(0) }
    val recoveryAnswers = remember { mutableStateMapOf<Int, Boolean>() }
    val recoveryQuestions by produceState(initialValue = emptyList<SecurityQuestion>()) {
        value = securityQuestionsManager.configuredQuestions()
    }
    val pinLockoutRemainingMs = rememberLockoutRemainingMs(
        active = step == AuthenticationStep.ENTER_PIN && pinManager.isLockedOut(),
        remainingProvider = pinManager::getLockoutRemainingMs,
    )
    val questionLockoutRemainingMs = rememberLockoutRemainingMs(
        active = step == AuthenticationStep.SECURITY_QUESTIONS && securityQuestionsManager.isLockedOut(),
        remainingProvider = securityQuestionsManager::getLockoutRemainingMs,
    )

    when (step) {
        AuthenticationStep.ENTER_PIN -> {
            PinEntryScreen(
                title = title,
                subtitle = subtitle,
                enteredPin = pinInput,
                confirmLabel = "Unlock",
                errorMessage = pinErrorMessage,
                isLockedOut = pinManager.isLockedOut(),
                lockoutRemainingMs = pinLockoutRemainingMs,
                showForgotPin = recoveryQuestions.isNotEmpty(),
                shakeTrigger = pinShakeTrigger,
                cancelLabel = if (onCancel != null) "Cancel" else null,
                onDigitPressed = { digit ->
                    pinErrorMessage = null
                    if (pinInput.length < RequiredPinLength) {
                        pinInput += digit
                    }
                },
                onBackspace = {
                    pinErrorMessage = null
                    if (pinInput.isNotEmpty()) {
                        pinInput = pinInput.dropLast(1)
                    }
                },
                onClear = {
                    pinErrorMessage = null
                    pinInput = ""
                },
                onConfirm = {
                    if (pinManager.verifyPIN(pinInput)) {
                        pinManager.resetFailedAttempts()
                        pinInput = ""
                        pinErrorMessage = null
                        onAuthenticated()
                    } else {
                        pinManager.onWrongAttempt()
                        pinInput = ""
                        pinShakeTrigger += 1
                        pinErrorMessage = if (pinManager.isLockedOut()) {
                            "Too many wrong PIN attempts. Try again in ${formatLockoutCountdown(pinManager.getLockoutRemainingMs())}."
                        } else {
                            "Incorrect PIN. ${pinManager.remainingAttemptsBeforeLockout()} attempts left."
                        }
                    }
                },
                onForgotPin = {
                    step = AuthenticationStep.SECURITY_QUESTIONS
                    pinErrorMessage = null
                },
                onCancel = onCancel,
            )
        }

        AuthenticationStep.SECURITY_QUESTIONS -> {
            SecurityQuestionScreen(
                title = "Answer your recovery questions",
                subtitle = "All 3 answers must match. Three wrong tries trigger a 20-minute lockout.",
                questions = recoveryQuestions,
                selectedAnswers = recoveryAnswers,
                errorMessage = questionErrorMessage,
                isLockedOut = securityQuestionsManager.isLockedOut(),
                lockoutRemainingMs = questionLockoutRemainingMs,
                onAnswerSelected = { index, answer ->
                    questionErrorMessage = null
                    recoveryAnswers[index] = answer
                },
                onSubmit = {
                    scope.launch {
                        val verified = securityQuestionsManager.verifyAnswers(recoveryAnswers.toMap())
                        if (verified) {
                            securityQuestionsManager.resetFailedAttempts()
                            questionErrorMessage = null
                            pinInput = ""
                            stagedNewPin = ""
                            step = AuthenticationStep.SET_NEW_PIN
                        } else {
                            securityQuestionsManager.onWrongAttempt()
                            questionErrorMessage = if (securityQuestionsManager.isLockedOut()) {
                                "Recovery is locked. Try again in ${formatLockoutCountdown(securityQuestionsManager.getLockoutRemainingMs())}."
                            } else {
                                "Those answers did not match. ${securityQuestionsManager.remainingAttemptsBeforeLockout()} attempts left."
                            }
                        }
                    }
                },
                onBack = {
                    questionErrorMessage = null
                    recoveryAnswers.clear()
                    step = AuthenticationStep.ENTER_PIN
                },
            )
        }

        AuthenticationStep.SET_NEW_PIN -> {
            PinEntryScreen(
                title = "Set a new PIN",
                subtitle = "Choose a new 6-digit PIN. The bcrypt hash stays encrypted on-device and survives app updates as long as the key alias stays the same.",
                enteredPin = pinInput,
                confirmLabel = "Continue",
                errorMessage = pinErrorMessage,
                shakeTrigger = pinShakeTrigger,
                cancelLabel = "Back",
                onDigitPressed = { digit ->
                    pinErrorMessage = null
                    if (pinInput.length < RequiredPinLength) {
                        pinInput += digit
                    }
                },
                onBackspace = {
                    pinErrorMessage = null
                    if (pinInput.isNotEmpty()) {
                        pinInput = pinInput.dropLast(1)
                    }
                },
                onClear = {
                    pinErrorMessage = null
                    pinInput = ""
                },
                onConfirm = {
                    stagedNewPin = pinInput
                    pinInput = ""
                    pinErrorMessage = null
                    step = AuthenticationStep.CONFIRM_NEW_PIN
                },
                onCancel = {
                    pinInput = ""
                    pinErrorMessage = null
                    step = AuthenticationStep.SECURITY_QUESTIONS
                },
            )
        }

        AuthenticationStep.CONFIRM_NEW_PIN -> {
            PinEntryScreen(
                title = "Confirm your new PIN",
                subtitle = "Enter the same 6 digits again to finish the reset.",
                enteredPin = pinInput,
                confirmLabel = "Save new PIN",
                errorMessage = pinErrorMessage,
                shakeTrigger = pinShakeTrigger,
                cancelLabel = "Back",
                onDigitPressed = { digit ->
                    pinErrorMessage = null
                    if (pinInput.length < RequiredPinLength) {
                        pinInput += digit
                    }
                },
                onBackspace = {
                    pinErrorMessage = null
                    if (pinInput.isNotEmpty()) {
                        pinInput = pinInput.dropLast(1)
                    }
                },
                onClear = {
                    pinErrorMessage = null
                    pinInput = ""
                },
                onConfirm = {
                    if (pinInput == stagedNewPin) {
                        pinManager.storePIN(pinInput)
                        pinInput = ""
                        pinErrorMessage = null
                        onAuthenticated()
                    } else {
                        pinInput = ""
                        pinShakeTrigger += 1
                        pinErrorMessage = "The two PIN entries did not match. Please try again."
                    }
                },
                onCancel = {
                    pinInput = ""
                    pinErrorMessage = null
                    step = AuthenticationStep.SET_NEW_PIN
                },
            )
        }
    }
}

@Composable
private fun rememberLockoutRemainingMs(
    active: Boolean,
    remainingProvider: () -> Long,
): Long {
    var remainingMs by remember { mutableLongStateOf(remainingProvider()) }

    LaunchedEffect(active) {
        remainingMs = remainingProvider()
        if (!active) {
            return@LaunchedEffect
        }

        while (remainingMs > 0L) {
            delay(1_000L)
            remainingMs = remainingProvider()
        }
    }

    return remainingMs
}

private enum class AuthenticationStep {
    ENTER_PIN,
    SECURITY_QUESTIONS,
    SET_NEW_PIN,
    CONFIRM_NEW_PIN,
}
