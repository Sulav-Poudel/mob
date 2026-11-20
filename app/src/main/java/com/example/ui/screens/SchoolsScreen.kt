package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ApplicationStatus
import com.example.model.ProgressMetrics
import com.example.model.ProgressMode
import com.example.model.University
import com.example.ui.components.ApplicationStatusBadge
import com.example.ui.components.CssRequirementBadge
import com.example.ui.components.DeadlineCountdownBadge
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EnglishProficiencyBadge
import com.example.ui.components.FinAidDocBadge
import com.example.ui.state.TrackerSnapshot
import com.example.ui.theme.CollegeTrackerTheme
import com.example.ui.theme.MonoMetricStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SchoolsScreen(
    snapshot: TrackerSnapshot,
    onOpenUniversity: (String) -> Unit,
    onAddSchool: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf<ApplicationStatus?>(null) }

    val filteredSchools = remember(snapshot.universities, searchQuery, selectedFilter) {
        snapshot.universities.filter { uni ->
            val matchesQuery = searchQuery.isBlank() ||
                uni.name.contains(searchQuery, ignoreCase = true) ||
                uni.collegeTraits.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedFilter == null || uni.statusEnum == selectedFilter
            matchesQuery && matchesStatus
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search target schools or traits...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schools_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${snapshot.universities.size})") }
                    )
                    ApplicationStatus.entries.forEach { status ->
                        val count = snapshot.universities.count { it.statusEnum == status }
                        FilterChip(
                            selected = selectedFilter == status,
                            onClick = {
                                selectedFilter = if (selectedFilter == status) null else status
                            },
                            label = { Text("${status.label} ($count)") }
                        )
                    }
                }
            }

            if (filteredSchools.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (snapshot.universities.isEmpty()) {
                                    "No target schools yet"
                                } else {
                                    "No schools match your filter"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (snapshot.universities.isEmpty()) {
                                    "Add universities to track supplements, CSS Profile rules, English waivers, and deadlines."
                                } else {
                                    "Try clearing your search or status filter."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (snapshot.universities.isEmpty()) {
                                Button(onClick = onAddSchool) {
                                    Icon(Icons.Filled.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add School")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredSchools, key = { it.id }) { uni ->
                    val metrics = snapshot.progressByUniversity[uni.id]
                        ?: ProgressMetrics(0, 0, 0, 0, 0, 0, 0f, 0f)
                    SchoolDirectoryCard(
                        university = uni,
                        metrics = metrics,
                        mode = snapshot.progressMode,
                        onClick = { onOpenUniversity(uni.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchoolDirectoryCard(
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
            .testTag("school_card_${university.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = university.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Supplements: ${metrics.totalCount} created (target ${university.targetCountInt}) · ${metrics.readyCount} ready",
                        style = MonoMetricStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                ApplicationStatusBadge(status = university.statusEnum)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorialProgressBar(
                    fraction = fraction,
                    height = 8.dp,
                    fillColor = barColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$percent%",
                    style = MonoMetricStyle,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (university.applicationDeadlineMillis != null) {
                    DeadlineCountdownBadge(
                        deadlineMillis = university.applicationDeadlineMillis,
                        prefix = "App: "
                    )
                }
                if (university.financialAidDeadlineMillis != null) {
                    DeadlineCountdownBadge(
                        deadlineMillis = university.financialAidDeadlineMillis,
                        prefix = "Aid: "
                    )
                }
                FinAidDocBadge(status = university.finAidStatusEnum)
                CssRequirementBadge(requirement = university.cssEnum)
                EnglishProficiencyBadge(policy = university.englishEnum)
            }

            if (university.collegeTraits.isNotBlank()) {
                Text(
                    text = university.collegeTraits,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
