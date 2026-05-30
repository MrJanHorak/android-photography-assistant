package com.janhorak.shutterdeck.gear.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.janhorak.shutterdeck.gear.domain.GearInsuranceSummary
import com.janhorak.shutterdeck.ui.components.ResultRow

@Composable
internal fun GearInsuranceExportCard(
    summary: GearInsuranceSummary,
    exportStatus: String?,
    hasInventoryItems: Boolean,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Owned inventory export",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Export saved gear items for insurance records. Values are exported exactly as entered; the app does not store a currency code yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ResultRow("Owned items", summary.itemCount.toString())
            ResultRow("Serial numbers saved", summary.itemsWithSerialNumber.toString())
            ResultRow("Missing serial numbers", summary.itemsMissingSerialNumber.toString())
            ResultRow("Current values saved", summary.itemsWithCurrentValue.toString())
            ResultRow("Missing current values", summary.itemsMissingCurrentValue.toString())
            ResultRow("Reference photos saved", summary.itemsWithReferencePhoto.toString())
            ResultRow("Total purchase value", formatMoney(summary.totalPurchaseValue))
            ResultRow("Total current value", formatMoney(summary.totalCurrentValue))
            if (exportStatus != null) {
                Text(
                    text = exportStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (exportStatus.startsWith("Couldn't") || exportStatus.startsWith("Add at least")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f),
                    enabled = hasInventoryItems,
                ) {
                    Text("Save CSV")
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f),
                    enabled = hasInventoryItems,
                ) {
                    Text("Save PDF")
                }
            }
            if (!hasInventoryItems) {
                Text(
                    text = "Add at least one saved gear item to export an insurance record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
