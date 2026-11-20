package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ApplicationStatus
import com.example.model.CssProfileRequirement
import com.example.model.EnglishProficiency
import com.example.model.FinancialAidDocStatus
import com.example.model.ProgressMode
import com.example.model.SupplementStatus
import com.example.ui.theme.CollegeTrackerTheme
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.util.DateUtils

@Composable
fun EditorialProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progressFraction"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        if (animatedFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(fillColor)
            )
        }
    }
}

@Composable
fun ProgressModeToggle(
    selectedMode: ProgressMode,
    onModeSelected: (ProgressMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProgressMode.entries.forEach { mode ->
                val selected = mode == selectedMode
                val bgColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "toggleBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "toggleText"
                )
                val tag = if (mode == ProgressMode.WORD_WEIGHTED) {
                    "progress_mode_toggle_word"
                } else {
                    "progress_mode_toggle_count"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable(role = Role.RadioButton) { onModeSelected(mode) }
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag(tag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ApplicationStatusBadge(
    status: ApplicationStatus,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val (bg, fg) = when (status) {
        ApplicationStatus.ACCEPTED -> academic.acceptedContainer to academic.onAcceptedContainer
        ApplicationStatus.WAITLISTED -> academic.waitlistedContainer to academic.onWaitlistedContainer
        ApplicationStatus.IN_PROGRESS -> academic.inProgressContainer to academic.onInProgressContainer
        ApplicationStatus.NOT_STARTED -> academic.neutralContainer to academic.onNeutralContainer
    }
    StatusPill(text = status.label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun SupplementStatusBadge(
    status: SupplementStatus,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val (bg, fg) = when (status) {
        SupplementStatus.READY_TO_SUBMIT -> academic.acceptedContainer to academic.onAcceptedContainer
        SupplementStatus.IN_PROGRESS -> academic.inProgressContainer to academic.onInProgressContainer
        SupplementStatus.NOT_STARTED -> academic.neutralContainer to academic.onNeutralContainer
    }
    StatusPill(text = status.label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun FinAidDocBadge(
    status: FinancialAidDocStatus,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val (bg, fg) = when (status) {
        FinancialAidDocStatus.DONE -> academic.acceptedContainer to academic.onAcceptedContainer
        FinancialAidDocStatus.STARTED -> academic.finAidContainer to academic.onFinAidContainer
        FinancialAidDocStatus.NOT_STARTED -> academic.neutralContainer to academic.onNeutralContainer
    }
    StatusPill(text = "Aid Docs: ${status.label}", containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun CssRequirementBadge(
    requirement: CssProfileRequirement,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val (bg, fg) = when (requirement) {
        CssProfileRequirement.EASY_ALTERNATIVE -> academic.acceptedContainer to academic.onAcceptedContainer
        CssProfileRequirement.ALTERNATIVE_WITH_MAIL -> academic.finAidContainer to academic.onFinAidContainer
        CssProfileRequirement.COMPULSORY -> academic.inProgressContainer to academic.onInProgressContainer
        CssProfileRequirement.VERY_HARD_ALTERNATIVE -> academic.urgentContainer to academic.onUrgentContainer
    }
    StatusPill(text = "CSS: ${requirement.label}", containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun EnglishProficiencyBadge(
    policy: EnglishProficiency,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val (bg, fg) = when (policy) {
        EnglishProficiency.EASY_WAIVE -> academic.acceptedContainer to academic.onAcceptedContainer
        EnglishProficiency.EMAIL_WAIVE -> academic.inProgressContainer to academic.onInProgressContainer
        EnglishProficiency.MOI_REQUIRED -> academic.finAidContainer to academic.onFinAidContainer
        EnglishProficiency.REQUIRED -> academic.urgentContainer to academic.onUrgentContainer
    }
    StatusPill(text = "Eng: ${policy.label}", containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun DeadlineCountdownBadge(
    deadlineMillis: Long?,
    prefix: String = "",
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val days = DateUtils.daysUntilUtcDate(deadlineMillis)
    val label = if (deadlineMillis == null) {
        "${prefix}No date"
    } else {
        val dateText = DateUtils.formatUtcDateCompact(deadlineMillis)
        val countText = DateUtils.countdownLabel(deadlineMillis)
        "$prefix$dateText · $countText"
    }
    val (bg, fg) = when {
        days == null -> academic.neutralContainer to academic.onNeutralContainer
        days <= 7 -> academic.urgentContainer to academic.onUrgentContainer
        days <= 21 -> academic.finAidContainer to academic.onFinAidContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MonoMetricStyle,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorialDatePickerField(
    label: String,
    selectedMillis: Long?,
    onDateSelected: (Long?) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (selectedMillis != null) {
                        DateUtils.formatUtcDateShort(selectedMillis)
                    } else {
                        "Tap to pick date"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedMillis != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (selectedMillis != null) {
                IconButton(
                    onClick = { onDateSelected(null) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedMillis ?: DateUtils.todayUtcDayStartMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateSelected(datePickerState.selectedDateMillis)
                        showDialog = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
