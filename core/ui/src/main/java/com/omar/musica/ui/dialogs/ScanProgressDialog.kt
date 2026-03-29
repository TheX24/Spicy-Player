package com.omar.musica.ui.dialogs

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.omar.musica.store.ScanProgress

@Composable
fun ScanProgressDialog(
    isScanning: Boolean,
    scanProgress: ScanProgress,
    scanHistory: List<String>
) {
    if (!isScanning) return

    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = { /* Prevent dismiss during scan */ }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Library Synchronizing",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .animateContentSize()
                            .padding(16.dp)
                    ) {
                        scanHistory.forEach { historyItem ->
                            Text(
                                text = historyItem,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { 
                                if (scanProgress.totalCount > 0) 
                                    scanProgress.currentCount.toFloat() / scanProgress.totalCount.toFloat()
                                else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = colorScheme.primary,
                            trackColor = colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val scanText = remember(scanProgress) {
                            if (scanProgress.summary.isNotEmpty()) scanProgress.summary else scanProgress.phase
                        }
                        
                        Text(
                            text = "Scan: $scanText",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (scanProgress.isUpdating) {
                            Text(
                                text = "Syncing with media session...",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                
                // Background usage info
                val isIgnoringBatteryOptimizations = remember {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        pm.isIgnoringBatteryOptimizations(context.packageName)
                    } else true
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isIgnoringBatteryOptimizations) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.BatteryAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unrestrict Background Sync", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp), tint = colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Background Settings", style = MaterialTheme.typography.labelLarge, color = colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
