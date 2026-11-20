package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ApplicationStatus
import com.example.model.FinancialAidDocStatus
import com.example.model.ProgressMetrics
import com.example.model.ProgressMode
import com.example.model.University
import com.example.ui.components.ApplicationStatusBadge
import com.example.ui.components.DeadlineCountdownBadge
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.FinAidDocBadge
import com.example.ui.components.ProgressModeToggle
import com.example.ui.state.TrackerSnapshot
import com.example.ui.theme.CollegeTrackerTheme
import com.example.ui.theme.MonoMetricStyle

private data class UpcomingDeadlineItem(
    val university: University,
    val isFinancialAid: Boolean,
    val millis: Long
)

@Composable
fun DashboardScreen(
    snapshot: TrackerSnapshot,
    onSelectProgressMode: (ProgressMode) -> Unit,
    onOpenUniversity: (String) -> Unit,
    onAddSchool: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val mode = snapshot.progressMode
    val overall = snapshot.overallProgress

    val acceptedCount = remember(snapshot.universities) {
        snapshot.universities.count { it.statusEnum == ApplicationStatus.ACCEPTED }
    }
    val waitlistedCount = remember(snapshot.universities) {
        snapshot.universities.count { it.statusEnum == ApplicationStatus.WAITLISTED }
    }
    val inProgressCount = remember(snapshot.universities) {
        snapshot.universities.count { it.statusEnum == ApplicationStatus.IN_PROGRESS }
    }
    val aidDoneCount = remember(snapshot.universities) {
        snapshot.universities.count { it.finAidStatusEnum == FinancialAidDocStatus.DONE }
    }

    val upcomingDeadlines = remember(snapshot.universities) {
        snapshot.universities.flatMap { uni ->
            buildList {
                uni.applicationDeadlineMillis?.let {
                    add(UpcomingDeadlineItem(uni, isFinancialAid = false, millis = it))
                }
                uni.financialAidDeadlineMillis?.let {
                    add(UpcomingDeadlineItem(uni, isFinancialAid = true, millis = it))
                }
            }
        }.sortedBy { it.millis }.take(4)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OverallReadinessHeroCard(
                    overall = overall,
                    mode = mode,
                    schoolCount = snapshot.universities.size,
                    onSelectProgressMode = onSelectProgressMode
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Accepted",
                        value = acceptedCount.toString(),
                        accentColor = academic.accepted,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "In Progress",
                        value = inProgressCount.toString(),
                        accentColor = academic.inProgress,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Waitlisted",
                        value = waitlistedCount.toString(),
                        accentColor = academic.waitlisted,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Aid Done",
                        value = "$aidDoneCount/${snapshot.universities.size}",
                        accentColor = academic.finAidAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (snapshot.universities.isEmpty()) {
                item {
                    EmptyDashboardCard(onAddSchool = onAddSchool)
                }
            } else {
                if (upcomingDeadlines.isNotEmpty()) {
                    item {
                        UpcomingDeadlinesCard(
                            items = upcomingDeadlines,
                            onOpenUniversity = onOpenUniversity,
                            onOpenCalendar = onOpenCalendar
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "University Readiness",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (mode == ProgressMode.WORD_WEIGHTED) {
                                    "Weighted by Ready-to-Submit essay word limits"
                                } else {
                                    "Based on Ready-to-Submit supplement count"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(snapshot.universities, key = { it.id }) { uni ->
                    val metrics = snapshot.progressByUniversity[uni.id]
                        ?: ProgressMetrics(0, 0, 0, 0, 0, 0, 0f, 0f)
                    UniversityProgressCard(
                        university = uni,
                        metrics = metrics,
                        mode = mode,
                        onClick = { onOpenUniversity(uni.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun OverallReadinessHeroCard(
    overall: ProgressMetrics,
    mode: ProgressMode,
    schoolCount: Int,
    onSelectProgressMode: (ProgressMode) -> Unit
) {
    val academic = CollegeTrackerTheme.academicColors
    val percent = overall.percentFor(mode)
    val fraction = overall.fractionFor(mode)
    val barColor = if (percent >= 80) academic.accepted else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overall_progress_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OVERALL READINESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (mode == ProgressMode.WORD_WEIGHTED) {
                                "${overall.readyWeight} / ${overall.totalWeight} words"
                            } else {
                                "${overall.readyCount} / ${overall.totalCount} essays"
                            },
                            style = MonoMetricStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            ProgressModeToggle(
                selectedMode = mode,
                onModeSelected = onSelectProgressMode
            )

            EditorialProgressBar(
                fraction = fraction,
                height = 12.dp,
                fillColor = barColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$schoolCount ${if (schoolCount == 1) "school" else "schools"}",
                    style = MonoMetricStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${overall.readyCount} ready · ${overall.inProgressCount} drafting · ${overall.notStartedCount} pending",
                    style = MonoMetricStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UpcomingDeadlinesCard(
    items: List<UpcomingDeadlineItem>,
    onOpenUniversity: (String) -> Unit,
    onOpenCalendar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Upcoming Deadlines",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenCalendar() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Calendar",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Open Calendar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .clickable { onOpenUniversity(item.university.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.university.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (item.isFinancialAid) "Financial Aid Deadline" else "Application Deadline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    DeadlineCountdownBadge(deadlineMillis = item.millis)
                }
            }
        }
    }
}

@Composable
private fun UniversityProgressCard(
    university: University,
    metrics: ProgressMetrics,
    mode: ProgressMode,
    onClick: () -> Unit
) {
    val academic = CollegeTrackerTheme.academicColors
    val percent = metrics.percentFor(mode)
    val fraction = metrics.fractionFor(mode)
    val barColor = if (percent >= 80) academic.accepted else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("dashboard_uni_card_${university.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = university.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${metrics.readyCount}/${metrics.totalCount} ready · ${metrics.readyWeight}/${metrics.totalWeight} words",
                            style = MonoMetricStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleLarge,
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }

            EditorialProgressBar(
                fraction = fraction,
                height = 9.dp,
                fillColor = barColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ApplicationStatusBadge(status = university.statusEnum)
                FinAidDocBadge(status = university.finAidStatusEnum)
                if (university.applicationDeadlineMillis != null) {
                    DeadlineCountdownBadge(deadlineMillis = university.applicationDeadlineMillis)
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboardCard(onAddSchool: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "Start Your College List",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Add your target universities, supplement word limits, financial aid deadlines, and brainstorming notes to see live readiness bars.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddSchool,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_add_school_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add First University")
            }
        }
    }
}
