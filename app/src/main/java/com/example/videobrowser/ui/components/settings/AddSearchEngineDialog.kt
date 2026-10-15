package com.example.videobrowser.ui.components.settings

import android.webkit.URLUtil
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.videobrowser.model.CustomSearchEngine
import java.util.UUID

@Composable
fun AddSearchEngineDialog(
    initialEngine: CustomSearchEngine? = null,
    onDismiss: () -> Unit,
    onSaveEngine: (CustomSearchEngine) -> Unit
) {
    var name by remember { mutableStateOf(initialEngine?.displayName ?: "") }
    var urlTemplate by remember { mutableStateOf(initialEngine?.searchUrlTemplate ?: "https://") }
    var homeUrl by remember { mutableStateOf(initialEngine?.homeUrl ?: "") }
    var testedUrlResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialEngine == null) "Add Custom Search Engine" else "Edit Search Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Engine Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Engine Name (e.g. Kagi, Startpage)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_engine_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // URL Template
                OutlinedTextField(
                    value = urlTemplate,
                    onValueChange = {
                        urlTemplate = it
                        errorMessage = null
                        testedUrlResult = null
                    },
                    label = { Text("Search URL Template") },
                    placeholder = { Text("https://example.com/search?q=%s") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_engine_url_input")
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tip: Use %s or {searchTerms} where search queries should be inserted.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Home URL (Optional)
                OutlinedTextField(
                    value = homeUrl,
                    onValueChange = { homeUrl = it },
                    label = { Text("Home URL (Optional)") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Test URL Template Button
                OutlinedButton(
                    onClick = {
                        val tempEngine = CustomSearchEngine(
                            id = "test",
                            displayName = name.ifBlank { "Test" },
                            searchUrlTemplate = urlTemplate,
                            homeUrl = homeUrl
                        )
                        testedUrlResult = tempEngine.buildSearchUrl("android jetpack compose")
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Configuration ('android jetpack compose')", fontSize = 12.sp)
                }

                testedUrlResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Generated Query URL:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = result,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedUrl = urlTemplate.trim()

                            if (trimmedName.isBlank()) {
                                errorMessage = "Please enter an engine name."
                                return@Button
                            }
                            if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                                errorMessage = "URL must start with http:// or https://"
                                return@Button
                            }

                            val engine = CustomSearchEngine(
                                id = initialEngine?.id ?: "engine_${UUID.randomUUID().toString().take(8)}",
                                displayName = trimmedName,
                                searchUrlTemplate = trimmedUrl,
                                homeUrl = homeUrl.trim().ifEmpty { trimmedUrl },
                                hint = "Custom search engine",
                                isBuiltIn = false
                            )
                            onSaveEngine(engine)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_custom_engine_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Engine", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
