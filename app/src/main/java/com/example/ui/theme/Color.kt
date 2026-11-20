package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Editorial Academic Light Tokens
val WarmPaper = Color(0xFFFAF7F2)
val CreamCard = Color(0xFFFFFDF9)
val MutedParchment = Color(0xFFF1ECE2)
val CollegiateNavy = Color(0xFF1B365D)
val NavyContainerLight = Color(0xFFD7E2F2)
val NavyOnContainerLight = Color(0xFF0F213B)
val AcademicGold = Color(0xFFC48B28)
val GoldContainerLight = Color(0xFFF7E8CC)
val GoldOnContainerLight = Color(0xFF3F2A04)
val BotanicalSage = Color(0xFF2E6F40)
val SageContainerLight = Color(0xFFD5ECD9)
val SageOnContainerLight = Color(0xFF0C3318)
val TerracottaCrimson = Color(0xFFB43E3E)
val CrimsonContainerLight = Color(0xFFFADADA)
val CrimsonOnContainerLight = Color(0xFF4A0D0D)
val SoftCobalt = Color(0xFF355C9A)
val CobaltContainerLight = Color(0xFFDCE7F9)
val CobaltOnContainerLight = Color(0xFF102A54)
val DeepInk = Color(0xFF181C24)
val MutedSlate = Color(0xFF5A6475)
val WarmOutline = Color(0xFFD6CEC0)
val WarmOutlineVariant = Color(0xFFE7E0D3)

// Editorial Academic Dark Tokens
val DarkSlateBg = Color(0xFF111621)
val DarkCard = Color(0xFF19202E)
val DarkElevated = Color(0xFF242D3F)
val LightNavy = Color(0xFF9BBDF2)
val LightNavyOn = Color(0xFF0E2342)
val DarkNavyContainer = Color(0xFF1B365D)
val DarkNavyOnContainer = Color(0xFFDCE8FA)
val LightGold = Color(0xFFE5B45B)
val LightGoldOn = Color(0xFF382504)
val DarkGoldContainer = Color(0xFF52380B)
val DarkGoldOnContainer = Color(0xFFF9E4BC)
val LightSage = Color(0xFF84D197)
val LightSageOn = Color(0xFF0B3217)
val DarkSageContainer = Color(0xFF194726)
val DarkSageOnContainer = Color(0xFFCFF2D7)
val LightCrimson = Color(0xFFF08A8A)
val LightCrimsonOn = Color(0xFF470B0B)
val DarkCrimsonContainer = Color(0xFF6E1F1F)
val DarkCrimsonOnContainer = Color(0xFFFCDDDD)
val LightCobalt = Color(0xFF93B4EB)
val DarkCobaltContainer = Color(0xFF1D3661)
val DarkCobaltOnContainer = Color(0xFFDCE7F9)
val WarmIvoryInk = Color(0xFFF2EFE9)
val CoolAsh = Color(0xFFB0B8C7)
val DarkOutline = Color(0xFF3A4356)
val DarkOutlineVariant = Color(0xFF2A3242)

@Immutable
data class AcademicColors(
    val accepted: Color,
    val acceptedContainer: Color,
    val onAcceptedContainer: Color,
    val inProgress: Color,
    val inProgressContainer: Color,
    val onInProgressContainer: Color,
    val waitlisted: Color,
    val waitlistedContainer: Color,
    val onWaitlistedContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    val urgent: Color,
    val urgentContainer: Color,
    val onUrgentContainer: Color,
    val finAidAccent: Color,
    val finAidContainer: Color,
    val onFinAidContainer: Color,
)

val LightAcademicColors = AcademicColors(
    accepted = BotanicalSage,
    acceptedContainer = SageContainerLight,
    onAcceptedContainer = SageOnContainerLight,
    inProgress = SoftCobalt,
    inProgressContainer = CobaltContainerLight,
    onInProgressContainer = CobaltOnContainerLight,
    waitlisted = AcademicGold,
    waitlistedContainer = GoldContainerLight,
    onWaitlistedContainer = GoldOnContainerLight,
    neutral = MutedSlate,
    neutralContainer = MutedParchment,
    onNeutralContainer = DeepInk,
    urgent = TerracottaCrimson,
    urgentContainer = CrimsonContainerLight,
    onUrgentContainer = CrimsonOnContainerLight,
    finAidAccent = AcademicGold,
    finAidContainer = GoldContainerLight,
    onFinAidContainer = GoldOnContainerLight,
)

val DarkAcademicColors = AcademicColors(
    accepted = LightSage,
    acceptedContainer = DarkSageContainer,
    onAcceptedContainer = DarkSageOnContainer,
    inProgress = LightCobalt,
    inProgressContainer = DarkCobaltContainer,
    onInProgressContainer = DarkCobaltOnContainer,
    waitlisted = LightGold,
    waitlistedContainer = DarkGoldContainer,
    onWaitlistedContainer = DarkGoldOnContainer,
    neutral = CoolAsh,
    neutralContainer = DarkElevated,
    onNeutralContainer = WarmIvoryInk,
    urgent = LightCrimson,
    urgentContainer = DarkCrimsonContainer,
    onUrgentContainer = DarkCrimsonOnContainer,
    finAidAccent = LightGold,
    finAidContainer = DarkGoldContainer,
    onFinAidContainer = DarkGoldOnContainer,
)

val LocalAcademicColors = staticCompositionLocalOf { LightAcademicColors }
