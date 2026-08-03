package com.mamer.steptrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mamer.steptrack.data.DailySteps
import com.mamer.steptrack.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: StepViewModel
) {
    val fullHistory by viewModel.fullHistory.collectAsStateWithLifecycle()
    val totalStepsMonth = fullHistory.sumOf { it.steps }
    val activeDays = fullHistory.size
    
    var selectedTab by remember { mutableStateOf("Harian") }

    val filteredHistory = remember(fullHistory, selectedTab) {
        when (selectedTab) {
            "Mingguan" -> groupHistoryByWeek(fullHistory)
            "Bulanan" -> groupHistoryByMonth(fullHistory)
            else -> fullHistory.sortedByDescending { it.date }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HistoryHeader()
            }

            item {
                HistorySummaryCard(totalStepsMonth, activeDays)
            }

            item {
                HistoryTabs(selectedTab) { selectedTab = it }
            }

            if (filteredHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Belum ada riwayat aktivitas",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(filteredHistory) { record ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        HistoryRecordCard(record = record, type = selectedTab)
                    }
                }
            }
        }
    }
}

private fun groupHistoryByWeek(list: List<DailySteps>): List<DailySteps> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    
    return list.groupBy { 
        val date = sdf.parse(it.date) ?: Date()
        cal.time = date
        // Use Year + Week of Year as key
        "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }.map { (_, weekSteps) ->
        val firstDay = weekSteps.minBy { it.date }
        DailySteps(
            date = firstDay.date, // Use first day date as base
            steps = weekSteps.sumOf { it.steps },
            target = weekSteps.sumOf { it.target },
            distance = weekSteps.sumOf { it.distance },
            calories = weekSteps.sumOf { it.calories },
            activeTimeMillis = weekSteps.sumOf { it.activeTimeMillis }
        )
    }.sortedByDescending { it.date }
}

private fun groupHistoryByMonth(list: List<DailySteps>): List<DailySteps> {
    return list.groupBy { 
        it.date.substring(0, 7) // yyyy-MM
    }.map { (monthKey, monthSteps) ->
        DailySteps(
            date = "$monthKey-01",
            steps = monthSteps.sumOf { it.steps },
            target = monthSteps.sumOf { it.target },
            distance = monthSteps.sumOf { it.distance },
            calories = monthSteps.sumOf { it.calories },
            activeTimeMillis = monthSteps.sumOf { it.activeTimeMillis }
        )
    }.sortedByDescending { it.date }
}

@Composable
fun HistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Riwayat",
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Catatan aktivitas harian",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* Handle filter */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun HistorySummaryCard(totalSteps: Int, days: Int) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = "Total Aktivitas Bulan Ini",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format(Locale.getDefault(), "%,d", totalSteps).replace(',', '.'),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$days hari aktivitas tersimpan",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistoryTabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val tabs = listOf("Harian", "Mingguan", "Bulanan")
        
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(listOf(Color(0xFFFF8730), Color(0xFFFF691F)))
                        } else {
                            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
                        }
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HistoryRecordCard(record: DailySteps, type: String) {
    val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val dateDisplay = when (type) {
        "Mingguan" -> {
             val cal = Calendar.getInstance()
             cal.time = sdfSource.parse(record.date) ?: Date()
             "Minggu ke-${cal.get(Calendar.WEEK_OF_YEAR)}, ${cal.get(Calendar.YEAR)}"
        }
        "Bulanan" -> {
             val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
             val date = sdfSource.parse(record.date)
             sdfMonth.format(date!!)
        }
        else -> {
             val sdfTarget = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.forLanguageTag("id-ID"))
             val date = sdfSource.parse(record.date)
             sdfTarget.format(date!!)
        }
    }

    val isAchieved = record.steps >= record.target

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = dateDisplay,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target ${String.format(Locale.getDefault(), "%,d", record.target).replace(',', '.')} langkah",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAchieved) SuccessBgLight else WarningBgLight)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isAchieved) "Tercapai" else "Belum",
                        color = if (isAchieved) SuccessGreen else WarningOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(value = String.format(Locale.getDefault(), "%,d", record.steps).replace(',', '.'), label = "Langkah", modifier = Modifier.weight(1f))
                StatBox(value = String.format(Locale.getDefault(), "%.1f km", record.distance / 1000.0), label = "Jarak", modifier = Modifier.weight(1f))
                StatBox(value = record.calories.toInt().toString(), label = "Kalori", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
