package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DailySteps
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepCounterDashboard(
    viewModel: StepViewModel,
    modifier: Modifier = Modifier
) {
    val todayRecord by viewModel.todaySteps.collectAsStateWithLifecycle()
    val historyList by viewModel.stepHistory.collectAsStateWithLifecycle()
    val sensorType by viewModel.activeSensorType.collectAsStateWithLifecycle()
    val isTracking by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    
    var showGoalDialog by remember { mutableStateOf(false) }
    var tempGoalInput by remember { mutableStateOf("") }
    
    val currentSteps = todayRecord?.steps ?: 0
    val currentTarget = todayRecord?.target ?: 10000
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val isTablet = maxWidth > 600.dp
            
            if (isTablet) {
                // Adaptive Bento Grid Layout: 2 Columns for Tablet
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Column: Header, Steps Hero & Info
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        BentoHeader(
                            isTracking = isTracking,
                            onToggleTracking = {
                                if (isTracking) viewModel.stopSensorService() else viewModel.startSensorService()
                            }
                        )
                        
                        StatusIndicatorCard(sensorType, isTracking)
                        
                        StepHeroBentoCard(
                            steps = currentSteps,
                            target = currentTarget,
                            onEditGoalClick = {
                                tempGoalInput = currentTarget.toString()
                                showGoalDialog = true
                            }
                        )
                    }
                    
                    // Right Column: Metric grid, progress chart & simulator tools
                    Column(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MetricBentoRow(steps = currentSteps)
                        
                        StepBarChart(
                            history = historyList,
                            currentDate = selectedDate,
                            onDaySelected = { viewModel.selectDate(it) }
                        )
                        
                        InteractiveDiagnosticsCard(
                            viewModel = viewModel,
                            currentSteps = currentSteps,
                            onEditGoalClick = {
                                tempGoalInput = currentTarget.toString()
                                showGoalDialog = true
                            }
                        )
                        
                        BentoFooterStatus(isTracking = isTracking)
                    }
                }
            } else {
                // Phone layout: Elegant vertical Bento Grid list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        BentoHeader(
                            isTracking = isTracking,
                            onToggleTracking = {
                                if (isTracking) viewModel.stopSensorService() else viewModel.startSensorService()
                            }
                        )
                    }
                    
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            StatusIndicatorCard(sensorType, isTracking)
                        }
                    }
                    
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            StepHeroBentoCard(
                                steps = currentSteps,
                                target = currentTarget,
                                onEditGoalClick = {
                                    tempGoalInput = currentTarget.toString()
                                    showGoalDialog = true
                                }
                            )
                        }
                    }
                    
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            MetricBentoRow(steps = currentSteps)
                        }
                    }
                    
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            StepBarChart(
                                history = historyList,
                                currentDate = selectedDate,
                                onDaySelected = { viewModel.selectDate(it) }
                            )
                        }
                    }
                    
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            InteractiveDiagnosticsCard(
                                viewModel = viewModel,
                                currentSteps = currentSteps,
                                onEditGoalClick = {
                                    tempGoalInput = currentTarget.toString()
                                    showGoalDialog = true
                                }
                            )
                        }
                    }
                    
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            BentoFooterStatus(isTracking = isTracking)
                        }
                    }
                }
            }
        }
    }
    
    if (showGoalDialog) {
        val dark = isSystemInDarkTheme()
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    "Set Daily Goal",
                    fontWeight = FontWeight.Bold,
                    color = if (dark) BentoTextPrimaryDark else BentoTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "How many steps do you aim to take each day?",
                        color = if (dark) BentoTextSecondaryDark else BentoTextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = tempGoalInput,
                        onValueChange = { tempGoalInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Daily Steps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = if (dark) BentoTextPrimaryDark else BentoTextPrimary,
                            unfocusedTextColor = if (dark) BentoTextPrimaryDark else BentoTextPrimary,
                            unfocusedContainerColor = if (dark) BentoCardBgDark else BentoCardBg,
                            focusedContainerColor = if (dark) BentoWeeklyCardBgDark else BentoWeeklyCardBg,
                            focusedLabelColor = if (dark) BentoMainCardProgressDark else BentoMainCardProgress,
                            cursorColor = if (dark) BentoMainCardProgressDark else BentoMainCardProgress
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
                        containerColor = if (dark) BentoMainCardProgressDark else BentoMainCardProgress
                    ),
                    modifier = Modifier.testTag("goal_confirm_button")
                ) {
                    Text("Save", color = if (dark) BentoMainCardText else Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoalDialog = false },
                    modifier = Modifier.testTag("goal_cancel_button")
                ) {
                    Text("Cancel", color = if (dark) BentoTextSecondaryDark else BentoTextSecondary)
                }
            },
            containerColor = if (dark) BentoCardBgDark else BentoCardBg
        )
    }
}

@Composable
fun BentoHeader(
    isTracking: Boolean,
    onToggleTracking: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val todayStr = dateFormat.format(Date())
    val dark = isSystemInDarkTheme()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = todayStr.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (dark) BentoTextSecondaryDark else BentoTextSecondary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (dark) BentoTextPrimaryDark else BentoTextPrimary,
                letterSpacing = (-0.5).sp
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onToggleTracking,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (dark) BentoWeeklyCardBgDark else BentoWeeklyCardBg)
                    .testTag("service_toggle_button")
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Toggle Service",
                    tint = if (isTracking) {
                        if (dark) BentoMainCardProgressDark else BentoMainCardProgress
                    } else {
                        if (dark) BentoTextSecondaryDark else BentoTextSecondary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Modern initial logo JD/MA
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (dark) BentoMainCardBgDark else BentoMainCardBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MA",
                    fontWeight = FontWeight.Bold,
                    color = if (dark) BentoMainCardTextDark else BentoMainCardText,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun StatusIndicatorCard(sensorType: String, isTracking: Boolean) {
    val dark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) BentoCardBgDark else BentoCardBg
        ),
        border = BorderStroke(
            1.dp,
            if (dark) BentoBorderColor.copy(alpha = 0.15f) else BentoBorderColor.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulse state dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isTracking) Color(0xFF34A853) else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SENSOR ACTIVE BACKBONE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) BentoTextSecondaryDark else BentoTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sensorType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (dark) BentoTextPrimaryDark else BentoTextPrimary
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isTracking) {
                            if (dark) BentoMainCardBgDark.copy(alpha = 0.4f) else BentoMainCardBg.copy(alpha = 0.4f)
                        } else {
                            Color.Red.copy(alpha = 0.1f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isTracking) "ACTIVE" else "PAUSED",
                    color = if (isTracking) {
                        if (dark) Color(0xFF5395FF) else BentoMainCardProgress
                    } else {
                        Color.Red
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StepHeroBentoCard(
    steps: Int,
    target: Int,
    onEditGoalClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) BentoMainCardBgDark else BentoMainCardBg
    val text = if (dark) BentoMainCardTextDark else BentoMainCardText
    val progressColor = if (dark) BentoMainCardProgressDark else BentoMainCardProgress
    
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(
            1.dp,
            text.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditGoalClick)
            .shadow(3.dp, RoundedCornerShape(28.dp))
            .testTag("step_progress_ring")
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "CURRENT STEPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = text.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DecimalFormat("#,###").format(steps),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = text,
                        fontSize = 44.sp,
                        letterSpacing = (-1.5).sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(text.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsWalk,
                        contentDescription = "Walk silhouette",
                        tint = progressColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bento thick Linear tracker
            val progressFraction = if (target > 0) (steps.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 50f),
                label = "Linear tracker animate"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(text.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val percent = (progressFraction * 100).toInt()
                Text(
                    text = "$percent% of your daily goal",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = text.copy(alpha = 0.8f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${DecimalFormat("#,###").format(target)} steps",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = text
                    )
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Goal",
                        tint = progressColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// Retain StepProgressRing for testing compat so GreetingScreenshotTest remains completely valid!
@Composable
fun StepProgressRing(
    steps: Int,
    target: Int,
    onEditGoalClick: () -> Unit
) {
    StepHeroBentoCard(steps = steps, target = target, onEditGoalClick = onEditGoalClick)
}

@Composable
fun MetricBentoRow(steps: Int) {
    val calories = steps * 0.04
    val distanceKm = (steps * 0.75) / 1000.0
    val activeMinutes = (distanceKm / 5.0 * 60.0).toInt()
    
    val dark = isSystemInDarkTheme()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoMetricCard(
                title = "Burned",
                value = "${DecimalFormat("#,##0").format(calories)} kcal",
                icon = Icons.Filled.LocalFireDepartment,
                iconBgColor = if (dark) BentoCalorieBgDark else BentoCalorieBg,
                iconColor = if (dark) BentoCalorieTextDark else BentoCalorieText,
                modifier = Modifier.weight(1f)
            )
            
            BentoMetricCard(
                title = "Distance",
                value = "${DecimalFormat("#,##0.0").format(distanceKm)} km",
                icon = Icons.Filled.LocationOn, // Standard LocationOn pin icon
                iconBgColor = if (dark) BentoDistanceBgDark else BentoDistanceBg,
                iconColor = if (dark) BentoDistanceTextDark else BentoDistanceText,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoMetricCard(
                title = "Active Time",
                value = "$activeMinutes mins",
                icon = Icons.Filled.Schedule,
                iconBgColor = if (dark) BentoActiveBgDark else BentoActiveBg,
                iconColor = if (dark) BentoActiveTextDark else BentoActiveText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BentoMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) BentoCardBgDark else BentoCardBg
        ),
        border = BorderStroke(
            1.dp,
            if (dark) BentoBorderColor.copy(alpha = 0.15f) else BentoBorderColor.copy(alpha = 0.1f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) BentoTextSecondaryDark else BentoTextSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) BentoTextPrimaryDark else BentoTextPrimary
                )
            }
        }
    }
}

@Composable
fun StepBarChart(
    history: List<DailySteps>,
    currentDate: String,
    onDaySelected: (String) -> Unit
) {
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dark = isSystemInDarkTheme()
    
    val maxSteps = (history.maxOfOrNull { it.steps } ?: 10000).coerceAtLeast(8000)
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) BentoWeeklyCardBgDark else BentoWeeklyCardBg
        ),
        border = BorderStroke(
            1.dp,
            if (dark) BentoBorderColor.copy(alpha = 0.15f) else BentoBorderColor.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .testTag("weekly_progress_chart")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weekly Progress",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dark) BentoTextPrimaryDark else BentoTextPrimary
                )
                Text(
                    "+12% vs last week",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected
                )
            }
            Text(
                "Tap a bar below to switch historical details",
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) BentoTextSecondaryDark else BentoTextSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                history.forEach { record ->
                    val isSelected = record.date == currentDate
                    val displaySteps = record.steps
                    
                    val heightRatio = (displaySteps.toFloat() / maxSteps.toFloat()).coerceIn(0.04f, 1f)
                    
                    val dayLabel = try {
                        val parsed = parseFormat.parse(record.date)
                        if (parsed != null) dayFormat.format(parsed).uppercase() else record.date.takeLast(2)
                    } catch (e: Exception) {
                        record.date.takeLast(2).uppercase()
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clickable { onDaySelected(record.date) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp * heightRatio)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) {
                                        if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected
                                    } else {
                                        if (dark) BentoWeeklyBarUnselectedDark else BentoWeeklyBarUnselected
                                    }
                                )
                                .testTag("chart_bar_${record.date}")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected
                            } else {
                                if (dark) BentoTextSecondaryDark else BentoTextSecondary
                            },
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveDiagnosticsCard(
    viewModel: StepViewModel,
    currentSteps: Int,
    onEditGoalClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dark) BentoCardBgDark else BentoCardBg
        ),
        border = BorderStroke(
            1.dp,
            if (dark) BentoBorderColor.copy(alpha = 0.15f) else BentoBorderColor.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .testTag("interactive_diagnostics_panel")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.OfflineBolt,
                    contentDescription = null,
                    tint = if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Simulator Tools",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dark) BentoTextPrimaryDark else BentoTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Simulate physical hardware walking in the cloud workspace below:",
                style = MaterialTheme.typography.bodySmall,
                color = if (dark) BentoTextSecondaryDark else BentoTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.addManualSteps(100) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) BentoWeeklyCardBgDark else BentoWeeklyCardBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("sim_100_btn")
                ) {
                    Text(
                        "+100", 
                        color = if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = { viewModel.addManualSteps(1000) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) BentoWeeklyCardBgDark else BentoWeeklyCardBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("sim_1000_btn")
                ) {
                    Text(
                        "+1,000", 
                        color = if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected, 
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = onEditGoalClick,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(
                            listOf(
                                if (dark) BentoMainCardProgressDark else BentoMainCardProgress,
                                if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected
                            )
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .minimumInteractiveComponentSize()
                        .testTag("sim_goal_btn")
                ) {
                    Text(
                        "Change Goal", 
                        color = if (dark) BentoTextPrimaryDark else BentoTextPrimary, 
                        fontSize = 11.sp, 
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun BentoFooterStatus(isTracking: Boolean) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34A853))
            )
            Text(
                text = "Pedometer Sensor Active",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (dark) BentoTextSecondaryDark else BentoTextSecondary
            )
        }
        
        Text(
            text = "HISTORY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (dark) BentoWeeklyBarSelectedDark else BentoWeeklyBarSelected,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(4.dp)
        )
    }
}
