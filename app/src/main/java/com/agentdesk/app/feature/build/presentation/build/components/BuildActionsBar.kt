package com.agentdesk.app.feature.build.presentation.build.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agentdesk.app.core.ui.theme.Spacing
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.build.domain.model.BuildStatus
import com.agentdesk.app.feature.build.domain.model.BuildVariant

@Composable
fun BuildActionsBar(
    isBuilding: Boolean,
    selectedVariant: BuildVariant,
    latestRun: BuildRun?,
    onVariantChange: (BuildVariant) -> Unit,
    onStartBuild: () -> Unit,
    onCancelBuild: () -> Unit,
    onInstallApk: () -> Unit,
    onExportApk: () -> Unit,
    onShareApk: () -> Unit,
    onFixWithAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isBuilding) {
            Button(
                onClick = onCancelBuild,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.small))
                Text("Cancel Build")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    FilterChip(
                        selected = selectedVariant == BuildVariant.DEBUG,
                        onClick = { onVariantChange(BuildVariant.DEBUG) },
                        label = { Text("Debug") }
                    )
                    Spacer(modifier = Modifier.width(Spacing.small))
                    FilterChip(
                        selected = selectedVariant == BuildVariant.RELEASE,
                        onClick = { onVariantChange(BuildVariant.RELEASE) },
                        label = { Text("Release") }
                    )
                }

                Button(onClick = onStartBuild) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.extraSmall))
                    Text("Build APK")
                }
            }
        }

        if (latestRun != null && !isBuilding) {
            Spacer(modifier = Modifier.height(Spacing.small))
            if (latestRun.status == BuildStatus.SUCCESS && latestRun.apkHostPath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onInstallApk,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.extraSmall))
                        Text("Install APK")
                    }

                    OutlinedButton(
                        onClick = onExportApk,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.extraSmall))
                        Text("Export")
                    }

                    IconButton(onClick = onShareApk) {
                        Icon(Icons.Default.Share, contentDescription = "Share APK")
                    }
                }
            } else if (latestRun.status == BuildStatus.FAILED) {
                FilledTonalButton(
                    onClick = onFixWithAi,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.small))
                    Text("Fix with AI")
                }
            }
        }
    }
}
