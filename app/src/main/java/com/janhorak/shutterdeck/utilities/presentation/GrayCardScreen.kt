package com.janhorak.shutterdeck.utilities.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.ui.effects.ReferenceDisplayMode
import com.janhorak.shutterdeck.utilities.domain.GrayCardReference

@Composable
fun GrayCardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedReferenceName by rememberSaveable { mutableStateOf(GrayCardReference.GRAY_18.name) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }

    val selectedReference = remember(selectedReferenceName) {
        GrayCardReference.valueOf(selectedReferenceName)
    }
    val referenceColor = remember(selectedReference) {
        Color(
            red = selectedReference.red,
            green = selectedReference.green,
            blue = selectedReference.blue,
        )
    }

    ReferenceDisplayMode(useDarkSystemBarIcons = selectedReference.useDarkSystemBarIcons)

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(referenceColor)
                .pointerInput(controlsVisible) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                },
        )

        if (controlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = selectedReference.label,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = selectedReference.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GrayCardReference.entries.forEach { reference ->
                                FilterChip(
                                    selected = reference == selectedReference,
                                    onClick = { selectedReferenceName = reference.name },
                                    label = { Text(reference.label) },
                                )
                            }
                        }
                        Text(
                            text = "Tap the background to hide or show controls. The screen stays awake and brightness is set to maximum while this reference is open.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "For the most accurate reference, use a standard or natural display profile if your device offers one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
