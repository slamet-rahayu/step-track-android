package com.mamer.steptrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mamer.steptrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterDashboard(
    viewModel: StepViewModel,
    modifier: Modifier = Modifier,
    onNavigateToStats: () -> Unit = {}
) {
    val todayRecord by viewModel.todaySteps.collectAsStateWithLifecycle()
    val isTracking by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val activeSensor by viewModel.activeSensorType.collectAsStateWithLifecycle()
    
    var showGoalDialog by remember { mutableStateOf(false) }
    var tempGoalInput by remember { mutableStateOf("") }
    
    val currentSteps = todayRecord?.steps ?: 0
    val currentTarget = todayRecord?.target ?: 10000
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background effect from CSS (.phone::before)
            val topBgColor = if (isSystemInDarkTheme()) TopBgDark else TopBgLight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(268.dp)
                    .clip(RoundedCornerShape(bottomStart = 42.dp, bottomEnd = 42.dp))
                    .background(topBgColor)
            )

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp) // Space for bottom nav
            ) {
                item {
                    StepTrackHeader(sensorName = activeSensor)
                }
                
                item {
                    StepHeroCard(
                        steps = currentSteps,
                        target = currentTarget,
                        onEditGoalClick = {
                            tempGoalInput = currentTarget.toString()
                            showGoalDialog = true
                        }
                    )
                }
                
                item {
                    MetricRow(record = todayRecord)
                }
                
                item {
                    ActivityStatusCard(steps = currentSteps, target = currentTarget)
                }
                
                item {
                    MainActionButton(
                        text = "Lihat Statistik",
                        onClick = onNavigateToStats
                    )
                }
                
                // Keep simulation tools for development convenience
                item {
                    InteractiveDiagnosticsCard(viewModel = viewModel)
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    "Set Daily Goal",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        "How many steps do you aim to take each day?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = tempGoalInput,
                        onValueChange = { tempGoalInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Daily Steps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputGoal = tempGoalInput.toIntOrNull() ?: 10000
                        if (inputGoal > 0) {
                            viewModel.updateStepGoal(inputGoal)
                        }
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("goal_confirm_button")
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoalDialog = false },
                    modifier = Modifier.testTag("goal_cancel_button")
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(26.dp)
        )
    }
}
