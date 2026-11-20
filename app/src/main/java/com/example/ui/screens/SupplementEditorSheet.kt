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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.SupplementInput
import com.example.model.Supplement
import com.example.model.SupplementStatus
import com.example.model.SupplementType
import com.example.model.WordBucket

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SupplementEditorSheet(
    universityId: String,
    universityName: String,
    existingSupplement: Supplement?,
    availableTypes: List<SupplementType>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (SupplementInput) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(existingSupplement?.id) { mutableStateOf(existingSupplement?.title ?: "") }
    var status by remember(existingSupplement?.id) {
        mutableStateOf(existingSupplement?.statusEnum ?: SupplementStatus.NOT_STARTED)
    }
    var wordBucket by remember(existingSupplement?.id) {
        mutableStateOf(existingSupplement?.wordBucketEnum ?: WordBucket.W250)
    }
    var selectedEssayType by remember(existingSupplement?.id) {
        mutableStateOf(existingSupplement?.essayType ?: "")
    }
    var customTypeDraft by remember(existingSupplement?.id) { mutableStateOf("") }
    var prompt by remember(existingSupplement?.id) { mutableStateOf(existingSupplement?.prompt ?: "") }
    var brainstormNotes by remember(existingSupplement?.id) {
        mutableStateOf(existingSupplement?.brainstormNotes ?: "")
    }

    // Build combined chip list: saved catalog types + current supplement's type if not yet in catalog
    val displayTypeNames = remember(availableTypes, selectedEssayType) {
        val list = availableTypes.map { it.name }.toMutableList()
        if (selectedEssayType.isNotBlank() && list.none { it.equals(selectedEssayType, ignoreCase = true) }) {
            list.add(selectedEssayType)
        }
        list
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = universityName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (existingSupplement == null) "Add Supplement" else "Edit Supplement",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (existingSupplement != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(existingSupplement.id) },
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Supplement",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                label = { Text("Label / Title (Optional)") },
                placeholder = { Text("e.g., Supplement 1 or Intellectual Vitality") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supplement_title_input")
            )

            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SupplementStatus.entries.forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        label = { Text(st.label) }
                    )
                }
            }

            Text(
                text = "Word Limit Bucket",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WordBucket.entries.forEach { bucket ->
                    FilterChip(
                        selected = wordBucket == bucket,
                        onClick = { wordBucket = bucket },
                        label = { Text(bucket.label) }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Essay Type (Select existing or write Other)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (displayTypeNames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        displayTypeNames.forEach { typeName ->
                            val isSelected = selectedEssayType.equals(typeName, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedEssayType = if (isSelected) "" else typeName
                                },
                                label = { Text(typeName) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customTypeDraft,
                        onValueChange = { customTypeDraft = it.take(80) },
                        label = { Text("Other / Custom Type") },
                        placeholder = { Text("Type new category (saved for all schools)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_type_input")
                    )
                    OutlinedButton(
                        onClick = {
                            val trimmed = customTypeDraft.trim()
                            if (trimmed.isNotEmpty()) {
                                selectedEssayType = trimmed
                                customTypeDraft = ""
                            }
                        },
                        enabled = customTypeDraft.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("add_custom_type_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Use")
                    }
                }
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(3000) },
                label = { Text("Essay Prompt") },
                placeholder = { Text("Paste the official university prompt here...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supplement_prompt_input")
            )

            OutlinedTextField(
                value = brainstormNotes,
                onValueChange = { brainstormNotes = it.take(10000) },
                label = { Text("Brainstorming Notes") },
                placeholder = { Text("Outline your story, specific courses/professors, themes, and key takeaways...") },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("supplement_notes_input")
            )

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
                        // If user typed in customTypeDraft without tapping "Use", apply it automatically
                        val finalEssayType = customTypeDraft.trim().ifEmpty { selectedEssayType.trim() }
                        onSave(
                            SupplementInput(
                                id = existingSupplement?.id,
                                universityId = universityId,
                                title = title.trim(),
                                status = status,
                                wordBucket = wordBucket,
                                essayType = finalEssayType,
                                prompt = prompt,
                                brainstormNotes = brainstormNotes
                            )
                        )
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_supplement_button")
                ) {
                    Text(if (isSaving) "Saving..." else "Save Supplement")
                }
            }
        }
    }
}
