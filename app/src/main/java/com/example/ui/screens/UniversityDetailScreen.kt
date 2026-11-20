package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.Supplement
import com.example.model.SupplementStatus
import com.example.model.University
import com.example.ui.components.CssRequirementBadge
import com.example.ui.components.DeadlineCountdownBadge
import com.example.ui.components.EditorialProgressBar
import com.example.ui.components.EnglishProficiencyBadge
import com.example.ui.components.ProgressModeToggle
import com.example.ui.components.StatusPill
import com.example.ui.components.SupplementStatusBadge
import com.example.ui.theme.CollegeTrackerTheme
import com.example.ui.theme.MonoMetricStyle
import com.example.ui.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UniversityDetailScreen(
    university: University,
    supplements: List<Supplement>,
    metrics: ProgressMetrics,
    progressMode: ProgressMode,
    onSelectProgressMode: (ProgressMode) -> Unit,
    onBack: () -> Unit,
    onEditSchool: () -> Unit,
    onDeleteSchool: () -> Unit,
    onUpdateApplicationStatus: (ApplicationStatus) -> Unit,
    onUpdateFinAidStatus: (FinancialAidDocStatus) -> Unit,
    onGenerateMissingSlots: () -> Unit,
    onAddSupplement: () -> Unit,
    onEditSupplement: (Supplement) -> Unit,
    onQuickCycleSupplementStatus: (Supplement) -> Unit,
    modifier: Modifier = Modifier
) {
    val academic = CollegeTrackerTheme.academicColors
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = university.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEditSchool,
                        modifier = Modifier.testTag("edit_school_button")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit school")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_school_button")
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete school",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Admissions & Status Card
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
                                Text(
                                    text = "Admissions Status",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                DeadlineCountdownBadge(
                                    deadlineMillis = university.applicationDeadlineMillis,
                                    prefix = "App: "
                                )
                            }

                            Text(
                                text = "Deadline: ${DateUtils.formatUtcDateShort(university.applicationDeadlineMillis)} · Target Supplements: ${university.targetCountInt}",
                                style = MonoMetricStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ApplicationStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = university.statusEnum == st,
                                        onClick = { onUpdateApplicationStatus(st) },
                                        label = { Text(st.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Financial Aid & Testing Card
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
                                        Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = academic.finAidAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Financial Aid & Testing",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                DeadlineCountdownBadge(
                                    deadlineMillis = university.financialAidDeadlineMillis,
                                    prefix = "Aid: "
                                )
                            }

                            Text(
                                text = "Financial Aid Deadline: ${DateUtils.formatUtcDateShort(university.financialAidDeadlineMillis)}",
                                style = MonoMetricStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CssRequirementBadge(requirement = university.cssEnum)
                                EnglishProficiencyBadge(policy = university.englishEnum)
                            }

                            Text(
                                text = "Financial Aid Documents",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FinancialAidDocStatus.entries.forEach { docSt ->
                                    FilterChip(
                                        selected = university.finAidStatusEnum == docSt,
                                        onClick = { onUpdateFinAidStatus(docSt) },
                                        label = { Text(docSt.label) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // College-Specific Traits Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onEditSchool() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "College-Specific Traits",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "Tap to edit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = university.collegeTraits.ifBlank {
                                    "No traits recorded yet. Tap to add distinctive programs, labs, professors, or campus notes."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (university.collegeTraits.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }

                item {
                    // School Supplement Progress Card
                    val percent = metrics.percentFor(progressMode)
                    val barColor = if (percent >= 80) academic.accepted else MaterialTheme.colorScheme.primary
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
                                Text(
                                    text = "Supplement Progress",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "$percent%",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = barColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            ProgressModeToggle(
                                selectedMode = progressMode,
                                onModeSelected = onSelectProgressMode
                            )

                            EditorialProgressBar(
                                fraction = metrics.fractionFor(progressMode),
                                height = 10.dp,
                                fillColor = barColor
                            )

                            Text(
                                text = "${metrics.readyCount}/${metrics.totalCount} Ready to Submit · ${metrics.readyWeight}/${metrics.totalWeight} weighted words",
                                style = MonoMetricStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (supplements.size < university.targetCountInt) {
                                val missing = university.targetCountInt - supplements.size
                                OutlinedButton(
                                    onClick = onGenerateMissingSlots,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create $missing Missing Supplement Slots")
                                }
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
                            text = "Supplements (${supplements.size})",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Button(
                            onClick = onAddSupplement,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_supplement_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Essay")
                        }
                    }
                }

                if (supplements.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "No supplements added yet",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tap Add Essay to record word limit, essay type, prompt, and brainstorming notes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(supplements, key = { it.id }) { supp ->
                        SupplementCard(
                            supplement = supp,
                            onClick = { onEditSupplement(supp) },
                            onCycleStatus = { onQuickCycleSupplementStatus(supp) }
                        )
                    }
                }

                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove ${university.name}?") },
            text = {
                Text("This will permanently remove this school and all ${supplements.size} of its supplements from your tracker.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSchool()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplementCard(
    supplement: Supplement,
    onClick: () -> Unit,
    onCycleStatus: () -> Unit
) {
    val displayTitle = supplement.title.ifBlank {
        supplement.essayType.ifBlank { "Untitled Supplement" }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("supplement_card_${supplement.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCycleStatus() }
                ) {
                    SupplementStatusBadge(status = supplement.statusEnum)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(
                    text = supplement.wordBucketEnum.label,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (supplement.essayType.isNotBlank()) {
                    StatusPill(
                        text = supplement.essayType,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (supplement.prompt.isNotBlank()) {
                Text(
                    text = supplement.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (supplement.brainstormNotes.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = supplement.brainstormNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
