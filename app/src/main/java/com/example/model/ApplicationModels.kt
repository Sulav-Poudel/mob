package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ApplicationStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    WAITLISTED("Waitlisted"),
    ACCEPTED("Accepted");

    companion object {
        fun fromRaw(raw: String?): ApplicationStatus =
            entries.firstOrNull { it.name == raw } ?: NOT_STARTED
    }
}

enum class SupplementStatus(val label: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    READY_TO_SUBMIT("Ready to Submit");

    companion object {
        fun fromRaw(raw: String?): SupplementStatus =
            entries.firstOrNull { it.name == raw } ?: NOT_STARTED
    }
}

enum class WordBucket(val label: String, val shortLabel: String, val weight: Int) {
    W100("100 words", "100w", 100),
    W150("150 words", "150w", 150),
    W250("250 words", "250w", 250),
    W250_500("250–500 words", "250–500w", 375),
    W500_PLUS("500+ words", "500+w", 600);

    companion object {
        fun fromRaw(raw: String?): WordBucket =
            entries.firstOrNull { it.name == raw } ?: W250
    }
}

enum class CssProfileRequirement(val label: String) {
    COMPULSORY("Compulsory"),
    EASY_ALTERNATIVE("Easy Alternative"),
    ALTERNATIVE_WITH_MAIL("Alternative with Mail"),
    VERY_HARD_ALTERNATIVE("Very Hard Alternative");

    companion object {
        fun fromRaw(raw: String?): CssProfileRequirement =
            entries.firstOrNull { it.name == raw } ?: COMPULSORY
    }
}

enum class FinancialAidDocStatus(val label: String) {
    NOT_STARTED("Not Started"),
    STARTED("Started"),
    DONE("Done");

    companion object {
        fun fromRaw(raw: String?): FinancialAidDocStatus =
            entries.firstOrNull { it.name == raw } ?: NOT_STARTED
    }
}

enum class EnglishProficiency(val label: String) {
    REQUIRED("Required"),
    EASY_WAIVE("Easy Waive"),
    EMAIL_WAIVE("Email Waive"),
    MOI_REQUIRED("MOI Required");

    companion object {
        fun fromRaw(raw: String?): EnglishProficiency =
            entries.firstOrNull { it.name == raw } ?: REQUIRED
    }
}

enum class ProgressMode(val label: String) {
    WORD_WEIGHTED("Word-Weighted"),
    ESSAY_COUNT("Essay Count")
}

@Keep
@IgnoreExtraProperties
data class University(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val applicationDeadlineMillis: Long? = null,
    val applicationStatus: String = ApplicationStatus.NOT_STARTED.name,
    val targetSupplementCount: Long = 0L,
    val financialAidDeadlineMillis: Long? = null,
    val cssProfileRequirement: String = CssProfileRequirement.COMPULSORY.name,
    val financialAidDocStatus: String = FinancialAidDocStatus.NOT_STARTED.name,
    val englishProficiency: String = EnglishProficiency.REQUIRED.name,
    val collegeTraits: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: ApplicationStatus get() = ApplicationStatus.fromRaw(applicationStatus)
    val cssEnum: CssProfileRequirement get() = CssProfileRequirement.fromRaw(cssProfileRequirement)
    val finAidStatusEnum: FinancialAidDocStatus get() = FinancialAidDocStatus.fromRaw(financialAidDocStatus)
    val englishEnum: EnglishProficiency get() = EnglishProficiency.fromRaw(englishProficiency)
    val targetCountInt: Int get() = targetSupplementCount.toInt().coerceAtLeast(0)
}

@Keep
@IgnoreExtraProperties
data class Supplement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val universityId: String = "",
    val title: String = "",
    val status: String = SupplementStatus.NOT_STARTED.name,
    val wordBucket: String = WordBucket.W250.name,
    val essayType: String = "",
    val prompt: String = "",
    val brainstormNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: SupplementStatus get() = SupplementStatus.fromRaw(status)
    val wordBucketEnum: WordBucket get() = WordBucket.fromRaw(wordBucket)
}

@Keep
@IgnoreExtraProperties
data class SupplementType(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val normalizedName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

val DEFAULT_STARTER_ESSAY_TYPES = listOf(
    "Why Us",
    "Why Major",
    "Community",
    "Extracurricular",
    "Diversity & Perspective",
    "Challenge & Growth",
    "Short Answer"
)
