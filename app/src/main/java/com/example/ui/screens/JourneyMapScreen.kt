package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JourneyData
import com.example.data.StageModel
import com.example.data.TaskModel
import com.example.viewmodel.JourneyViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun JourneyMapScreen(
    viewModel: JourneyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val completedTaskIds by viewModel.completedTaskIds.collectAsState()
    val progress by viewModel.userProgress.collectAsState()
    val selectedStageId by viewModel.selectedStageId.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var savingsInput by remember { mutableStateOf("") }

    // Constants for calculation
    val totalTasksList = JourneyData.stages.flatMap { it.tasks }
    val totalTasksCount = totalTasksList.size
    val completedCount = totalTasksList.count { completedTaskIds.contains(it.id) }
    val totalProgressRatio = if (totalTasksCount > 0) completedCount.toFloat() / totalTasksCount else 0f

    val selectedStage = JourneyData.stages.firstOrNull { it.id == selectedStageId } ?: JourneyData.stages[0]

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // High fidelity styled dashboard header
            JourneyHeader(
                completedCount = completedCount,
                totalCount = totalTasksCount,
                ratio = totalProgressRatio,
                savings = progress.totalSavingsJod,
                onAddSavingsClick = { showAddSavingsDialog = true },
                onResetClick = { showResetDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFFDF8FD)), // Artistic Flair Soft Lavender Canvas
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 1. CARTOON PLAYFUL MAP CANVAS (Occupies top portion)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFFEADDFF), RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFEF7FF), // Extremely light lavender-pink
                                Color(0xFFF3EDF7)  // Soft warm grey-lavender
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CartoonMap(
                    stages = JourneyData.stages,
                    activeStageId = progress.currentStageId,
                    selectedStageId = selectedStageId,
                    completedTaskIds = completedTaskIds,
                    onStageClick = { stageId ->
                        viewModel.selectStage(stageId)
                        Toast.makeText(context, "السيارة تتحرك نحو: ${JourneyData.stages[stageId - 1].title}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. STAGE TASKS SHEET PANEL (Occupies bottom portion)
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFFFEF7FF)) // Soft light elegant card panel
                    .border(1.dp, Color(0xFFEADDFF), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                StageDetailsPanel(
                    stage = selectedStage,
                    completedTaskIds = completedTaskIds,
                    isCarHere = progress.currentStageId == selectedStage.id,
                    onTaskToggle = { taskId ->
                        viewModel.toggleTask(taskId, selectedStage.id)
                    },
                    onMoveCarHere = {
                        viewModel.setCarStage(selectedStage.id)
                        Toast.makeText(context, "تم توجيه السيارة قانونياً لهنا!", Toast.LENGTH_SHORT).show()
                    },
                    currentCarStageId = progress.currentStageId
                )
            }
        }
    }

    // Interactive Dialogs
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    "إعادة تشغيل الرحلة؟",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "هل أنت متأكد من مسح جميع التقدم والمهام والعودة لنقطة الصفر في عمان بالدولار الصفري؟",
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.resetJourney()
                        showResetDialog = false
                    }
                ) {
                    Text("نعم، ابدأ من الصفر")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showAddSavingsDialog) {
        AlertDialog(
            onDismissRequest = { showAddSavingsDialog = false },
            title = {
                Text(
                    "محاكي الادخار للرحلة",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "أدخل المبلغ الذي وفرته مجاناً من عملك الحر بالأردن (دينار) لتحديث ميزانيتك المعنوية للمشروع:",
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = savingsInput,
                        onValueChange = { savingsInput = it },
                        label = { Text("المبلغ التوفيري (دينار / يورو)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = savingsInput.toDoubleOrNull() ?: 0.0
                        viewModel.addSavings(parsed)
                        savingsInput = ""
                        showAddSavingsDialog = false
                    }
                ) {
                    Text("إضافة للميزانية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSavingsDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun JourneyHeader(
    completedCount: Int,
    totalCount: Int,
    ratio: Float,
    savings: Double,
    onAddSavingsClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8FD)), // Gentle Soft Lavender Canvas background
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dashboard Title and resetting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onResetClick,
                    modifier = Modifier
                        .background(Color(0xFFE8DEF8), CircleShape) // Light Purple Container
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Journey",
                        tint = Color(0xFF6750A4), // Brand Purple
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "طريق ألمانيا 🇩🇪🚙",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF211A2D) // Dark violet-black
                        )
                    )
                    Text(
                        text = "من عمان إلى برلين • المسار الأخضر المالي",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4), // Primary purple
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Progress Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // High-fidelity Savings bubble matching HTML: bg-[#ffd8e4], color-[#7d5260]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD8E4))
                        .clickable { onAddSavingsClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "💸 المحفظة: ${String.format("%.1f", savings)} JOD",
                        color = Color(0xFF7D5260),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Completed text on the right
                Text(
                    text = "المهام المكتملة: $completedCount من أصل $totalCount (${(ratio * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF49454F) // Subtitle Grey
                    )
                )
            }

            // Elegant indicator bar matching HTML bg-[#eaddff] and foreground-[#6750a4]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEADDFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB18CFE), // Bright light purple
                                    Color(0xFF6750A4)  // Main classic purple
                                )
                            )
                        )
                )
            }

            // Uplifting motivational quote
            Text(
                text = when {
                    ratio == 0f -> "اضغط على الأردن 🇯🇴 بالخريطة وابدأ خطوتك الأولى مجاناً!"
                    ratio < 0.25f -> "بداية الرحلة تعتمد على الصبر ونقاط التعلم بالأردن 📚"
                    ratio < 0.50f -> "عمل رائع! أنت تبحث عن فرصة FSJ للتطوع وتعويض السفر ✈️"
                    ratio < 0.75f -> "أنت تغزو شوارع ألمانيا وتكسب اليورو! واصل الكفاح 💶🔨"
                    ratio < 1f -> "اقتربت جداً! أوراق الجنسية وملف التجنيس بانتظارك 🥳📜"
                    else -> "مبروك! حصلت على جنسية أقوى رابع جواز في العالم! 🇩🇪🦁"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CartoonMap(
    stages: List<StageModel>,
    activeStageId: Int,
    selectedStageId: Int,
    completedTaskIds: Set<String>,
    onStageClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthDp = maxWidth
        val heightDp = maxHeight

        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. DRAWING PATH & BACKGROUND DETAILS
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val roadOutlineColor = Color(0xFFEADDFF) // Beautiful Lavender Road outline matching design HTML
            val roadCoreColor = Color(0xFFFDF8FD)    // Light Lavender-White center road

            // Create curved roadmap path
            val path = Path()
            if (stages.isNotEmpty()) {
                val firstPoint = Offset(
                    stages[0].xRatio * canvasWidth,
                    stages[0].yRatio * canvasHeight
                )
                path.moveTo(firstPoint.x, firstPoint.y)

                for (i in 1 until stages.size) {
                    val current = Offset(stages[i].xRatio * canvasWidth, stages[i].yRatio * canvasHeight)
                    val prev = Offset(stages[i - 1].xRatio * canvasWidth, stages[i - 1].yRatio * canvasHeight)
                    
                    // Bezier anchor point to make curvy lines
                    val controlX = (prev.x + current.x) / 2f + (if (i % 2 == 0) 100f else -100f)
                    val controlY = (prev.y + current.y) / 2f

                    path.quadraticTo(controlX, controlY, current.x, current.y)
                }
            }

            // Draw shadow base road
            drawPath(
                path = path,
                color = roadOutlineColor,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            // Draw center road
            drawPath(
                path = path,
                color = roadCoreColor,
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // Draw dashed dividing road lanes matching HTML: stroke="#6750a4" stroke-dasharray="12 12"
            drawPath(
                path = path,
                color = Color(0xFF6750A4),
                style = Stroke(
                    width = 2f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            )

            // Draw cute natural map drawings (trees, planes, borders)
            // Jordan indicator text in the bottom
            val jorPin = Offset(stages[0].xRatio * canvasWidth, stages[0].yRatio * canvasHeight)
            drawCircle(Color(0x156750A4), radius = 60f, center = jorPin)

            // Germany indicator text in the top
            val gerPin = Offset(stages[5].xRatio * canvasWidth, stages[5].yRatio * canvasHeight)
            drawCircle(Color(0x157D5260), radius = 60f, center = gerPin)
        }

        // Cartoon Background elements
        // Sun on the top left
        Text(
            "☀️",
            fontSize = 32.sp,
            modifier = Modifier.offset(x = 24.dp, y = 24.dp)
        )

        // Cloud on the top right
        Text(
            "☁️",
            fontSize = 28.sp,
            modifier = Modifier.offset(x = (widthDp.value * 0.7f).dp, y = 80.dp)
        )

        // Jordan Landmark Emoji
        Text(
            "🏛️ Petra",
            fontSize = 11.sp,
            color = Color(0xFF7D5260),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(
                    x = (stages[0].xRatio * widthDp.value - 30f).dp,
                    y = (stages[0].yRatio * heightDp.value + 30f).dp
                )
                .background(Color(0xFFEADDFF), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp)
        )

        // German Berlin gate / Reichstag Emoji
        Text(
            "🏰 Berlin",
            fontSize = 11.sp,
            color = Color(0xFF6750A4),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(
                    x = (stages[5].xRatio * widthDp.value - 25f).dp,
                    y = (stages[5].yRatio * heightDp.value - 25f).dp
                )
                .background(Color(0xFFEADDFF), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp)
        )

        // Flying aircraft in middle
        Text(
            "✈️",
            fontSize = 24.sp,
            modifier = Modifier
                .offset(
                    x = (widthDp.value * 0.45f).dp,
                    y = (heightDp.value * 0.48f).dp
                )
                .rotate(-15f)
        )

        // 2. DRAWING STAGES NODES
        stages.forEach { stage ->
            val nodeX = stage.xRatio * widthDp.value
            val nodeY = stage.yRatio * heightDp.value

            // Calculate progress inside this specific stage
            val completedInStage = stage.tasks.count { completedTaskIds.contains(it.id) }
            val stageDone = completedInStage == stage.tasks.size
            val stageStarted = completedInStage > 0

            // Animate node color based on status (matching original logic with artistic style)
            val containerColor by animateColorAsState(
                targetValue = when {
                    stageDone -> Color(0xFF6750A4)     // Complete: Vibrant Brand Purple
                    stageStarted -> Color(0xFFFFD8E4)  // Started: Passionate Pink Accent
                    else -> Color(0xFFF3EDF7)          // Locked: Soft Lavender-Grey
                }
            )

            val borderStrokeColor = if (stage.id == selectedStageId) Color(0xFF6750A4) else Color(0xFFEADDFF)
            val borderSize by animateDpAsState(targetValue = if (stage.id == selectedStageId) 4.dp else 2.dp)
            val scaleCoef by animateFloatAsState(targetValue = if (stage.id == selectedStageId) 1.25f else 1.0f)

            Box(
                modifier = Modifier
                    .offset(
                        x = (nodeX - 22f).dp,
                        y = (nodeY - 22f).dp
                    )
                    .size(44.dp)
                    .scale(scaleCoef)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(borderSize, borderStrokeColor, CircleShape)
                    .clickable { onStageClick(stage.id) }
                    .testTag("stage_pin_${stage.id}"),
                contentAlignment = Alignment.Center
            ) {
                // Showing emojis inside nodes for gamified look
                Text(
                    text = stage.iconEmoji,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                // Small badge of completed tasks inside
                if (completedInStage > 0 && !stageDone) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .background(Color(0xFF7D5260), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$completedInStage",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. MOVING DECORATIVE CAR 🚗 WITH ANIMATION Physics
        // Locate matching stage positions for active location
        val activeStage = stages.firstOrNull { it.id == activeStageId } ?: stages[0]
        val carX = activeStage.xRatio * widthDp.value
        val carY = activeStage.yRatio * heightDp.value

        // Smooth physics-based interpolation
        val animatedCarX by animateFloatAsState(
            targetValue = carX,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f)
        )
        val animatedCarY by animateFloatAsState(
            targetValue = carY,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f)
        )

        // Draw car icon overlay with "أنت هنا" speech bubble as requested by design
        Column(
            modifier = Modifier
                .offset(
                    x = (animatedCarX - 25f).dp,
                    y = (animatedCarY - 54f).dp // Hover nicely above node
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant speech bubble/badge
            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(8.dp))
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFEADDFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "أنت هنا 📍",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF6750A4)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            // Big cute mirrored animated car
            Text(
                "🚗",
                fontSize = 26.sp,
                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f) // Mirrored car
            )
        }
    }
}

@Composable
fun StageDetailsPanel(
    stage: StageModel,
    completedTaskIds: Set<String>,
    isCarHere: Boolean,
    onTaskToggle: (String) -> Unit,
    onMoveCarHere: () -> Unit,
    currentCarStageId: Int
) {
    val completedCountInStage = stage.tasks.count { completedTaskIds.contains(it.id) }
    val stageProgressRatio = if (stage.tasks.isNotEmpty()) completedCountInStage.toFloat() / stage.tasks.size else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top card info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stage.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF211A2D) // Dark violet-black
                        )
                    )
                    Text(stage.iconEmoji, fontSize = 20.sp)
                }

                Text(
                    text = stage.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF49454F) // Subtitle grey
                    ),
                    maxLines = 2
                )
            }

            // Quick Badge Indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8DEF8)) // Light Purple Container
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stage.locationName,
                        color = Color(0xFF1D192B), // Dark text matching nav design
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Row of current status & car action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage.budgetInfo,
                color = Color(0xFF6750A4), // Brand primary purple matching design
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            if (!isCarHere) {
                // Allows users to manually force-drive progress car
                Button(
                    onClick = onMoveCarHere,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)), // Brand purple action
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Drive Here",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("قُد السيارة لهنا", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Car is here",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "السيارة تقف هنا 🚗",
                        color = Color(0xFF6750A4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Inner Stage Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LinearProgressIndicator(
                progress = { stageProgressRatio },
                color = Color(0xFF6750A4), // Purple progress
                trackColor = Color(0xFFEADDFF), // Lavender track
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Text(
                text = "${(stageProgressRatio * 100).toInt()}% منجز",
                color = Color(0xFF49454F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Tasks checklist LazyColumn
        Card(
            modifier = Modifier.fillMaxSize().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8FD)), // Soft Lavender card bg
            shape = RoundedCornerShape(16.dp),
            border = borderStrokeStyle()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(stage.tasks) { task ->
                    val isChecked = completedTaskIds.contains(task.id)
                    TaskItemRow(
                        task = task,
                        isChecked = isChecked,
                        onCheckedChange = { onTaskToggle(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun borderStrokeStyle() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = Color(0xFFEADDFF) // Light lavender border
)

@Composable
fun TaskItemRow(
    task: TaskModel,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val containerBg = if (isChecked) Color(0x106750A4) else Color(0x05000000)
    val taskTextColor = if (isChecked) Color(0xFF6750A4).copy(alpha = 0.6f) else Color(0xFF211A2D)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Detailed task text
        Text(
            text = task.title,
            color = taskTextColor,
            fontSize = 12.sp,
            fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.Right,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )

        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF6750A4), // Brand Purple check
                uncheckedColor = Color(0xFFEADDFF), // Lavender border
                checkmarkColor = Color.White
            ),
            modifier = Modifier.testTag("task_check_${task.id}")
        )
    }
}
