package com.omar.musica.settings.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun UpdateDialog(
    status: UpdateStatus,
    onClearStatus: () -> Unit,
    context: Context
) {
    when (status) {
        is UpdateStatus.NewVersion -> {
            AlertDialog(
                onDismissRequest = { onClearStatus() },
                title = { Text("New Update Available") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Version \${status.release.tagName} is now available!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        if (status.release.body.isNotEmpty()) {
                            Text(
                                text = parseMarkdown(status.release.body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.release.htmlUrl))
                        context.startActivity(intent)
                        onClearStatus()
                    }) {
                        Text("Download")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onClearStatus() }) {
                        Text("Not Now")
                    }
                }
            )
        }
        is UpdateStatus.UpToDate -> {
            if (status.isManual) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Gramophone is up to date!", Toast.LENGTH_SHORT).show()
                    onClearStatus()
                }
            } else {
                LaunchedEffect(Unit) {
                    onClearStatus()
                }
            }
        }
        is UpdateStatus.Error -> {
            if (status.isManual) {
                LaunchedEffect(status) {
                    Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                    onClearStatus()
                }
            } else {
                LaunchedEffect(status) {
                    onClearStatus()
                }
            }
        }
        UpdateStatus.Checking -> {
            // Loading indicator is shown via ButtonSettingItem subtitle usually, or toast
        }
        UpdateStatus.Idle -> {}
    }
}

@Composable
fun parseMarkdown(text: String): AnnotatedString {
    val titleSmall = MaterialTheme.typography.titleSmall
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            val currentLine = line.trim()
            
            if (currentLine.startsWith("##")) {
                withStyle(style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = titleSmall.fontSize
                )) {
                    append(currentLine.replace("#", "").trim())
                }
            } 
            else if (currentLine.startsWith("-")) {
                append("  • ")
                processInlineMarkdown(currentLine.substring(1).trim())
            }
            else {
                processInlineMarkdown(currentLine)
            }

            if (index < lines.size - 1) append("\n")
        }
    }
}

fun AnnotatedString.Builder.processInlineMarkdown(text: String) {
    val regex = """\*\*(.*?)\*\*""".toRegex()
    var lastMatchEnd = 0
    
    regex.findAll(text).forEach { matchResult ->
        append(text.substring(lastMatchEnd, matchResult.range.first))
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(matchResult.groupValues[1])
        }
        lastMatchEnd = matchResult.range.last + 1
    }
    append(text.substring(lastMatchEnd))
}
