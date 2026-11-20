package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GeminiModelOption
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateTextSecondary

@Composable
fun CompactModelSelectorRow(
    selectedModel: GeminiModelOption,
    enabled: Boolean,
    onSelectModel: (GeminiModelOption) -> Unit,
    modifier: Modifier = Modifier,
    triggerTestTag: String = "model_selector_trigger",
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Memory,
                contentDescription = null,
                tint = SlateTextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.model_selector_label),
                style = MaterialTheme.typography.labelMedium,
                color = SlateTextSecondary,
            )
        }

        Box {
            Surface(
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(triggerTestTag),
                shape = RoundedCornerShape(12.dp),
                color = MidnightSurface,
                border = BorderStroke(1.dp, SlateBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = selectedModel.modelId,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(SlateSurfaceElevated),
            ) {
                GeminiModelOption.entries.forEach { option ->
                    val isSelected = option == selectedModel
                    DropdownMenuItem(
                        modifier = Modifier.testTag("model_option_${option.modelId}"),
                        text = {
                            Column {
                                Text(
                                    text = option.modelId,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) ElectricCyan else IceWhite,
                                )
                                Text(
                                    text = option.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateTextSecondary,
                                )
                            }
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelectModel(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun FallbackStatusBanner(
    requestedModelId: String,
    answeredModelId: String,
    modifier: Modifier = Modifier,
    testTag: String = "fallback_status_note",
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = CyanContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(
                    R.string.fallback_answered_note,
                    requestedModelId,
                    answeredModelId,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = IceWhite,
            )
        }
    }
}
