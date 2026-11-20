package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.University
import com.example.ui.components.ApplicationStatusBadge
import com.example.ui.components.DeadlineCountdownBadge
import com.example.ui.components.FinAidDocBadge
import com.example.ui.components.StatusPill
import com.example.ui.state.TrackerSnapshot
import com.example.ui.theme.CollegeTrackerTheme
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.util.DateUtils

private enum class CalendarFilter(val label: String) {
    ALL("All Deadlines"),
    APPLICATION("University"),
    FINANCIAL_AID("Financial Aid")
}

private data class CalendarDeadlineEvent(
    val university: University,
    val isFinancialAid: Boolean,
    val dayMillis: Long
)

@Composable
fun CalendarScreen(
    snapshot: TrackerSnapshot,
    onOpenUniversity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    val initialYm = remember { DateUtils.currentYearMonth() }
    var visibleYear by rememberSaveable { mutableIntStateOf(initialYm.first) }
    var visibleMonth by rememberSaveable { mutableIntStateOf(initialYm.second) }
    var selectedDayMillis by rememberSaveable { mutableStateOf<Long?>(DateUtils.todayUtcDayStartMillis()) }
    var activeFilter by rememberSaveable { mutableStateOf(CalendarFilter.ALL) }
    var showEntireMonthAgenda by rememberSaveable { mutableStateOf(false) }

    val allEvents = remember(snapshot.universities, activeFilter) {
        snapshot.universities.flatMap { uni ->
            buildList {
                if (activeFilter != CalendarFilter.FINANCIAL_AID && uni.applicationDeadlineMillis != null) {
                    val (y, m, d) = DateUtils.utcYearMonthDay(uni.applicationDeadlineMillis)
                    add(
                        CalendarDeadlineEvent(
                            university = uni,
                            isFinancialAid = false,
                            dayMillis = DateUtils.utcDayStartMillis(y, m, d)
                        )
                    )
                }
                if (activeFilter != CalendarFilter.APPLICATION && uni.financialAidDeadlineMillis != null) {
                    val (y, m, d) = DateUtils.utcYearMonthDay(uni.financialAidDeadlineMillis)
                    add(
                        CalendarDeadlineEvent(
                            university = uni,
                            isFinancialAid = true,
                            dayMillis = DateUtils.utcDayStartMillis(y, m, d)
                        )
                    )
                }
            }
        }.sortedBy { it.dayMillis }
    }

    val eventsByDay = remember(allEvents) {
        allEvents.groupBy { it.dayMillis }
    }

    val monthEvents = remember(allEvents, visibleYear, visibleMonth) {
        allEvents.filter { ev ->
            val (y, m, _) = DateUtils.utcYearMonthDay(ev.dayMillis)
            y == visibleYear && m == visibleMonth
        }
    }

    val agendaEvents = remember(showEntireMonthAgenda, monthEvents, eventsByDay, selectedDayMillis) {
        if (showEntireMonthAgenda) {
            monthEvents
        } else {
            selectedDayMillis?.let { eventsByDay[it].orEmpty() }.orEmpty()
        }
    }

    val daysInMonth = remember(visibleYear, visibleMonth) { DateUtils.daysInMonth(visibleYear, visibleMonth) }
    val firstOffset = remember(visibleYear, visibleMonth) { DateUtils.firstDayOfWeekOffset(visibleYear, visibleMonth) }
    val todayMillis = remember { DateUtils.todayUtcDayStartMillis() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Month Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (visibleMonth == 0) {
                                        visibleMonth = 11
                                        visibleYear -= 1
                                    } else {
                                        visibleMonth -= 1
                                    }
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Previous month"
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = DateUtils.formatMonthYear(visibleYear, visibleMonth),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        val (y, m) = DateUtils.currentYearMonth()
                                        visibleYear = y
                                        visibleMonth = m
                                        selectedDayMillis = todayMillis
                                    }
                                ) {
                                    Text("Jump to Today")
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (visibleMonth == 11) {
                                        visibleMonth = 0
                                        visibleYear += 1
                                    } else {
                                        visibleMonth += 1
                                    }
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = "Next month"
                                )
                            }
                        }

                        // Day of week headers
                        val weekLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekLabels.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Calendar 7-column grid
                        val totalCells = firstOffset + daysInMonth
                        val rows = (totalCells + 6) / 7
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (rowIndex in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (colIndex in 0..6) {
                                        val cellNum = rowIndex * 7 + colIndex
                                        val dayOfMonth = cellNum - firstOffset + 1
                                        if (dayOfMonth in 1..daysInMonth) {
                                            val dayMillis = DateUtils.utcDayStartMillis(
                                                visibleYear,
                                                visibleMonth,
                                                dayOfMonth
                                            )
                                            val dayEvents = eventsByDay[dayMillis].orEmpty()
                                            val hasApp = dayEvents.any { !it.isFinancialAid }
                                            val hasAid = dayEvents.any { it.isFinancialAid }
                                            val isSelected = selectedDayMillis == dayMillis && !showEntireMonthAgenda
                                            val isToday = dayMillis == todayMillis

                                            CalendarDayCell(
                                                day = dayOfMonth,
                                                isSelected = isSelected,
                                                isToday = isToday,
                                                hasAppDeadline = hasApp,
                                                hasAidDeadline = hasAid,
                                                appDotColor = MaterialTheme.colorScheme.primary,
                                                aidDotColor = academic.finAidAccent,
                                                onClick = {
                                                    selectedDayMillis = dayMillis
                                                    showEntireMonthAgenda = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f).height(48.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = MaterialTheme.colorScheme.primary, label = "Application")
                            LegendDot(color = academic.finAidAccent, label = "Financial Aid")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showEntireMonthAgenda) {
                            "All in ${DateUtils.formatMonthYear(visibleYear, visibleMonth)} (${monthEvents.size})"
                        } else {
                            "Due ${DateUtils.formatUtcDateShort(selectedDayMillis)} (${agendaEvents.size})"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    FilterChip(
                        selected = showEntireMonthAgenda,
                        onClick = { showEntireMonthAgenda = !showEntireMonthAgenda },
                        label = {
                            Text(if (showEntireMonthAgenda) "Day View" else "Full Month (${monthEvents.size})")
                        }
                    )
                }
            }

            if (agendaEvents.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (showEntireMonthAgenda) {
                                    "No deadlines scheduled in this month."
                                } else {
                                    "No deadlines on this date."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!showEntireMonthAgenda && monthEvents.isNotEmpty()) {
                                TextButton(onClick = { showEntireMonthAgenda = true }) {
                                    Text("View all ${monthEvents.size} deadlines this month")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    agendaEvents,
                    key = { "${it.university.id}_${it.isFinancialAid}_${it.dayMillis}" }
                ) { event ->
                    CalendarAgendaCard(
                        event = event,
                        onClick = { onOpenUniversity(event.university.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasAppDeadline: Boolean,
    hasAidDeadline: Boolean,
    appDotColor: Color,
    aidDotColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = day.toString(),
                style = MonoMetricStyle,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.height(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasAppDeadline) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else appDotColor)
                    )
                }
                if (hasAidDeadline) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else aidDotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarAgendaCard(
    event: CalendarDeadlineEvent,
    onClick: () -> Unit
) {
    val academic = CollegeTrackerTheme.academicColors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.university.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                DeadlineCountdownBadge(deadlineMillis = event.dayMillis)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.isFinancialAid) {
                    StatusPill(
                        text = "Financial Aid Deadline",
                        containerColor = academic.finAidContainer,
                        contentColor = academic.onFinAidContainer
                    )
                    FinAidDocBadge(status = event.university.finAidStatusEnum)
                } else {
                    StatusPill(
                        text = "Application Deadline",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    ApplicationStatusBadge(status = event.university.statusEnum)
                }
            }
        }
    }
}
