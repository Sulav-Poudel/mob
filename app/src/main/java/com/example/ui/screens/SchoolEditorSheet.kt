package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.UniversityInput
import com.example.model.ApplicationStatus
import com.example.model.CssProfileRequirement
import com.example.model.EnglishProficiency
import com.example.model.FinancialAidDocStatus
import com.example.model.University
import com.example.ui.components.EditorialDatePickerField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SchoolEditorSheet(
    existingUniversity: University?,
    existingSupplementCount: Int,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (UniversityInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(existingUniversity?.id) { mutableStateOf(existingUniversity?.name ?: "") }
    var appDeadlineMillis by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.applicationDeadlineMillis)
    }
    var appStatus by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.statusEnum ?: ApplicationStatus.NOT_STARTED)
    }
    var targetSuppsText by remember(existingUniversity?.id) {
        mutableStateOf((existingUniversity?.targetCountInt ?: 2).toString())
    }
    var finAidDeadlineMillis by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.financialAidDeadlineMillis)
    }
    var cssRequirement by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.cssEnum ?: CssProfileRequirement.COMPULSORY)
    }
    var finAidStatus by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.finAidStatusEnum ?: FinancialAidDocStatus.NOT_STARTED)
    }
    var englishProf by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.englishEnum ?: EnglishProficiency.REQUIRED)
    }
    var collegeTraits by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity?.collegeTraits ?: "")
    }
    var autoCreateSlots by remember(existingUniversity?.id) {
        mutableStateOf(existingUniversity == null)
    }
    var nameError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingUniversity == null) "Add Target University" else "Edit University",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                label = { Text("University Name *") },
                placeholder = { Text("e.g., Stanford University") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("school_name_input")
            )

            SectionLabel("Application Status")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ApplicationStatus.entries.forEach { status ->
                    FilterChip(
                        selected = appStatus == status,
                        onClick = { appStatus = status },
                        label = { Text(status.label) }
                    )
                }
            }

            EditorialDatePickerField(
                label = "Application Deadline",
                selectedMillis = appDeadlineMillis,
                onDateSelected = { appDeadlineMillis = it },
                testTag = "school_app_deadline_picker"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = targetSuppsText,
                    onValueChange = { raw ->
                        targetSuppsText = raw.filter { it.isDigit() }.take(2)
                    },
                    label = { Text("No. of Supplements") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("school_supplements_count_input")
                )
            }

            val parsedCount = targetSuppsText.toIntOrNull()?.coerceIn(0, 50) ?: 0
            if ((existingUniversity == null && parsedCount > 0) ||
                (existingUniversity != null && parsedCount > existingSupplementCount)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = autoCreateSlots,
                        onCheckedChange = { autoCreateSlots = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (existingUniversity == null) {
                            "Auto-create $parsedCount starter supplement slots"
                        } else {
                            "Auto-create ${parsedCount - existingSupplementCount} missing supplement slots"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            EditorialDatePickerField(
                label = "Financial Aid Deadline",
                selectedMillis = finAidDeadlineMillis,
                onDateSelected = { finAidDeadlineMillis = it },
                testTag = "school_finaid_deadline_picker"
            )

            SectionLabel("CSS Profile Requirement")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CssProfileRequirement.entries.forEach { css ->
                    FilterChip(
                        selected = cssRequirement == css,
                        onClick = { cssRequirement = css },
                        label = { Text(css.label) }
                    )
                }
            }

            SectionLabel("Financial Aid Documents")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FinancialAidDocStatus.entries.forEach { docStatus ->
                    FilterChip(
                        selected = finAidStatus == docStatus,
                        onClick = { finAidStatus = docStatus },
                        label = { Text(docStatus.label) }
                    )
                }
            }

            SectionLabel("English Proficiency Test")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EnglishProficiency.entries.forEach { policy ->
                    FilterChip(
                        selected = englishProf == policy,
                        onClick = { englishProf = policy },
                        label = { Text(policy.label) }
                    )
                }
            }

            OutlinedTextField(
                value = collegeTraits,
                onValueChange = { collegeTraits = it.take(2000) },
                label = { Text("College-Specific Traits & Notes") },
                placeholder = { Text("Distinctive programs, labs, professors, campus vibe, alumni hooks...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("school_traits_input")
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) {
                            nameError = "Please enter the university name."
                            return@Button
                        }
                        onSave(
                            UniversityInput(
                                id = existingUniversity?.id,
                                name = trimmed,
                                applicationDeadlineMillis = appDeadlineMillis,
                                applicationStatus = appStatus,
                                targetSupplementCount = parsedCount,
                                financialAidDeadlineMillis = finAidDeadlineMillis,
                                cssProfileRequirement = cssRequirement,
                                financialAidDocStatus = finAidStatus,
                                englishProficiency = englishProf,
                                collegeTraits = collegeTraits,
                                autoCreateSupplementSlots = autoCreateSlots
                            )
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_school_button")
                ) {
                    Text(if (isSaving) "Saving..." else "Save School")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )
}
