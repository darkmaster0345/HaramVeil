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

package com.haramveil.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.haramveil.R
import com.haramveil.data.local.OnboardingPreferencesRepository
import com.haramveil.data.models.SecurityQuestion
import com.haramveil.data.models.StoredSecurityQuestion
import java.security.MessageDigest

class SecurityQuestionsManager(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val onboardingPreferencesRepository = OnboardingPreferencesRepository(applicationContext)
    private val encryptedPreferences by lazy {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            applicationContext,
            PreferenceFileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun questionCatalog(): List<SecurityQuestion> {
        return QuestionDefinitions.map { definition ->
            SecurityQuestion(
                id = definition.id,
                prompt = applicationContext.getString(definition.promptResId),
            )
        }
    }

    suspend fun storeConfiguredQuestions(
        answers: List<Pair<SecurityQuestion, Boolean>>,
    ) {
        val storedQuestions = answers.take(MaxConfiguredQuestions).map { (question, answer) ->
            StoredSecurityQuestion(
                questionId = question.id,
                answerHash = hashAnswer(question.id, answer),
            )
        }
        onboardingPreferencesRepository.saveSecurityQuestions(storedQuestions)
        resetFailedAttempts()
    }

    suspend fun storeAnswers(
        answers: Map<Int, Boolean>,
    ) {
        val existingQuestions = onboardingPreferencesRepository.readSnapshot().securityQuestions
        if (existingQuestions.isEmpty()) {
            return
        }

        val updatedQuestions = existingQuestions.mapIndexed { index, storedQuestion ->
            val updatedAnswer = answers[index]
            if (updatedAnswer == null) {
                storedQuestion
            } else {
                storedQuestion.copy(
                    answerHash = hashAnswer(storedQuestion.questionId, updatedAnswer),
                )
            }
        }
        onboardingPreferencesRepository.saveSecurityQuestions(updatedQuestions)
        resetFailedAttempts()
    }

    suspend fun verifyAnswers(
        attempts: Map<Int, Boolean>,
    ): Boolean {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return false
        }

        val storedQuestions = onboardingPreferencesRepository.readSnapshot().securityQuestions
        if (storedQuestions.size < MaxConfiguredQuestions) {
            return false
        }

        return storedQuestions.take(MaxConfiguredQuestions).allIndexed { index, storedQuestion ->
            val attemptedAnswer = attempts[index] ?: return@allIndexed false
            hashAnswer(storedQuestion.questionId, attemptedAnswer) == storedQuestion.answerHash
        }
    }

    suspend fun configuredQuestions(): List<SecurityQuestion> {
        val configuredIds = onboardingPreferencesRepository.readSnapshot().securityQuestions
            .take(MaxConfiguredQuestions)
            .map { stored -> stored.questionId }
        val catalogById = questionCatalog().associateBy { question -> question.id }
        return configuredIds.mapNotNull { questionId -> catalogById[questionId] }
    }

    fun onWrongAttempt() {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return
        }

        val updatedAttempts = encryptedPreferences.getInt(QuestionFailedAttemptsKey, 0) + 1
        val editor = encryptedPreferences.edit()
        if (updatedAttempts >= MaxFailedAttempts) {
            editor.putInt(QuestionFailedAttemptsKey, updatedAttempts)
            editor.putLong(QuestionLockoutUntilKey, System.currentTimeMillis() + LockoutDurationMs)
        } else {
            editor.putInt(QuestionFailedAttemptsKey, updatedAttempts)
        }
        editor.apply()
    }

    fun resetFailedAttempts() {
        encryptedPreferences.edit()
            .putInt(QuestionFailedAttemptsKey, 0)
            .putLong(QuestionLockoutUntilKey, 0L)
            .apply()
    }

    fun isLockedOut(): Boolean {
        clearExpiredLockoutIfNeeded()
        return encryptedPreferences.getLong(QuestionLockoutUntilKey, 0L) > System.currentTimeMillis()
    }

    fun getLockoutRemainingMs(): Long {
        clearExpiredLockoutIfNeeded()
        return (encryptedPreferences.getLong(QuestionLockoutUntilKey, 0L) - System.currentTimeMillis())
            .coerceAtLeast(0L)
    }

    fun remainingAttemptsBeforeLockout(): Int {
        clearExpiredLockoutIfNeeded()
        if (isLockedOut()) {
            return 0
        }
        val failedAttempts = encryptedPreferences.getInt(QuestionFailedAttemptsKey, 0)
        return (MaxFailedAttempts - failedAttempts).coerceAtLeast(0)
    }

    private fun clearExpiredLockoutIfNeeded() {
        val lockoutUntil = encryptedPreferences.getLong(QuestionLockoutUntilKey, 0L)
        if (lockoutUntil == 0L || lockoutUntil > System.currentTimeMillis()) {
            return
        }

        encryptedPreferences.edit()
            .putInt(QuestionFailedAttemptsKey, 0)
            .putLong(QuestionLockoutUntilKey, 0L)
            .apply()
    }

    private fun hashAnswer(
        questionId: String,
        answer: Boolean,
    ): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$questionId:${answer.toString().lowercase()}".toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private inline fun <T> List<T>.allIndexed(
        predicate: (Int, T) -> Boolean,
    ): Boolean {
        forEachIndexed { index, item ->
            if (!predicate(index, item)) {
                return false
            }
        }
        return true
    }

    private companion object {
        const val MaxConfiguredQuestions = 3
        private const val PreferenceFileName = "haramveil_recovery_state"
        private const val QuestionFailedAttemptsKey = "question_failed_attempts"
        private const val QuestionLockoutUntilKey = "question_lockout_until_epoch_ms"
        private const val MaxFailedAttempts = 3
        private const val LockoutDurationMs = 20 * 60 * 1_000L

        val QuestionDefinitions = listOf(
            QuestionDefinition(
                id = "first_school_karachi",
                promptResId = R.string.security_question_first_school_karachi,
            ),
            QuestionDefinition(
                id = "older_sibling",
                promptResId = R.string.security_question_older_sibling,
            ),
            QuestionDefinition(
                id = "father_name_m",
                promptResId = R.string.security_question_father_name_m,
            ),
            QuestionDefinition(
                id = "dae_first_year",
                promptResId = R.string.security_question_dae_first_year,
            ),
            QuestionDefinition(
                id = "wear_glasses",
                promptResId = R.string.security_question_wear_glasses,
            ),
            QuestionDefinition(
                id = "same_birth_city",
                promptResId = R.string.security_question_same_birth_city,
            ),
            QuestionDefinition(
                id = "bike_before_ten",
                promptResId = R.string.security_question_bike_before_ten,
            ),
            QuestionDefinition(
                id = "left_handed_family_member",
                promptResId = R.string.security_question_left_handed_family_member,
            ),
            QuestionDefinition(
                id = "first_friend_neighborhood",
                promptResId = R.string.security_question_first_friend_neighborhood,
            ),
            QuestionDefinition(
                id = "memorized_surah_before_twelve",
                promptResId = R.string.security_question_memorized_surah_before_twelve,
            ),
        )
    }
}

private data class QuestionDefinition(
    val id: String,
    val promptResId: Int,
)
