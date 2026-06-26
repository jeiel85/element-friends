package com.elementfriends.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.TextStyle
import com.elementfriends.data.database.DiscoveredFormulaEntity
import com.elementfriends.data.model.*
import com.elementfriends.data.repository.MergeResult
import com.elementfriends.ui.audio.SoundSynth
import com.elementfriends.ui.theme.*
import com.elementfriends.ui.viewmodel.GameViewModel
import com.elementfriends.ui.viewmodel.GameUiState
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class DragState(
    val elementId: String,
    val initialPositionInRoot: Offset,
    val currentPositionInRoot: Offset
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    // Drag-and-Drop state
    var activeDragState by remember { mutableStateOf<DragState?>(null) }
    var slotLeftCenter by remember { mutableStateOf<Offset?>(null) }
    var slotRightCenter by remember { mutableStateOf<Offset?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Bottom tab state: 0 -> Element friends list, 1 -> Discovered formula book (도감), 2 -> Cloud Sync Settings
    var activeTab by remember { mutableStateOf(0) }
    
    // Input for backup restore code
    var restoreCodeInput by remember { mutableStateOf("") }

    // Clicked badge details popup state
    var selectedBadgeDetail by remember { mutableStateOf<ElementBadge?>(null) }

    // Kid-friendly step-by-step synthesis hint guidebook state
    var showGuideDialog by remember { mutableStateOf(false) }
    
    // Discovered formula book matching encyclopedia modal state
    var showMatchingDogamDialog by remember { mutableStateOf(false) }
    
    // Daily Hint Dialog state
    var showHintDialog by remember { mutableStateOf(false) }
    
    // Toast helper
    val showToast = { msg: String ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // Get current titles based on level
    val ageTitle = when {
        state.progress.level <= 1 -> "초보 조수 🧪"
        state.progress.level == 2 -> "인턴 과학자 🔍"
        state.progress.level == 3 -> "정식 연구원 🎓"
        state.progress.level == 4 -> "화학마스터 🌟"
        else -> "위대한 연금술사 👑"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PastelMint, BackgroundCream, SkyBluePastel)
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
        // --- 1. HEADER SECTION ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePaper.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "원소 친구들",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepMint,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🧪",
                                fontSize = 22.sp
                            )
                        }
                        Text(
                            text = "우당탕탕 파스텔 실험실",
                            fontSize = 12.sp,
                            color = SoftGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Level Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Interactive Guidance Replay Button
                        IconButton(
                            onClick = { viewModel.startTutorial() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PastelMint.copy(alpha = 0.5f))
                        ) {
                            Text("🎓", fontSize = 14.sp)
                        }

                        // Audio Toggle Button (BGM/Effects Music)
                        IconButton(
                            onClick = { viewModel.toggleAudio() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (state.isAudioEnabled) PastelMint.copy(alpha = 0.5f) else SoftGray.copy(alpha = 0.35f))
                        ) {
                            Text(if (state.isAudioEnabled) "🔊" else "🔇", fontSize = 14.sp)
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(LemonCream)
                                .border(1.5.dp, SecondaryPeach, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Level Icon",
                                tint = SecondaryPeach,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lv.${state.progress.level} $ageTitle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryPeach
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // EXP Progress Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "연구 점수",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorC,
                        modifier = Modifier.width(48.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .clip(CircleShape)
                            .background(PastelPink)
                    ) {
                        val animatedExpProgress by animateFloatAsState(
                            targetValue = if (state.progress.level > 0) state.progress.experience.toFloat() / 100f else 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "Exp Bar"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedExpProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(PrimaryMint, ColorH)
                                    )
                                )
                        )
                        Text(
                            text = "${state.progress.experience} / 100 XP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorC,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // --- KIDS TUTORIAL WELCOME HERO BANNER ---
        if (!state.progress.hasCompletedTutorial) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.startTutorial() },
                colors = CardDefaults.cardColors(containerColor = LemonCream),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, SecondaryPeach.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "🎓", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "꼬마 과학자 첫걸음 가이드! 🌟",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryPeach
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "터치와 드래그로 촉감 원소 합체하는 방법 배우기 (+50 XP!)",
                                fontSize = 11.sp,
                                color = ColorC.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.startTutorial() },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryPeach),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("시작 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // --- 2. INTERACTIVE WORKSPACE SECTION ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePaper.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Background Lab Bubbles Graphics to make it feel like a real vessel
                ChemistryVesselBackground(
                    animationPlaying = state.combineAnimationActive,
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Particle effect overlay when chemical combinations succeed
                PastelParticleEffectOverlay(
                    comboCount = state.comboCount,
                    modifier = Modifier.fillMaxSize()
                )

                // Fancy combo count badge
                if (state.comboCount > 0) {
                    ComboBadge(
                        comboCount = state.comboCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "원소 두 개를 골라 합체기로 가져오세요!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorC
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "💡 원소 캐릭터를 옆의 슬롯으로 드래그하면 합체가 시작돼요!",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryMint,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Interactive Bond Slots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Slot 1 (Left)
                        WorkspaceSlot(
                            elementId = state.workspaceLeft,
                            testTag = "workspace_left",
                            label = "수조 ①",
                            onClear = { viewModel.clearLeftSlot() },
                            combineAnimationActive = state.combineAnimationActive,
                            isLeft = true,
                            canCombine = state.workspaceLeft != null && state.workspaceRight != null,
                            onCombine = { viewModel.triggerCombine() },
                            onPositioned = { slotLeftCenter = it },
                            onMove = {
                                state.workspaceLeft?.let { elementId ->
                                    viewModel.setRightSlot(elementId)
                                    viewModel.clearLeftSlot()
                                }
                            }
                        )

                        // Magical Plus/Merge Effect Overlay
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(52.dp)
                        ) {
                            val pulseScale by rememberInfiniteTransition(label = "").animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )
                            Icon(
                                imageVector = Icons.Rounded.AddCircle,
                                contentDescription = "Combine sign",
                                tint = if (state.workspaceLeft != null && state.workspaceRight != null) SecondaryPeach else SoftGray.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(if (state.workspaceLeft != null && state.workspaceRight != null) pulseScale else 1f)
                            )
                        }

                        // Slot 2 (Right)
                        WorkspaceSlot(
                            elementId = state.workspaceRight,
                            testTag = "workspace_right",
                            label = "수조 ②",
                            onClear = { viewModel.clearRightSlot() },
                            combineAnimationActive = state.combineAnimationActive,
                            isLeft = false,
                            canCombine = state.workspaceLeft != null && state.workspaceRight != null,
                            onCombine = { viewModel.triggerCombine() },
                            onPositioned = { slotRightCenter = it },
                            onMove = {
                                state.workspaceRight?.let { elementId ->
                                    viewModel.setLeftSlot(elementId)
                                    viewModel.clearRightSlot()
                                }
                            }
                        )
                    }

                    // 合体 (Bond/Merge) Button
                    AnimatedVisibility(
                        visible = state.workspaceLeft != null && state.workspaceRight != null,
                        enter = scaleIn(spring(Spring.DampingRatioHighBouncy)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Button(
                            onClick = { viewModel.triggerCombine() },
                            enabled = !state.combineAnimationActive,
                            modifier = Modifier
                                .testTag("combine_button")
                                .height(52.dp)
                                .widthIn(min = 180.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryPeach,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(26.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Merge button icon",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "화학결합! ⚡",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Workspace helper row with guide button always handy for kids
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (state.workspaceLeft != null || state.workspaceRight != null) {
                            TextButton(
                                onClick = { viewModel.clearWorkspace() },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(
                                    text = "실험대 비우기 🧹",
                                    color = TertiaryPink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Button(
                            onClick = { showGuideDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LemonCream,
                                contentColor = SecondaryPeach
                            ),
                            border = BorderStroke(1.dp, SecondaryPeach.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .testTag("open_guide_button")
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "💡", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "합체 조합 비밀수첩",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorC
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showMatchingDogamDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PastelMint,
                                contentColor = DeepMint
                            ),
                            border = BorderStroke(1.dp, DeepMint.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .testTag("open_matching_dogam_button")
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📖", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "매칭 도감",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorC
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showHintDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LavenderMist,
                                contentColor = ColorCompound
                            ),
                            border = BorderStroke(1.dp, ColorCompound.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .testTag("open_hint_button")
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🔍", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "힌트 찬스",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- 3. TAB-BASED SELECTION / LIBRARY ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePaper),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Pill Bar
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = SecondaryBackground,
                    indicator = { tabPositions ->
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[activeTab])
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfacePaper)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, PastelMint, RoundedCornerShape(18.dp)),
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "원소들",
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 0) DeepMint else ColorC
                        )
                    }
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "도감",
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 1) DeepMint else ColorC
                        )
                    }
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "퀴즈 🎯",
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 2) DeepMint else ColorC
                        )
                    }
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "백업 ☁️",
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 3) DeepMint else ColorC
                        )
                    }
                    Tab(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "배지 🏆",
                            fontSize = 11.sp,
                            fontWeight = if (activeTab == 4) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == 4) DeepMint else ColorC
                        )
                    }
                }

                // Tab Contents
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    when (activeTab) {
                        0 -> {
                            // Element Friends Picker List
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.baseElements) { element ->
                                    ElementCardPicker(
                                        element = element,
                                        isAlreadyLoaded = state.workspaceLeft == element.id || state.workspaceRight == element.id,
                                        onSelect = { viewModel.selectElement(element.id) },
                                        onDragStart = { startPos ->
                                            activeDragState = DragState(
                                                elementId = element.id,
                                                initialPositionInRoot = startPos,
                                                currentPositionInRoot = startPos
                                            )
                                        },
                                        onDrag = { amount ->
                                            activeDragState = activeDragState?.copy(
                                                currentPositionInRoot = activeDragState!!.currentPositionInRoot + amount
                                            )
                                        },
                                        onDragEnd = {
                                            activeDragState?.let { drag ->
                                                val dropPos = drag.currentPositionInRoot
                                                val threshold = with(density) { 60.dp.toPx() }
                                                
                                                val leftCenter = slotLeftCenter
                                                val rightCenter = slotRightCenter
                                                
                                                if (leftCenter != null && (dropPos - leftCenter).getDistance() < threshold) {
                                                    viewModel.setLeftSlot(drag.elementId)
                                                } else if (rightCenter != null && (dropPos - rightCenter).getDistance() < threshold) {
                                                    viewModel.setRightSlot(drag.elementId)
                                                }
                                            }
                                            activeDragState = null
                                        },
                                        onDragCancel = {
                                            activeDragState = null
                                        }
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Illustrated Guide Book (도감)
                            CompoundBookView(
                                unlockedEntities = state.unlockedFormulas,
                                onFormulaClick = { formulaId ->
                                    val recipe = CompoundRecipe.ALL_RECIPES.firstOrNull { it.formulaId == formulaId }
                                    if (recipe != null) {
                                        // Load reactants to playground when clicked in guide! Super interactive.
                                        viewModel.clearWorkspace()
                                        viewModel.selectElement(recipe.ingredientA)
                                        viewModel.selectElement(recipe.ingredientB)
                                        activeTab = 0
                                        showToast("도감 가이드로 결합 원소들을 배치했어요! 😉")
                                    }
                                },
                                onElementClick = { elementId ->
                                    viewModel.clearWorkspace()
                                    viewModel.selectElement(elementId)
                                    activeTab = 0
                                    showToast("실험실에 ${elementId} 원소를 배치했어요! 🧪")
                                }
                             )
                        }

                        2 -> {
                            DailyQuizView(
                                state = state,
                                viewModel = viewModel
                            )
                        }

                        3 -> {
                            // Simulated Cloud Sync backup control panel
                            CloudSyncPanel(
                                state = state,
                                restoreCodeInput = restoreCodeInput,
                                onRestoreCodeChanged = { restoreCodeInput = it },
                                onPerformBackup = { viewModel.performCloudBackup() },
                                onPerformRestore = {
                                    if (restoreCodeInput.isNotBlank()) {
                                        viewModel.performCloudRestore(restoreCodeInput)
                                        restoreCodeInput = ""
                                    } else {
                                        showToast("복원할 텍스트 코드를 입력해주세요!")
                                    }
                                },
                                onClearLocalProgress = {
                                    viewModel.resetGame()
                                    showToast("실험실 데이터를 초기화했습니다!")
                                }
                            )
                        }

                        4 -> {
                            BadgeCollectionTab(
                                state = state,
                                onBadgeClick = { selectedBadgeDetail = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS SECTION (Level Up, New Compund Discovered) ---

    // 1. New Compound Blast Discovery Pop-up
    state.lastMergeResult?.let { success ->
        DiscoveryDialog(
            success = success,
            onDismiss = { viewModel.dismissMergeDialog() }
        )
    }

    // 2. Fused Error Notice
    state.errorMessage?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissErrorDialog() },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissErrorDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryPink)
                ) {
                    Text("다시 해볼래요!")
                }
            },
            title = {
                Text(
                    text = "원소들이 부딪혀 팅~ 💥",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TertiaryPink
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "😓",
                        fontSize = 44.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = errorMsg,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        color = ColorC
                    )
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurfacePaper
        )
    }

    // 3. Level Up Event Celebration
    state.showLevelUpDialog?.let { level ->
        LevelUpCelebDialog(
            level = level,
            title = ageTitle,
            onDismiss = { viewModel.dismissLevelUpDialog() }
        )
    }

    // 4. Cloud Backup Code copypast visual success
    if (state.syncSuccess) {
        Dialog(onDismissRequest = { viewModel.dismissSyncSuccess() }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🥳",
                        fontSize = 54.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "클라우드 세이브 완료!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DeepMint
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "연구실 정보가 가상의 클라우드 백업 공간에 저장되었습니다.",
                        fontSize = 13.sp,
                        color = SoftGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    state.lastSyncCodeGenerated?.let { code ->
                        Text(
                            text = "귀여운 실험실 고유 코드:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorC
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LemonCream, RoundedCornerShape(12.dp))
                                .border(1.dp, SecondaryPeach.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = code,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryPeach,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                viewModel.getExportToken { token ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ElementFriendsBackup", token)
                                    clipboard.setPrimaryClip(clip)
                                    showToast("복원용 텍스트 백업키가 클립보드에 복사되었어요! 타 기기에서 복원창에 붙여넣어 완벽 호환됩니다.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryPeach)
                        ) {
                            Text(text = "텍스트 백업 복제하기 📋", fontSize = 12.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { viewModel.dismissSyncSuccess() }) {
                        Text(text = "확인", color = DeepMint, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 5. Cloud Restore Visual Status feedback
    state.restoreStatus?.let { success ->
        Dialog(onDismissRequest = { viewModel.dismissRestoreStatus() }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (success) "✨" else "❌",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (success) "성공적으로 복원되었습니다!" else "복원에 실패했어요!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (success) DeepMint else TertiaryPink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (success) "이전 실험실의 레벨과 모든 도감이 안전하게 돌아왔어요! 야호!" else "올바르지 않은 백업 텍스트 형태 정보입니다. 코드를 다시 확인해주세요.",
                        fontSize = 12.sp,
                        color = SoftGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // 7. Badge Details Overlay Popup
    selectedBadgeDetail?.let { badge ->
        BadgeDetailDialog(
            badge = badge,
            onDismiss = { selectedBadgeDetail = null }
        )
    }

    // 8. Newly Unlocked Badge Celebration Dialog Alert
    state.justUnlockedBadge?.let { badge ->
        BadgeUnlockedDialog(
            badge = badge,
            onDismiss = { viewModel.dismissBadgeDialog() }
        )
    }

    // 6. Interactive Step-by-Step Tutorial
    InteractiveTutorialDialog(
        state = state,
        viewModel = viewModel
    )

    // Kid-Friendly Step-by-Step Combos Guide Manual
    if (showGuideDialog) {
        LabGuideDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showGuideDialog = false }
        )
    }

    // Matching Encyclopedia (매칭 도감) Modal Dialog
    if (showMatchingDogamDialog) {
        MatchingDogamDialog(
            state = state,
            onDismiss = { showMatchingDogamDialog = false }
        )
    }

    // Daily Hint Dialog
    if (showHintDialog) {
        DailyHintDialog(
            state = state,
            viewModel = viewModel,
            onDismiss = { showHintDialog = false }
        )
    }

    } // Closes Column

    // Render floating drag bubble on top of everything
    if (activeDragState != null) {
        val drag = activeDragState!!
        val element = ChemicalElement.BASE_MAP[drag.elementId]
        if (element != null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .offset {
                        IntOffset(
                            (drag.currentPositionInRoot.x - with(density) { 36.dp.toPx() }).toInt(),
                            (drag.currentPositionInRoot.y - with(density) { 36.dp.toPx() }).toInt()
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, element.color)
                        )
                    )
                    .border(2.dp, element.color, CircleShape)
                    .alpha(0.85f)
                    .scale(1.15f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = element.symbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorC
                    )
                    Text(
                        text = element.nameKo,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorC.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTS & HELPERS ---

@Composable
fun WorkspaceSlot(
    elementId: String?,
    testTag: String,
    label: String,
    onClear: () -> Unit,
    combineAnimationActive: Boolean,
    isLeft: Boolean,
    canCombine: Boolean = false,
    onCombine: (() -> Unit)? = null,
    onPositioned: ((Offset) -> Unit)? = null,
    onMove: (() -> Unit)? = null
) {
    val element = elementId?.let { ChemicalElement.BASE_MAP[it] }

    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    // Sliding collision animation distance
    val offsetX by animateDpAsState(
        targetValue = if (combineAnimationActive) {
            if (isLeft) 45.dp else (-45).dp
        } else 0.dp,
        animationSpec = tween(1200, easing = LinearOutSlowInEasing),
        label = "Colliding Offset"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.2f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "Drag Scale"
    )

    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = with(density) { 110.dp.toPx() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = offsetX)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SoftGray,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .testTag(testTag)
                .size(92.dp)
                .onGloballyPositioned { coordinates ->
                    if (onPositioned != null) {
                        val pos = coordinates.positionInRoot()
                        val coordsSize = coordinates.size
                        val center = pos + Offset(coordsSize.width / 2f, coordsSize.height / 2f)
                        onPositioned(center)
                    }
                }
                .clip(CircleShape)
                .background(
                    if (element != null) element.color.copy(alpha = 0.15f)
                    else Color.Transparent
                )
                .border(
                    width = 2.dp,
                    color = if (element != null) element.color else SoftGray.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (element != null) {
                // Render custom Element Bubble character
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .scale(scale)
                        .offset {
                            IntOffset(
                                dragOffset.value.x.toInt(),
                                dragOffset.value.y.toInt()
                            )
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.White, element.color)
                            )
                        )
                        .then(
                            if (!combineAnimationActive) {
                                Modifier.pointerInput(elementId) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            val currentX = dragOffset.value.x
                                            val currentY = dragOffset.value.y
                                            val isTriggerX = if (isLeft) {
                                                currentX > thresholdPx
                                            } else {
                                                currentX < -thresholdPx
                                            }
                                            val isTriggerYDown = currentY > thresholdPx

                                            if (isTriggerYDown) {
                                                coroutineScope.launch {
                                                    dragOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                }
                                                onClear()
                                            } else if (isTriggerX) {
                                                if (canCombine && onCombine != null) {
                                                    coroutineScope.launch {
                                                        dragOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                    }
                                                    onCombine()
                                                } else if (!canCombine && onMove != null) {
                                                    coroutineScope.launch {
                                                        dragOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                                    }
                                                    onMove()
                                                } else {
                                                    coroutineScope.launch {
                                                        dragOffset.animateTo(
                                                            Offset.Zero,
                                                            spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow)
                                                        )
                                                    }
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    dragOffset.animateTo(
                                                        Offset.Zero,
                                                        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMediumLow)
                                                    )
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            coroutineScope.launch {
                                                dragOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioHighBouncy))
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            coroutineScope.launch {
                                                dragOffset.snapTo(dragOffset.value + dragAmount)
                                            }
                                        }
                                    )
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = element.expression,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorC
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = element.symbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorC
                        )
                        Text(
                            text = element.nameKo,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorC.copy(alpha = 0.8f)
                        )
                    }

                    // Floating clear/X pill button on slot element
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(PastelPink)
                            .border(1.dp, TertiaryPink, CircleShape)
                            .clickable { onClear() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove element",
                            tint = TertiaryPink,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            } else {
                // Empty Dash State
                Canvas(modifier = Modifier.size(76.dp)) {
                    drawCircle(
                        color = SoftGray.copy(alpha = 0.3f),
                        style = Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Empty drop icon",
                    tint = SoftGray.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ElementCardPicker(
    element: ChemicalElement,
    isAlreadyLoaded: Boolean,
    onSelect: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .testTag("element_${element.id}")
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .onGloballyPositioned { coordinates ->
                positionInRoot = coordinates.positionInRoot()
            }
            .pointerInput(element.id, isAlreadyLoaded) {
                if (!isAlreadyLoaded) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            onDragStart(positionInRoot + startOffset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        },
                        onDragEnd = {
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragCancel()
                        }
                    )
                }
            }
            .clickable(enabled = !isAlreadyLoaded) { onSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlreadyLoaded) SoftGray.copy(alpha = 0.1f) else element.color.copy(alpha = 0.25f)
        ),
        border = BorderStroke(
            width = if (isAlreadyLoaded) 1.dp else 2.dp,
            color = if (isAlreadyLoaded) SoftGray.copy(alpha = 0.2f) else element.color
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (isAlreadyLoaded) {
                            Modifier.background(SoftGray.copy(alpha = 0.15f))
                        } else {
                            Modifier.background(Brush.radialGradient(colors = listOf(Color.White, element.color)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = element.symbol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAlreadyLoaded) SoftGray else ColorC
                )
            }

            Text(
                text = element.nameKo,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlreadyLoaded) SoftGray else ColorC
            )

            Text(
                text = element.expression,
                fontSize = 10.sp,
                color = if (isAlreadyLoaded) SoftGray else ColorC.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CompoundBookView(
    unlockedEntities: List<DiscoveredFormulaEntity>,
    onFormulaClick: (String) -> Unit,
    onElementClick: (String) -> Unit
) {
    val unlockedIds = remember(unlockedEntities) { unlockedEntities.map { it.formulaId }.toSet() }
    var selectedCategory by remember { mutableStateOf(ElementCategory.ALL) }

    // --- HashRouter State ---
    var currentHash by remember { mutableStateOf("#all") }
    val hashHistory = remember { mutableStateListOf("#all") }

    // Helper functions to navigate via the HashRouter
    val navigateTo = { hash: String ->
        if (currentHash != hash) {
            hashHistory.add(hash)
            currentHash = hash
        }
    }

    val navigateBack = {
        if (hashHistory.size > 1) {
            hashHistory.removeAt(hashHistory.lastIndex)
            currentHash = hashHistory.last()
        } else {
            currentHash = "#all"
        }
    }

    // Convert formula strings to beautiful chemical subscripts (e.g., CO2 -> CO₂)
    val formatSubscript = { text: String ->
        text.map { char ->
            when (char) {
                '0' -> '₀'
                '1' -> '₁'
                '2' -> '₂'
                '3' -> '₃'
                '4' -> '₄'
                '5' -> '₅'
                '6' -> '₆'
                '7' -> '₇'
                '8' -> '₈'
                '9' -> '₉'
                else -> char
            }
        }.joinToString("")
    }

    val filteredElements = remember(selectedCategory) {
        if (selectedCategory == ElementCategory.ALL) {
            ChemicalElement.ALL_BASE
        } else {
            ChemicalElement.ALL_BASE.filter { it.category == selectedCategory }
        }
    }

    val filteredRecipes = remember(selectedCategory, currentHash) {
        val baseRecipes = if (selectedCategory == ElementCategory.ALL) {
            CompoundRecipe.ALL_RECIPES
        } else {
            CompoundRecipe.ALL_RECIPES.filter { it.category == selectedCategory }
        }

        // Apply hash routing filters
        when {
            currentHash == "#unlocked" -> baseRecipes.filter { unlockedIds.contains(it.formulaId) }
            currentHash == "#locked" -> baseRecipes.filter { !unlockedIds.contains(it.formulaId) }
            else -> baseRecipes
        }
    }

    val unlockedInThisCategory = remember(unlockedIds, filteredRecipes) {
        filteredRecipes.count { unlockedIds.contains(it.formulaId) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🌐 Kid-Friendly Browser HashRouter Address Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryBackground.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, PastelMint.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back arrow
                    IconButton(
                        onClick = { navigateBack() },
                        enabled = hashHistory.size > 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back Hash Route",
                            tint = if (hashHistory.size > 1) DeepMint else SoftGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Home/Reset
                    IconButton(
                        onClick = {
                            hashHistory.clear()
                            hashHistory.add("#all")
                            currentHash = "#all"
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(text = "🏠", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Address Bar Field
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfacePaper)
                            .border(1.dp, SoftGray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔒", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kids.science/lab",
                            fontSize = 11.sp,
                            color = SoftGray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentHash,
                            fontSize = 11.sp,
                            color = DeepMint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fast Hashrouter Links Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fastRoutes = listOf(
                        "#all" to "🌐 모두 보기",
                        "#unlocked" to "🎉 발견한 물질",
                        "#locked" to "🔒 미발견 물질"
                    )
                    fastRoutes.forEach { (route, label) ->
                        val isRouteSelected = currentHash == route || 
                            (route == "#all" && !currentHash.startsWith("#unlocked") && !currentHash.startsWith("#locked") && !currentHash.startsWith("#formula/"))
                        
                        FilterChip(
                            selected = isRouteSelected,
                            onClick = { navigateTo(route) },
                            label = { Text(text = label, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LemonCream,
                                selectedLabelColor = ColorC,
                                containerColor = SurfacePaper
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isRouteSelected,
                                selectedBorderColor = SecondaryPeach,
                                borderColor = SoftGray.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- SUB-SCREEN 1: Detailed Compound Screen (Routed by Hash: #formula/{id}) ---
        if (currentHash.startsWith("#formula/")) {
            val formulaIdParam = currentHash.substringAfter("#formula/")
            val recipe = CompoundRecipe.ALL_RECIPES.firstOrNull { it.formulaId == formulaIdParam }
            
            if (recipe != null) {
                val elementA = ChemicalElement.BASE_MAP[recipe.ingredientA]
                val elementB = ChemicalElement.BASE_MAP[recipe.ingredientB]
                val isUnlocked = unlockedIds.contains(recipe.formulaId)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back link styled like a href web anchor
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navigateBack() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "👈 ", fontSize = 14.sp)
                        Text(
                            text = "도감 목록으로 돌아가기 (science.lab#catalog)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryPeach,
                            style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hero Spotlight Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                        border = BorderStroke(2.dp, if (isUnlocked) recipe.color.copy(alpha = 0.8f) else SoftGray.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Element Emoji Blob
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(if (isUnlocked) recipe.color.copy(alpha = 0.2f) else SoftGray.copy(alpha = 0.15f))
                                    .border(
                                        width = 2.dp,
                                        color = if (isUnlocked) recipe.color else SoftGray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isUnlocked) recipe.characterEmoji else "🔒",
                                    fontSize = 44.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Name and chemical subscripts
                            Text(
                                text = if (isUnlocked) recipe.nameKo else "??? 비밀의 화합물 ???",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isUnlocked) ColorC else SoftGray
                            )
                            
                            Text(
                                text = "화학식 기호: ${formatSubscript(recipe.formulaId)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) recipe.color else SoftGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Unlock status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isUnlocked) PastelMint else PastelPink)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isUnlocked) "🎉 성공적으로 연구 완료된 위대한 화합물입니다!" else "🔍 아직 합성법을 알아내지 못한 미지의 화합물입니다.",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) DeepMint else TertiaryPink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chemical Mechanism Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundCream),
                        border = BorderStroke(1.dp, SoftGray.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🧪 결합 공식 메커니즘",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorC
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Ingredient A Component
                                if (elementA != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onElementClick(elementA.id) }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(elementA.color.copy(alpha = 0.25f))
                                                .border(1.5.dp, elementA.color, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = elementA.expression, fontSize = 8.sp, color = ColorC)
                                                Text(text = elementA.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorC)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = elementA.nameKo,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorC
                                        )
                                    }
                                }

                                Text(
                                    text = "+",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SecondaryPeach,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                // Ingredient B Component
                                if (elementB != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onElementClick(elementB.id) }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(elementB.color.copy(alpha = 0.25f))
                                                .border(1.5.dp, elementB.color, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = elementB.expression, fontSize = 8.sp, color = ColorC)
                                                Text(text = elementB.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorC)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = elementB.nameKo,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorC
                                        )
                                    }
                                }

                                Text(
                                    text = "➔",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SecondaryPeach,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                // Target Result Component
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnlocked) recipe.color.copy(alpha = 0.25f) else SoftGray.copy(alpha = 0.15f))
                                            .border(1.5.dp, if (isUnlocked) recipe.color else SoftGray.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isUnlocked) recipe.characterEmoji else "🔒",
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isUnlocked) recipe.nameKo.substringBefore(" (") else "???",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorC
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cute explanation card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                        border = BorderStroke(1.dp, LemonCream)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "📖 꼬마 과학자 물질 백과사전",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryMint
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isUnlocked) recipe.description 
                                       else "이 화학물질은 비밀 공식에 싸여 있어요! 과학 연구소의 친구들, ${elementA?.nameKo ?: recipe.ingredientA} 원소와 ${elementB?.nameKo ?: recipe.ingredientB} 원소를 수조에 넣어 합체를 완성해 잠금을 풀어보세요!",
                                fontSize = 11.5.sp,
                                color = ColorC,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action: Go make it!
                    Button(
                        onClick = { onFormulaClick(recipe.formulaId) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🧪", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "결합 실험실로 가서 즉시 합성하기",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                currentHash = "#all"
            }
        } else {
            // --- SUB-SCREEN 2: Standard Lists Filtered by Category / Hash ---
            // Horizontal Scrollable Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElementCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    val backgroundColor = if (isSelected) SurfacePaper else Color.Transparent
                    val borderColor = if (isSelected) DeepMint else SoftGray.copy(alpha = 0.25f)
                    val textColor = if (isSelected) DeepMint else ColorC.copy(alpha = 0.7f)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(backgroundColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category.emoji, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = category.nameKo,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Elements (원소)
                if (filteredElements.isNotEmpty() && currentHash != "#locked" && currentHash != "#unlocked") {
                    item {
                        Text(
                            text = "🧪 주역 원소 친구들 (${filteredElements.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepMint,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                        )
                    }
                    
                    items(filteredElements) { element ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onElementClick(element.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                            border = BorderStroke(width = 1.dp, color = element.color.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(colors = listOf(Color.White, element.color))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = element.symbol,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ColorC
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${element.nameKo} (${element.nameEn})",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorC
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(element.color.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "번호: ${element.atomicNumber}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = element.color
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(3.dp))
                                    
                                    Text(
                                        text = element.description,
                                        fontSize = 11.sp,
                                        color = ColorC.copy(alpha = 0.8f),
                                        lineHeight = 15.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                
                                Text(
                                    text = element.expression,
                                    fontSize = 11.sp,
                                    color = ColorC.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Section 2: Chemical Formulas/Compounds
                if (filteredRecipes.isNotEmpty()) {
                    item {
                        val headerLabel = when (currentHash) {
                            "#unlocked" -> "🎉 연구 완료한 도감 화합물"
                            "#locked" -> "🔒 앞으로 합성해 찾아야 할 화합물"
                            else -> "🌌 결합 화학식 도감"
                        }
                        Text(
                            text = "$headerLabel ($unlockedInThisCategory/${filteredRecipes.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepMint,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)
                        )
                    }

                    items(filteredRecipes) { recipe ->
                        val isUnlocked = unlockedIds.contains(recipe.formulaId)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigateTo("#formula/${recipe.formulaId}") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUnlocked) SurfacePaper else SecondaryBackground.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isUnlocked) recipe.color.copy(alpha = 0.6f) else SoftGray.copy(alpha = 0.15f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 1.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isUnlocked) recipe.color.copy(alpha = 0.25f)
                                            else SoftGray.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isUnlocked) recipe.characterEmoji else "🔒",
                                        fontSize = 20.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isUnlocked) recipe.nameKo else "???",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnlocked) ColorC else SoftGray
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isUnlocked) recipe.color.copy(alpha = 0.15f) else SoftGray.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = formatSubscript(recipe.formulaId),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUnlocked) recipe.color else SoftGray
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(3.dp))
                                    
                                    Text(
                                        text = if (isUnlocked) recipe.description else "합성을 통해 보너스 조합을 잠금 해제해보세요! 힌트: ${recipe.ingredientA} + ${recipe.ingredientB}",
                                        fontSize = 11.sp,
                                        color = if (isUnlocked) ColorC.copy(alpha = 0.8f) else SoftGray.copy(alpha = 0.6f),
                                        lineHeight = 15.sp
                                    )
                                }
                                
                                if (isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "Science icon",
                                        tint = recipe.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSyncPanel(
    state: GameUiState,
    restoreCodeInput: String,
    onRestoreCodeChanged: (String) -> Unit,
    onPerformBackup: () -> Unit,
    onPerformRestore: () -> Unit,
    onClearLocalProgress: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "실시간 도감 및 아카이브 상태 ☁️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepMint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "아기들은 연구 상태를 잃지 않고 다른 폰에서도 이어 연구할 수 있게 예쁜 오리지널 고유 백업 방울키를 생성할 수 있어요.",
                        fontSize = 11.sp,
                        color = ColorC.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { onPerformBackup() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "클라우드 세이브 백업하기 ✨", fontSize = 12.sp)
                    }
                }
            }
        }

        // Restore Progress Code area
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "이전 연구실 복구하기 (백업 복원) 🧬",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ColorC,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                OutlinedTextField(
                    value = restoreCodeInput,
                    onValueChange = onRestoreCodeChanged,
                    placeholder = { Text("복원할 연구실 텍스트 백업키를 붙여넣어주세요.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryMint,
                        unfocusedBorderColor = SoftGray.copy(alpha = 0.5f)
                    ),
                    textStyle = TextStyle(fontSize = 12.sp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onPerformRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryPeach),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "연구실 로드 & 복원 🪄", fontSize = 12.sp)
                }
            }
        }

        // Reset game data warning area
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelPink.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, TertiaryPink.copy(alpha = 0.20f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "실험실 초기화 (Fresh Start)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TertiaryPink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "도감 발견 내용과 수집한 경험치 레벨을 모두 지우고 1급 연구 조수로 완전히 새로 시작합니다.",
                        fontSize = 11.sp,
                        color = ColorC.copy(alpha = 0.70f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedButton(
                        onClick = onClearLocalProgress,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TertiaryPink),
                        border = BorderStroke(1.dp, TertiaryPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "처음부터 다시 연구하기 🧹", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryDialog(
    success: MergeResult.Success,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val transition = rememberInfiniteTransition(label = "")
        val starRotation by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotate"
        )

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePaper),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sparks and glow behind
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Sparkling Canvas Starburst
                    Canvas(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(1.2f)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val numRays = 8
                        val angleStep = 360.0 / numRays
                        val radianRot = Math.toRadians(starRotation.toDouble())
                        
                        for (i in 0 until numRays) {
                            val angleRad = Math.toRadians(i * angleStep) + radianRot
                            val end = Offset(
                                x = center.x + cos(angleRad).toFloat() * 70.dp.toPx(),
                                y = center.y + sin(angleRad).toFloat() * 70.dp.toPx()
                            )
                            drawLine(
                                color = SecondaryPeach.copy(alpha = 0.4f),
                                start = center,
                                end = end,
                                strokeWidth = 8f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                            )
                        }
                    }

                    // Compound character bubble bouncing
                    val bouncingScale by transition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = EaseInOutBack),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bounce"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(bouncingScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.White, success.recipe.color)
                                )
                            )
                            .border(3.dp, success.recipe.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = success.recipe.characterEmoji,
                            fontSize = 54.sp
                        )
                    }
                }

                Text(
                    text = if (success.newlyDiscovered) "✨ 새로운 친구 발견! ✨" else "알고 있는 결합체 발견!",
                    fontSize = 13.sp,
                    color = SecondaryPeach,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = success.recipe.nameKo,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = ColorC,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(success.recipe.color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "보관 기호: ${success.recipe.formulaId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = success.recipe.color
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = success.recipe.description,
                    fontSize = 13.sp,
                    color = ColorC.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (success.newlyDiscovered) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LemonCream)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "XP star",
                            tint = SecondaryPeach,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${success.xpGained} XP 보너스 획득!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryPeach
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(text = "도감에 저장하고 신나게 연구 계속하기!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LevelUpCelebDialog(
    level: Int,
    title: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfacePaper),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆",
                    fontSize = 68.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "경축! 실험 장비 증설 & 등급 업!",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = SecondaryPeach,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "레벨 ${level}급 연구과학자 달성!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ColorC
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "이제 아기 과학자는 [${title}] 입니다!",
                    fontSize = 12.sp,
                    color = DeepMint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "놀라운 집념으로 새로운 화학 기호들을 합쳐냈군요! 더 넓어진 우주와 신기한 호기심들이 아기를 환영하고 기다려요.",
                    fontSize = 11.sp,
                    color = SoftGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryPeach),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "야호! 고마워요 🥳", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChemistryVesselBackground(
    animationPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleProgress = remember { Animatable(0f) }
    
    LaunchedEffect(animationPlaying) {
        if (animationPlaying) {
            bubbleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = EaseOutQuad)
            )
            bubbleProgress.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        // Subtle bubbles flowing inside experimental chamber
        val width = size.width
        val height = size.height
        
        if (animationPlaying) {
            // Draw colorful rising particles when bonding elements
            val random = Random(42)
            for (i in 0..12) {
                val startX = width / 2 + (random.nextFloat() - 0.5f) * 160f
                val startY = height / 2 + 100f
                val currentY = startY - (bubbleProgress.value * 300f)
                val radius = 10f + (random.nextFloat() * 15f)
                
                drawCircle(
                    color = listOf(ColorH, ColorO, ColorHe, ColorNa).random(random).copy(alpha = 0.5f * (1f - bubbleProgress.value)),
                    radius = radius,
                    center = Offset(startX, currentY)
                )
            }
        }
    }
}

enum class LabParticleShape {
    Circle, Sparkle, Ring, Shard
}

class LabParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val maxRadius: Float,
    var radius: Float,
    var alpha: Float,
    val lifeSpan: Float,
    var age: Float = 0f,
    val shape: LabParticleShape = LabParticleShape.Circle,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f,
    val strokeWidth: Float = 0f
)

@Composable
fun PastelParticleEffectOverlay(
    comboCount: Int,
    modifier: Modifier = Modifier
) {
    // Keep a list of active particles
    val particles = remember { mutableStateListOf<LabParticle>() }
    var nextId by remember { mutableStateOf(0) }
    
    // Trigger burst on comboCount change
    LaunchedEffect(comboCount) {
        if (comboCount > 0) {
            val colors = listOf(
                Color(0xFFFF7F9F), // Vibrant Neon Pastel Peach/Pink
                Color(0xFFFFB3BA), // Soft Pastel Red
                Color(0xFFFFDFBA), // Soft Pastel Orange
                Color(0xFFFFF275), // Vibrant Sunny Yellow
                Color(0xFFBAFFC9), // Soft Pastel Green
                Color(0xFFBAE1FF), // Soft Pastel Blue
                Color(0xFFE8C4FD), // Soft Pastel Lilac
                Color(0xFF4D96FF), // Neon Sky Blue
                Color(0xFF6BCB77), // Neon Green
                Color(0xFFFF6B6B)  // Sweet Coral
            )
            
            // 1. Generate Shockwave Rings (The spectacular spatial expansion)
            repeat(2) { index ->
                particles.add(
                    LabParticle(
                        id = nextId++,
                        x = 0f,
                        y = 0f,
                        vx = 0f,
                        vy = 0f,
                        color = colors.random(),
                        maxRadius = 180f + (index * 60f),
                        radius = 10f,
                        alpha = 1.0f,
                        lifeSpan = 500f + (index * 150f),
                        shape = LabParticleShape.Ring,
                        strokeWidth = 6.dp.value
                    )
                )
            }

            // 2. Generate standard combinations of beautiful particles (Sparkles, Shards, Bubbles)
            val baseCount = 50 + (comboCount * 12).coerceAtMost(50)
            repeat(baseCount) {
                val angle = (0..359).random() * Math.PI / 180.0
                // Variable speed to give organic layered depth
                val speed = (20..220).random() / 10f
                val radius = (50..160).random() / 10f
                val lifespan = (600..1500).random().toFloat()
                
                // Randomly distribute shapes
                val shapeDetermine = (0..100).random()
                val shape = when {
                    shapeDetermine < 35 -> LabParticleShape.Sparkle  // 35% magic sparkles
                    shapeDetermine < 65 -> LabParticleShape.Shard    // 30% chemistry crystals info shards
                    else -> LabParticleShape.Circle                  // 35% classic glowing micro-bubbles
                }

                particles.add(
                    LabParticle(
                        id = nextId++,
                        x = 0f,
                        y = 0f,
                        vx = (cos(angle) * speed).toFloat(),
                        vy = (sin(angle) * speed).toFloat() - (10..35).random() / 10f, // upward lift bias
                        color = colors.random(),
                        maxRadius = radius,
                        radius = radius,
                        alpha = 1f,
                        lifeSpan = lifespan,
                        age = 0f,
                        shape = shape,
                        rotation = (0..359).random().toFloat(),
                        rotationSpeed = (-8..8).random().toFloat()
                    )
                )
            }
        }
    }

    // Ticker to animate particles frame-by-frame
    if (particles.isNotEmpty()) {
        LaunchedEffect(particles.size) {
            var lastTimeNanos = 0L
            while (particles.isNotEmpty()) {
                withFrameNanos { frameNanos ->
                    if (lastTimeNanos == 0L) {
                        lastTimeNanos = frameNanos
                    }
                    val dtNanos = frameNanos - lastTimeNanos
                    lastTimeNanos = frameNanos
                    val dtMs = dtNanos / 1_000_000f
                    // Cap dtMs to avoid issues with hardware frame hiccups
                    val cappedDt = dtMs.coerceIn(4f, 32f)

                    val iterator = particles.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.age += cappedDt
                        if (p.age >= p.lifeSpan) {
                            iterator.remove()
                        } else {
                            val progress = p.age / p.lifeSpan
                            val scaleF = cappedDt / 16f
                            
                            p.x += p.vx * scaleF
                            p.y += p.vy * scaleF
                            p.rotation += p.rotationSpeed * scaleF
                            
                            if (p.shape == LabParticleShape.Ring) {
                                // Rings expand linearly and fade out
                                p.radius = p.maxRadius * progress
                                p.alpha = 1f - progress
                            } else {
                                // Standard physics: Gravity + drag friction
                                p.vy += 0.08f * scaleF // Gravity
                                p.vx *= (1f - 0.025f * scaleF) // Air drag friction
                                p.vy *= (1f - 0.025f * scaleF)
                                
                                p.alpha = 1f - progress
                                // Particles fade-out in size slightly towards the end
                                p.radius = p.maxRadius * (1f - progress * 0.35f)
                            }
                        }
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        
        particles.forEach { p ->
            when (p.shape) {
                LabParticleShape.Ring -> {
                    // Expanding shockwave circle outline with fading stroke
                    drawCircle(
                        color = p.color,
                        radius = p.radius,
                        center = Offset(centerX + p.x, centerY + p.y),
                        alpha = p.alpha,
                        style = Stroke(
                            width = (p.strokeWidth * (1f - (p.age / p.lifeSpan))).coerceAtLeast(1f),
                            cap = StrokeCap.Round
                        )
                    )
                }
                LabParticleShape.Circle -> {
                    // Classic gas bubbles with an outer soft atmospheric aura glow
                    drawCircle(
                        color = p.color,
                        radius = p.radius * 2.0f,
                        center = Offset(centerX + p.x, centerY + p.y),
                        alpha = p.alpha * 0.22f
                    )
                    drawCircle(
                        color = p.color,
                        radius = p.radius,
                        center = Offset(centerX + p.x, centerY + p.y),
                        alpha = p.alpha
                    )
                }
                LabParticleShape.Sparkle -> {
                    // Beautiful 4-point magical math sparkle
                    translate(left = centerX + p.x, top = centerY + p.y) {
                        rotate(degrees = p.rotation, pivot = Offset.Zero) {
                            val sparklePath = Path().apply {
                                moveTo(0f, -p.radius)
                                quadraticTo(0f, 0f, p.radius, 0f)
                                quadraticTo(0f, 0f, 0f, p.radius)
                                quadraticTo(0f, 0f, -p.radius, 0f)
                                quadraticTo(0f, 0f, 0f, -p.radius)
                                close()
                            }
                            // Outer glowing flare
                            drawPath(
                                path = sparklePath,
                                color = p.color,
                                alpha = p.alpha * 0.3f,
                                style = Stroke(width = p.radius * 0.6f, cap = StrokeCap.Round)
                            )
                            // Inner solid flare
                            drawPath(
                                path = sparklePath,
                                color = Color.White.copy(alpha = p.alpha * 0.3f) // Blend white center for extra glow shine!
                            )
                            drawPath(
                                path = sparklePath,
                                color = p.color,
                                alpha = p.alpha
                            )
                        }
                    }
                }
                LabParticleShape.Shard -> {
                    // Spinning chemistry crystal shard (triangle)
                    translate(left = centerX + p.x, top = centerY + p.y) {
                        rotate(degrees = p.rotation, pivot = Offset.Zero) {
                            val shardPath = Path().apply {
                                moveTo(0f, -p.radius)
                                lineTo(p.radius * 0.86f, p.radius * 0.5f)
                                lineTo(-p.radius * 0.86f, p.radius * 0.5f)
                                close()
                            }
                            drawPath(
                                path = shardPath,
                                color = p.color,
                                alpha = p.alpha
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComboBadge(
    comboCount: Int,
    modifier: Modifier = Modifier
) {
    // Add visual bounce animation when combo count increments
    val scale = remember { Animatable(1f) }
    LaunchedEffect(comboCount) {
        scale.animateTo(1.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
        scale.animateTo(1.0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFF0F5), // LavenderBlush
                        Color(0xFFE0FFFF)  // LightCyan
                    )
                )
            )
            .border(
                1.5.dp, 
                Brush.sweepGradient(listOf(SecondaryPeach, PrimaryMint, ColorO)), 
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = "Combo star",
                tint = SecondaryPeach,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${comboCount} 콤보! 🔥",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorC
            )
        }
    }
}

@Composable
fun DailyQuizView(
    state: GameUiState,
    viewModel: GameViewModel
) {
    val quiz = state.dailyQuiz
    val progress = state.progress
    val isSolvedToday = progress.lastQuizSolvedDate == quiz.dateString || state.quizSuccess == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                border = BorderStroke(1.dp, DeepMint.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PastelMint.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏅", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "오늘의 일일 퀴즈", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorC)
                            Text(text = "매일 새로운 결합 공식 풀기", fontSize = 11.sp, color = ColorC.copy(alpha = 0.6f))
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SecondaryPeach.copy(alpha = 0.15f))
                            .border(1.dp, SecondaryPeach.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔥 ${progress.quizStreak} 일 연속", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ColorO)
                    }
                }
            }
        }

        // Quiz Question Main Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, if (isSolvedToday) PrimaryMint else DeepMint.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSolvedToday) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(PrimaryMint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = quiz.targetRecipe.characterEmoji, fontSize = 38.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "오늘의 퀴즈 완료! 🎉",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryMint
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "성공적으로 ${quiz.targetRecipe.nameKo} (${quiz.targetRecipe.formulaId}) 조합을 풀었습니다.",
                            fontSize = 12.sp,
                            color = ColorC,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepMint.copy(alpha = 0.1f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "퀴즈 풀기 보상: +100 XP ⚡",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepMint
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${quiz.targetRecipe.description}",
                            fontSize = 12.sp,
                            color = ColorC.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    } else {
                        Text(
                            text = "오늘의 화학식 합성 퀴즈 🎯",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepMint
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "어떤 두 원소를 결합해야 할까요?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorC
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SecondaryBackground)
                                .border(1.dp, SoftGray.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "❓", fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = quiz.targetRecipe.nameKo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorC
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "힌트: ${quiz.targetRecipe.description}",
                            fontSize = 11.sp,
                            color = ColorC.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "선택된 원소 (${state.quizSelectedElementIds.size}/2):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorC.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(52.dp)
                        ) {
                            repeat(2) { index ->
                                val selectedId = state.quizSelectedElementIds.getOrNull(index)
                                val element = selectedId?.let { id -> ChemicalElement.ALL_BASE.firstOrNull { it.id == id } }
                                if (element != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(element.color.copy(alpha = 0.2f))
                                            .border(1.5.dp, element.color, CircleShape)
                                            .clickable { viewModel.toggleQuizElement(element.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = element.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(SecondaryBackground)
                                            .border(1.dp, SoftGray.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "+", fontSize = 16.sp, color = SoftGray)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.submitQuiz() },
                            enabled = state.quizSelectedElementIds.size == 2,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepMint,
                                disabledContainerColor = SoftGray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = "화학 결합 융합하기! 🧪",
                                fontWeight = FontWeight.Bold,
                                color = if (state.quizSelectedElementIds.size == 2) Color.White else ColorC.copy(alpha = 0.4f)
                            )
                        }

                        if (state.quizSuccess == false) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "앗! 다른 물질이 합성되었거나 비율이 잘못되었습니다. 다시 도전해 보세요! 💥",
                                color = TertiaryPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (!isSolvedToday) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "원소를 골라보세요 👇",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepMint,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quiz.options.forEach { element ->
                            val isSelected = state.quizSelectedElementIds.contains(element.id)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) element.color.copy(alpha = 0.25f) else SurfacePaper)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) element.color else SoftGray.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.toggleQuizElement(element.id) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = element.symbol, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ColorC)
                                    Text(text = element.nameKo, fontSize = 10.sp, color = ColorC.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveTutorialDialog(
    state: GameUiState,
    viewModel: GameViewModel
) {
    if (!state.isTutorialActive) return

    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) }
    
    // Snapping states
    var hSnapped by remember { mutableStateOf(false) }
    var oSnapped by remember { mutableStateOf(false) }

    // Offset animatables
    val hOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val oOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // Combined/success phase state
    var combinationDone by remember { mutableStateOf(false) }
    var isCombining by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { viewModel.stopTutorial() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { /* Dismiss clicks on backdrop */ },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(340.dp)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundCream),
                border = BorderStroke(3.dp, PrimaryMint)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎓", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "꼬마 연구원 가이드",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepMint
                            )
                        }
                        
                        // Skip Button
                        TextButton(
                            onClick = { viewModel.stopTutorial() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("건너뛰기 ✖", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TertiaryPink)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mascot Talk Bubble
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(LemonCream)
                            .border(1.dp, SecondaryPeach.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🧪",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Column {
                            Text(
                                text = "실험실 요정 퐁퐁이",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SecondaryPeach
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (currentStep) {
                                    1 -> "안녕! 수소(H) 원소를 꾸욱 터치해서 위의 수조 ①번으로 드래그 해 봐! 💧"
                                    2 -> "정말 잘했어! 🥳 이제 산소(O) 원소도 똑같이 드래그해서 수조 ②번 구멍에 골인시켜 줘! 🌬️"
                                    3 -> "최고야! 합치기 공식 완성! 가운데의 찬란한 [화학결합! ⚡] 버튼을 탭해서 합쳐보자!"
                                    else -> "축하해! 훌륭한 꼬마 과학자의 탄생이야! 더 많은 원소를 조합해 우주를 채워 보자!"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorC,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!combinationDone) {
                        // Interactive Play board
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfacePaper)
                                .border(1.dp, SoftGray.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        ) {
                            // Two Vessels/Slots on top
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Target Slot 1 (Left)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(if (hSnapped) ColorH.copy(alpha = 0.25f) else SecondaryBackground)
                                            .border(
                                                width = 2.dp,
                                                color = if (hSnapped) ColorH else SoftGray.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hSnapped) {
                                            Text(text = "H", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ColorC)
                                        } else {
                                            Text(text = "①", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoftGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("수조 ①", fontSize = 11.sp, color = SoftGray, fontWeight = FontWeight.Bold)
                                }

                                // Pulsing combining indicator in between
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .padding(top = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "plus icon",
                                        tint = if (hSnapped && oSnapped) SecondaryPeach else SoftGray.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Target Slot 2 (Right)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(if (oSnapped) ColorO.copy(alpha = 0.25f) else SecondaryBackground)
                                            .border(
                                                width = 2.dp,
                                                color = if (oSnapped) ColorO else SoftGray.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (oSnapped) {
                                            Text(text = "O", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ColorC)
                                        } else {
                                            Text(text = "②", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SoftGray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("수조 ②", fontSize = 11.sp, color = SoftGray, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Guide arrow indicators for children
                            val infiniteTransition = rememberInfiniteTransition(label = "indicator")
                            val guideOffset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = -95f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1400, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "guide_offset"
                            )

                            if (currentStep == 1 && !hSnapped) {
                                // Guide arrow for H straight up
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 58.dp, bottom = 95.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.offset(y = guideOffset.dp)
                                    ) {
                                        Text("👆", fontSize = 20.sp)
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowUp,
                                            contentDescription = "Arrow",
                                            tint = SecondaryPeach,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (currentStep == 2 && !oSnapped) {
                                // Guide arrow for O straight up
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 58.dp, bottom = 95.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.offset(y = guideOffset.dp)
                                    ) {
                                        Text("👆", fontSize = 20.sp)
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowUp,
                                            contentDescription = "Arrow",
                                            tint = SecondaryPeach,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Draggable items at bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 24.dp)
                            ) {
                                // Draggable H on Left
                                val isHEnabled = currentStep == 1 && !hSnapped
                                Box(
                                    modifier = Modifier
                                        .padding(start = 38.dp)
                                        .align(Alignment.BottomStart)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .offset { IntOffset(hOffset.value.x.toInt(), hOffset.value.y.toInt()) }
                                            .clip(CircleShape)
                                            .background(
                                                if (hSnapped) SoftGray.copy(alpha = 0.2f)
                                                else if (isHEnabled) ColorH.copy(alpha = 0.4f)
                                                else ColorH.copy(alpha = 0.1f)
                                            )
                                            .border(
                                                width = if (isHEnabled) 2.5.dp else 1.dp,
                                                color = if (isHEnabled) ColorH else SoftGray.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                            .then(
                                                if (isHEnabled) {
                                                    Modifier.pointerInput(Unit) {
                                                        detectDragGestures(
                                                            onDragStart = { },
                                                            onDragEnd = {
                                                                val targetDeltaY = -120.dp.toPx()
                                                                val dist = Math.hypot(hOffset.value.x.toDouble(), (hOffset.value.y - targetDeltaY).toDouble())
                                                                if (dist < 60.dp.toPx()) {
                                                                    hSnapped = true
                                                                    coroutineScope.launch {
                                                                        SoundSynth.playCombineSound(success = true)
                                                                        hOffset.animateTo(Offset(0f, targetDeltaY), spring(Spring.DampingRatioMediumBouncy))
                                                                        currentStep = 2
                                                                    }
                                                                } else {
                                                                    coroutineScope.launch {
                                                                        hOffset.animateTo(Offset.Zero, spring(Spring.DampingRatioHighBouncy))
                                                                    }
                                                                }
                                                            },
                                                            onDragCancel = {
                                                                coroutineScope.launch { hOffset.animateTo(Offset.Zero) }
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                coroutineScope.launch {
                                                                    hOffset.snapTo(hOffset.value + dragAmount)
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "H",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isHEnabled) ColorC else SoftGray
                                            )
                                            Text(
                                                text = "수소",
                                                fontSize = 9.sp,
                                                color = if (isHEnabled) ColorC.copy(alpha = 0.6f) else SoftGray
                                            )
                                        }
                                    }
                                }

                                // Draggable O on Right
                                val isOEnabled = currentStep == 2 && !oSnapped
                                Box(
                                    modifier = Modifier
                                        .padding(end = 38.dp)
                                        .align(Alignment.BottomEnd)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .offset { IntOffset(oOffset.value.x.toInt(), oOffset.value.y.toInt()) }
                                            .clip(CircleShape)
                                            .background(
                                                if (oSnapped) SoftGray.copy(alpha = 0.2f)
                                                else if (isOEnabled) ColorO.copy(alpha = 0.4f)
                                                else ColorO.copy(alpha = 0.1f)
                                            )
                                            .border(
                                                width = if (isOEnabled) 2.5.dp else 1.dp,
                                                color = if (isOEnabled) ColorO else SoftGray.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                            .then(
                                                if (isOEnabled) {
                                                    Modifier.pointerInput(Unit) {
                                                        detectDragGestures(
                                                            onDragStart = { },
                                                            onDragEnd = {
                                                                val targetDeltaY = -120.dp.toPx()
                                                                val dist = Math.hypot(oOffset.value.x.toDouble(), (oOffset.value.y - targetDeltaY).toDouble())
                                                                if (dist < 60.dp.toPx()) {
                                                                    oSnapped = true
                                                                    coroutineScope.launch {
                                                                        SoundSynth.playCombineSound(success = true)
                                                                        oOffset.animateTo(Offset(0f, targetDeltaY), spring(Spring.DampingRatioMediumBouncy))
                                                                        currentStep = 3
                                                                    }
                                                                } else {
                                                                    coroutineScope.launch {
                                                                        oOffset.animateTo(Offset.Zero, spring(Spring.DampingRatioHighBouncy))
                                                                    }
                                                                }
                                                            },
                                                            onDragCancel = {
                                                                coroutineScope.launch { oOffset.animateTo(Offset.Zero) }
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                coroutineScope.launch {
                                                                    oOffset.snapTo(oOffset.value + dragAmount)
                                                                }
                                                            }
                                                        )
                                                    }
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "O",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isOEnabled) ColorC else SoftGray
                                            )
                                            Text(
                                                text = "산소",
                                                fontSize = 9.sp,
                                                color = if (isOEnabled) ColorC.copy(alpha = 0.6f) else SoftGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Combination Action Button for Step 3
                        val isCombineButtonEnabled = currentStep == 3 && !isCombining
                        val combinePulse by rememberInfiniteTransition(label = "").animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.06f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "combine_pulse"
                        )

                        Button(
                            onClick = {
                                isCombining = true
                                coroutineScope.launch {
                                    SoundSynth.playCombineSound(success = true)
                                    kotlinx.coroutines.delay(1000)
                                    combinationDone = true
                                    currentStep = 4
                                }
                            },
                            enabled = isCombineButtonEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryPeach,
                                disabledContainerColor = SoftGray.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(if (isCombineButtonEnabled) combinePulse else 1f)
                        ) {
                            Text(
                                text = "화학결합! ⚡",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCombineButtonEnabled) Color.White else SoftGray
                            )
                        }
                    } else {
                        // Success Compound Discovered Phase
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(PastelMint),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💧", fontSize = 54.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "물 (H₂O)",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepMint
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "성공을 축하해! 꼬마 과학자로서의 첫 합성이야!",
                                fontSize = 12.sp,
                                color = ColorC,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SecondaryPeach.copy(alpha = 0.15f))
                                    .border(1.5.dp, SecondaryPeach, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "가이드 특별 보상: +50 XP",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SecondaryPeach
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { viewModel.completeTutorial() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "멋진 본게임 시작하기! 🚀",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeCollectionTab(
    state: GameUiState,
    onBadgeClick: (ElementBadge) -> Unit
) {
    val badges = state.badges
    val unlockedCount = badges.count { it.isUnlocked }
    val totalCount = badges.size
    val progressPercent = if (totalCount > 0) (unlockedCount * 100) / totalCount else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("badge_collection_list"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SecondaryBackground),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆 원소 마스터 배지 컬렉션",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepMint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "신비로운 화학 세상을 모험하고 멋진 꼬마 박사 배지를 모아보세요!",
                        fontSize = 12.sp,
                        color = SoftGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "획득한 배지: $unlockedCount / $totalCount 개",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorC
                        )
                        Text(
                            text = "달성도 $progressPercent%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryMint
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (unlockedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = PrimaryMint,
                        trackColor = Color.White
                    )
                }
            }
        }

        // List of badges as a Grid with beautiful responsive columns
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                badges.chunked(2).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunk.forEach { badge ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(132.dp)
                            ) {
                                BadgeCompactCard(
                                    badge = badge,
                                    onClick = { onBadgeClick(badge) }
                                )
                            }
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeCompactCard(
    badge: ElementBadge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (badge.isUnlocked) {
        when (badge.id) {
            "baby_alchemist" -> SkyBluePastel
            "junior_scientist" -> LavenderMist
            "combo_sprout" -> PastelMint
            "combo_master" -> SoftPeach
            "lab_explorer" -> LemonCream
            "element_doctor" -> PastelPink
            "quiz_king" -> SecondaryBackground
            else -> SkyBluePastel
        }
    } else {
        Color.White
    }

    val outlineColor = if (badge.isUnlocked) {
        PrimaryMint.copy(alpha = 0.5f)
    } else {
        SoftGray.copy(alpha = 0.2f)
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .testTag("badge_card_${badge.id}"),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, outlineColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.isUnlocked) 1.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Emoji / Status Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp)
            ) {
                if (badge.isUnlocked) {
                    Text(text = badge.emoji, fontSize = 28.sp)
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge.emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.graphicsLayer(alpha = 0.25f, renderEffect = null)
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "잠겨있음",
                            tint = SoftGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Text info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = badge.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) ColorC else SoftGray,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (badge.isUnlocked) "획득 완료! 🎉" else badge.requirement,
                    fontSize = 9.sp,
                    color = if (badge.isUnlocked) PrimaryMint else SoftGray.copy(alpha = 0.8f),
                    maxLines = 2,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Small bottom progress indicator
            if (!badge.isUnlocked && badge.maxProgress > 1) {
                LinearProgressIndicator(
                    progress = { (badge.currentProgress.toFloat() / badge.maxProgress.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SoftGray,
                    trackColor = SoftGray.copy(alpha = 0.2f)
                )
            } else if (badge.isUnlocked) {
                Text(
                    text = "⭐ 완료 ⭐",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SecondaryPeach
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun BadgeUnlockedDialog(
    badge: ElementBadge,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, SecondaryPeach, RoundedCornerShape(24.dp))
                .testTag("badge_unlocked_dialog"),
            colors = CardDefaults.cardColors(containerColor = BackgroundCream)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 배지 획득 성공! 🎉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SecondaryPeach,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Big floating badge icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(PrimaryMint.copy(alpha = 0.15f))
                        .border(2.dp, PrimaryMint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.emoji,
                        fontSize = 54.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = badge.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorC,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = badge.description,
                    fontSize = 13.sp,
                    color = SoftGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "우와! 정말 멋져요! 😊",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeDetailDialog(
    badge: ElementBadge,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SoftGray.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .testTag("badge_detail_dialog"),
            colors = CardDefaults.cardColors(containerColor = SurfacePaper)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (badge.isUnlocked) "🎉 획득한 배지 정보" else "🔒 잠겨있는 배지 정보",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) PrimaryMint else SoftGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = badge.emoji,
                    fontSize = 48.sp,
                    modifier = Modifier.graphicsLayer(alpha = if (badge.isUnlocked) 1f else 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = badge.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorC
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = badge.description,
                    fontSize = 12.sp,
                    color = SoftGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(color = SoftGray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "도전 조건: ${badge.requirement}",
                    fontSize = 11.sp,
                    color = ColorC.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryMint)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LabGuideDialog(
    state: GameUiState,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val recipes = CompoundRecipe.ALL_RECIPES
    var currentPage by remember { mutableStateOf(0) }
    
    val recipe = recipes[currentPage]
    val elementA = ChemicalElement.BASE_MAP[recipe.ingredientA]
    val elementB = ChemicalElement.BASE_MAP[recipe.ingredientB]
    val isDiscovered = state.unlockedFormulas.any { it.formulaId == recipe.formulaId }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCream),
            border = BorderStroke(2.dp, PrimaryMint.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📖", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "꼬마 과학자의 합체 비법서",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryMint
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close guidebook",
                            tint = SoftGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Magic Recipe Book Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                    border = BorderStroke(1.dp, PastelMint)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Magic Result Bubble Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = recipe.characterEmoji,
                                fontSize = 52.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Code Symbol Name
                        Text(
                            text = recipe.nameKo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorC,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "화학식: ${recipe.formulaId}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftGray,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Unlock Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDiscovered) PastelMint else PastelPink
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isDiscovered) "🎉 이미 도감에 발견된 완성한 물질이에요!" else "✨ 아직 발견하지 못한 비밀 물질이에요!",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDiscovered) DeepMint else TertiaryPink,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Ingredients display: Element A + Element B
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundCream, shape = RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp)
                        ) {
                            // Element A Bubble
                            if (elementA != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(elementA.color.copy(alpha = 0.25f))
                                            .border(1.5.dp, elementA.color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = elementA.expression,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorC
                                            )
                                            Text(
                                                text = elementA.symbol,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ColorC
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${elementA.nameKo} (${elementA.id})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorC
                                    )
                                }
                            }

                            Text(
                                text = "+",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = SecondaryPeach,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Element B Bubble
                            if (elementB != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(elementB.color.copy(alpha = 0.25f))
                                            .border(1.5.dp, elementB.color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = elementB.expression,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ColorC
                                            )
                                            Text(
                                                text = elementB.symbol,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ColorC
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${elementB.nameKo} (${elementB.id})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorC
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Step-by-step instructions for kids
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "💡 어린이 합체 가이드 단계별 설명:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryMint
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val nameA = elementA?.nameKo ?: ""
                            val symA = elementA?.id ?: ""
                            val nameB = elementB?.nameKo ?: ""
                            val symB = elementB?.id ?: ""

                            Text(
                                text = "1️⃣ 원소 도구상자에서 **$nameA ($symA)** 친구를 가볍게 '콕' 눌러 수조로 보냅니다.",
                                fontSize = 11.sp,
                                color = ColorC,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "2️⃣ 그 다음, 반대쪽 수조에 **$nameB ($symB)** 친구를 '콕' 눌러 준비시킵니다.",
                                fontSize = 11.sp,
                                color = ColorC,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "3️⃣ 두 원소 캐릭터를 손가락으로 드래그해 서로 부딪히게 밀면 끝! 🧪✨",
                                fontSize = 11.sp,
                                color = ColorC,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Description of chemical details
                        Text(
                            text = "💡 알고 가기: " + recipe.description,
                            fontSize = 10.5.sp,
                            color = SoftGray,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LemonCream.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto Prepare ingredients to current workspace!
                Button(
                    onClick = {
                        viewModel.fillWorkspace(recipe.ingredientA, recipe.ingredientB)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMint),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🧪", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "이 재료들 실험대에 바로 준비하기",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Paginated Navigator Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentPage > 0) currentPage-- },
                        enabled = currentPage > 0,
                        colors = ButtonDefaults.textButtonColors(contentColor = SecondaryPeach)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "◀", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "이전 비법", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "${currentPage + 1} / ${recipes.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGray
                    )

                    TextButton(
                        onClick = { if (currentPage < recipes.size - 1) currentPage++ },
                        enabled = currentPage < recipes.size - 1,
                        colors = ButtonDefaults.textButtonColors(contentColor = SecondaryPeach)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "다음 비법", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "▶", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchingDogamDialog(
    state: GameUiState,
    onDismiss: () -> Unit
) {
    val recipes = CompoundRecipe.ALL_RECIPES
    val unlockedIds = remember(state.unlockedFormulas) { state.unlockedFormulas.map { it.formulaId }.toSet() }
    val unlockedCount = state.unlockedFormulas.size
    val totalCount = recipes.size
    
    // Filters: 0 -> All, 1 -> Discovered, 2 -> Locked
    var filterTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Subscripts helper
    val formatSubscript = { text: String ->
        text.map { char ->
            when (char) {
                '0' -> '₀'
                '1' -> '₁'
                '2' -> '₂'
                '3' -> '₃'
                '4' -> '₄'
                '5' -> '₅'
                '6' -> '₆'
                '7' -> '₇'
                '8' -> '₈'
                '9' -> '₉'
                else -> char
            }
        }.joinToString("")
    }

    // Expandable item state
    var expandedFormulaId by remember { mutableStateOf<String?>(null) }

    val filteredRecipes = remember(filterTab, searchQuery, unlockedIds) {
        recipes.filter { recipe ->
            val isDiscovered = unlockedIds.contains(recipe.formulaId)
            
            // Tab filter
            val tabMatch = when (filterTab) {
                1 -> isDiscovered
                2 -> !isDiscovered
                else -> true
            }
            
            // Search filter
            val searchMatch = if (searchQuery.isBlank()) {
                true
            } else {
                if (isDiscovered) {
                    recipe.nameKo.contains(searchQuery, ignoreCase = true) || 
                    recipe.formulaId.contains(searchQuery, ignoreCase = true) || 
                    recipe.nameEn.contains(searchQuery, ignoreCase = true)
                } else {
                    // Locked items can be searched by ingredients
                    val ingredientA = ChemicalElement.BASE_MAP[recipe.ingredientA]
                    val ingredientB = ChemicalElement.BASE_MAP[recipe.ingredientB]
                    val nameA = ingredientA?.nameKo ?: ""
                    val nameB = ingredientB?.nameKo ?: ""
                    nameA.contains(searchQuery, ignoreCase = true) || nameB.contains(searchQuery, ignoreCase = true)
                }
            }
            
            tabMatch && searchMatch
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCream),
            border = BorderStroke(2.dp, PrimaryMint.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📖", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "꼬마 과학자의 매칭 도감",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepMint
                            )
                            Text(
                                text = "내가 합체에 성공한 화학식 기록첩",
                                fontSize = 10.5.sp,
                                color = SoftGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PastelMint)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DeepMint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Dashboard Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                    border = BorderStroke(1.dp, PastelMint)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧪 물질 발견 진행도",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorC
                            )
                            Text(
                                text = "$unlockedCount / $totalCount 발견함 (${(unlockedCount * 100) / totalCount}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SecondaryPeach
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Beautiful linear progress bar with rounded corners
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(PastelMint)
                        ) {
                            val fraction = if (totalCount > 0) unlockedCount.toFloat() / totalCount.toFloat() else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(PrimaryMint, SecondaryPeach)
                                        )
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar and Filters
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("원소 이름 또는 화학식 검색...", fontSize = 12.sp, color = SoftGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfacePaper,
                        unfocusedContainerColor = SurfacePaper,
                        focusedBorderColor = PrimaryMint,
                        unfocusedBorderColor = PastelMint
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SoftGray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = SoftGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Mini filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterLabels = listOf("전체 ($totalCount)", "발견함 ($unlockedCount)", "미발견 (${totalCount - unlockedCount})")
                    filterLabels.forEachIndexed { index, label ->
                        val isSelected = filterTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DeepMint else SurfacePaper)
                                .border(1.dp, if (isSelected) DeepMint else PastelMint, RoundedCornerShape(12.dp))
                                .clickable { filterTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else ColorC
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Formulas
                if (filteredRecipes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔍", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "검색 조건에 맞는 화학식이 없어요.",
                                fontSize = 12.sp,
                                color = SoftGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filteredRecipes) { recipe ->
                            val isDiscovered = unlockedIds.contains(recipe.formulaId)
                            val isExpanded = expandedFormulaId == recipe.formulaId
                            
                            val elementA = ChemicalElement.BASE_MAP[recipe.ingredientA]
                            val elementB = ChemicalElement.BASE_MAP[recipe.ingredientB]
                            
                            val nameA = elementA?.nameKo ?: ""
                            val nameB = elementB?.nameKo ?: ""

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedFormulaId = if (isExpanded) null else recipe.formulaId
                                    },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDiscovered) SurfacePaper else SurfacePaper.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(
                                    width = if (isExpanded) 1.5.dp else 1.dp,
                                    color = if (isExpanded) SecondaryPeach else (if (isDiscovered) PastelMint else SoftGray.copy(alpha = 0.2f))
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Emoji bubble
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDiscovered) recipe.color.copy(alpha = 0.15f)
                                                    else SoftGray.copy(alpha = 0.1f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDiscovered) recipe.color.copy(alpha = 0.5f)
                                                    else SoftGray.copy(alpha = 0.2f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isDiscovered) recipe.characterEmoji else "🔒",
                                                fontSize = 22.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Left side details (Symbol, Name)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isDiscovered) formatSubscript(recipe.formulaId) else "???",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDiscovered) ColorC else SoftGray
                                                )
                                                
                                                if (isDiscovered) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(PastelMint)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "발견완료 🎉",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = DeepMint
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (isDiscovered) recipe.nameKo else "비밀 물질 ($nameA + $nameB)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDiscovered) ColorC.copy(alpha = 0.9f) else SoftGray
                                            )
                                        }

                                        // Right side action indicator
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = SoftGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Expanded content
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = PastelMint, thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        if (isDiscovered) {
                                            // Discovered Details
                                            Text(
                                                text = "🧪 화학 반응식",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryMint
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            // Equation presentation e.g. "2 H (수소) + O (산소) ➔ H₂O (물)"
                                            val equationText = when (recipe.formulaId) {
                                                "H2O" -> "수소 (H) × 2 + 산소 (O) ➔ 물 (H₂O)"
                                                "CO2" -> "탄소 (C) + 산소 (O) × 2 ➔ 이산화탄소 (CO₂)"
                                                "NaCl" -> "나트륨 (Na) + 염소 (Cl) ➔ 소금 (NaCl)"
                                                "HCl" -> "수소 (H) + 염소 (Cl) ➔ 염산 (HCl)"
                                                "NH3" -> "질소 (N) + 수소 (H) × 3 ➔ 암모니아 (NH₃)"
                                                "Fe2O3" -> "철 (Fe) × 2 + 산소 (O) × 3 ➔ 녹 (Fe₂O₃)"
                                                "CH4" -> "탄소 (C) + 수소 (H) × 4 ➔ 메탄가스 (CH₄)"
                                                else -> "$nameA + $nameB ➔ ${recipe.formulaId}"
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SecondaryBackground, shape = RoundedCornerShape(12.dp))
                                                    .padding(10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = equationText,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = ColorC,
                                                    textAlign = TextAlign.Center
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "💡 물질 설명 및 상식",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryMint
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = recipe.description,
                                                fontSize = 11.sp,
                                                color = ColorC,
                                                lineHeight = 15.sp
                                            )
                                        } else {
                                            // Locked details - clue
                                            Text(
                                                text = "🔒 미발견 결합 물질 힌트!",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TertiaryPink
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "이 물질은 **${nameA}** 원소와 **${nameB}** 원소를 결합하면 만들 수 있어요! 두 원소를 실험대에 배치해 서로 만나게 해보세요! 😉",
                                                fontSize = 11.sp,
                                                color = ColorC,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyHintDialog(
    state: GameUiState,
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDate = remember { DailyQuizManager.getCurrentDateString() }
    
    // Check hints used today
    val hintsUsed = if (state.progress.lastHintDate == currentDate) {
        state.progress.hintsUsedToday
    } else {
        0
    }
    val remainingChances = (3 - hintsUsed).coerceAtLeast(0)

    val formatSubscript = { text: String ->
        text.map { char ->
            when (char) {
                '0' -> '₀'
                '1' -> '₁'
                '2' -> '₂'
                '3' -> '₃'
                '4' -> '₄'
                '5' -> '₅'
                '6' -> '₆'
                '7' -> '₇'
                '8' -> '₈'
                '9' -> '₉'
                else -> char
            }
        }.joinToString("")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCream),
            border = BorderStroke(2.dp, SecondaryPeach.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "꼬마 과학자의 힌트 찬스",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryPeach
                            )
                            Text(
                                text = "어려운 실험을 해결해주는 인공지능 멘토",
                                fontSize = 10.sp,
                                color = SoftGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SoftPeach)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SecondaryPeach,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Limit Progress / Indicator Cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                    border = BorderStroke(1.dp, SoftPeach)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "오늘의 힌트 남은 기회",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftGray
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        // Heart rating indicator representing remaining hints
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..3) {
                                Text(
                                    text = if (i <= remainingChances) "💛" else "🤍",
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "오늘 사용할 수 있는 기회: $remainingChances / 3회",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingChances > 0) DeepMint else TertiaryPink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val activeHint = state.activeHintRecipe

                if (activeHint != null) {
                    // Show Active Hint
                    val elementA = ChemicalElement.BASE_MAP[activeHint.ingredientA]
                    val elementB = ChemicalElement.BASE_MAP[activeHint.ingredientB]
                    
                    if (elementA != null && elementB != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🧪 오늘의 추천 연구 과제!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepMint
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Hint detail card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfacePaper),
                                border = BorderStroke(1.5.dp, PrimaryMint.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Target emoji and name
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(activeHint.color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = activeHint.characterEmoji, fontSize = 22.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = activeHint.nameKo,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                            Text(
                                                text = formatSubscript(activeHint.formulaId),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SoftGray
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = PastelMint, thickness = 1.dp)
                                    
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "아래 두 원소를 실험대에 나란히 배치해 보세요!",
                                        fontSize = 11.sp,
                                        color = SoftGray,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Display the two ingredient elements beautifully
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Element A Bubble
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(elementA.color.copy(alpha = 0.3f))
                                                    .border(1.5.dp, elementA.color, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = elementA.symbol,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = elementA.nameKo,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                        }

                                        // Plus Operator
                                        Text(
                                            text = "➕",
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        )

                                        // Element B Bubble
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(elementB.color.copy(alpha = 0.3f))
                                                    .border(1.5.dp, elementB.color, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = elementB.symbol,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = elementB.nameKo,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Instant prepare button to place ingredients directly in the workspace slots
                                    Button(
                                        onClick = {
                                            viewModel.fillWorkspace(elementA.id, elementB.id)
                                            Toast.makeText(context, "실험대에 두 원소가 준비되었습니다! 🧪✨", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SoftPeach,
                                            contentColor = SecondaryPeach
                                        ),
                                        border = BorderStroke(1.dp, SecondaryPeach.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("fill_workspace_from_hint_button"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "🧪 실험대에 바로 준비하기",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "💡 실험대 위의 합성 버튼을 누르면 새로운 물질이 탄생해요!",
                                fontSize = 9.5.sp,
                                color = SoftGray,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // No Active Hint -> Show unlock choice or no remaining hints warning
                    if (remainingChances > 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "실험을 하다가 막히셨나요? 🤔",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "힌트를 사용하면 새로운 물질을 만드는 데 꼭 필요한 두 원소 조합 정보를 알려드려요!",
                                fontSize = 11.sp,
                                color = SoftGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    viewModel.revealHint(
                                        onSuccess = {
                                            Toast.makeText(context, "새로운 힌트가 도착했어요! 💡✨", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { errorMsg ->
                                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SecondaryPeach,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                                    .testTag("reveal_hint_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = "💡", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "오늘의 힌트 확인하기 (-1회)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Out of hints
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "오늘의 힌트를 모두 사용하셨어요! 😭",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TertiaryPink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "하지만 걱정하지 마세요! 다양한 원소들을 실험대에 자유롭게 끌어와서 섞어보는 것도 훌륭한 연구 방법이에요! 스스로 새로운 결합을 발견해 볼까요? 🧪✨ 내일 다시 새로운 힌트가 충전될 거예요!",
                                fontSize = 11.sp,
                                color = SoftGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(44.dp)
                                    .background(SoftGray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔒 힌트 소진 완료 (내일 충전돼요!)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


