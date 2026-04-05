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
import com.haramveil.data.models.SecurityQuestion
import com.haramveil.security.PinManager
import com.haramveil.security.SecurityQuestionsManager
import com.haramveil.ui.HaramVeilScreenBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChangePINScreen(
    pinManager: PinManager,
    securityQuestionsManager: SecurityQuestionsManager,
    onBack: () -> Unit,
    onPinChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(ChangePinStep.VERIFY_CURRENT_PIN) }
    var pinInput by remember { mutableStateOf("") }
    var stagedNewPin by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }
    var questionErrorMessage by remember { mutableStateOf<String?>(null) }
    var pinShakeTrigger by remember { mutableIntStateOf(0) }
    val recoveryAnswers = remember { mutableStateMapOf<Int, Boolean>() }
    val recoveryQuestions by produceState(initialValue = emptyList<SecurityQuestion>()) {
        value = securityQuestionsManager.configuredQuestions()
    }
    val pinLockoutRemainingMs = rememberChangePinLockoutRemainingMs(
        active = step == ChangePinStep.VERIFY_CURRENT_PIN && pinManager.isLockedOut(),
        remainingProvider = pinManager::getLockoutRemainingMs,
    )
    val questionLockoutRemainingMs = rememberChangePinLockoutRemainingMs(
        active = step == ChangePinStep.SECURITY_QUESTIONS && securityQuestionsManager.isLockedOut(),
        remainingProvider = securityQuestionsManager::getLockoutRemainingMs,
    )

    HaramVeilScreenBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (step) {
                ChangePinStep.VERIFY_CURRENT_PIN -> {
                    PinEntryScreen(
                        title = "Verify your current PIN",
                        subtitle = "Confirm the existing 6-digit PIN before HaramVeil saves a new one.",
                        enteredPin = pinInput,
                        confirmLabel = "Verify current PIN",
                        errorMessage = pinErrorMessage,
                        isLockedOut = pinManager.isLockedOut(),
                        lockoutRemainingMs = pinLockoutRemainingMs,
                        showForgotPin = recoveryQuestions.isNotEmpty(),
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
                            if (pinManager.verifyPIN(pinInput)) {
                                pinManager.resetFailedAttempts()
                                pinInput = ""
                                pinErrorMessage = null
                                step = ChangePinStep.SET_NEW_PIN
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
                            step = ChangePinStep.SECURITY_QUESTIONS
                            pinErrorMessage = null
                        },
                        onCancel = onBack,
                    )
                }

                ChangePinStep.SECURITY_QUESTIONS -> {
                    SecurityQuestionScreen(
                        title = "Recover access with your questions",
                        subtitle = "If you forgot the current PIN, answer all 3 saved recovery questions to set a new one.",
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
                                    step = ChangePinStep.SET_NEW_PIN
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
                            step = ChangePinStep.VERIFY_CURRENT_PIN
                        },
                    )
                }

                ChangePinStep.SET_NEW_PIN -> {
                    PinEntryScreen(
                        title = "Enter your new PIN",
                        subtitle = "Choose the new 6-digit PIN you want HaramVeil to protect.",
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
                            step = ChangePinStep.CONFIRM_NEW_PIN
                        },
                        onCancel = {
                            pinInput = ""
                            pinErrorMessage = null
                            step = ChangePinStep.VERIFY_CURRENT_PIN
                        },
                    )
                }

                ChangePinStep.CONFIRM_NEW_PIN -> {
                    PinEntryScreen(
                        title = "Confirm your new PIN",
                        subtitle = "Enter the same 6 digits again to save the new bcrypt hash.",
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
                                onPinChanged()
                            } else {
                                pinInput = ""
                                pinShakeTrigger += 1
                                pinErrorMessage = "The two PIN entries did not match. Please try again."
                            }
                        },
                        onCancel = {
                            pinInput = ""
                            pinErrorMessage = null
                            step = ChangePinStep.SET_NEW_PIN
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberChangePinLockoutRemainingMs(
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

private enum class ChangePinStep {
    VERIFY_CURRENT_PIN,
    SECURITY_QUESTIONS,
    SET_NEW_PIN,
    CONFIRM_NEW_PIN,
}
