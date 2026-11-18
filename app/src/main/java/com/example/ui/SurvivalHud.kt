package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.HudSnapshot
import com.example.game.SurvivalStatus
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberOrange
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.GlassHudBg
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SkyWater
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun SurvivalHud(
    hud: HudSnapshot,
    survivalStatus: SurvivalStatus,
    playerHunger: Float,
    playerThirst: Float,
    onJoystickMove: (Float, Float) -> Unit,
    onPrimaryAction: () -> Unit,
    onJump: () -> Unit,
    onToggleSprint: () -> Unit,
    onSelectSlot: (Int) -> Unit,
    onOpenCrafting: () -> Unit,
    onRefuelCampfire: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit
) {
    // Red Damage Vignette Pulse
    var showDamageFlash by remember { mutableStateOf(false) }
    LaunchedEffect(hud.damageFlashSeq) {
        if (hud.damageFlashSeq > 0) {
            showDamageFlash = true
            delay(220)
            showDamageFlash = false
        }
    }
    val flashAlpha by animateFloatAsState(
        targetValue = if (showDamageFlash) 1f else 0f,
        animationSpec = tween(160),
        label = "damage_flash"
    )

    // Floating Toast Timer
    var toastVisible by remember { mutableStateOf(false) }
    LaunchedEffect(hud.toastSeq) {
        if (hud.toastSeq > 0 && hud.toastMessage.isNotEmpty()) {
            toastVisible = true
            delay(2200)
            toastVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Damage Vignette Overlay
        if (flashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(flashAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color(0x8CDC2626)
                            )
                        )
                    )
            )
        }

        // Center Crosshair & Target Prompt
        val hasTarget = hud.targetPrompt != null
        val crosshairScale by animateFloatAsState(
            targetValue = if (hasTarget) 1.45f else 1.0f,
            animationSpec = tween(120),
            label = "crosshair_scale"
        )
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(crosshairScale)
                    .clip(CircleShape)
                    .background(if (hasTarget) AmberGold.copy(alpha = 0.30f) else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (hasTarget) AmberGold else Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
            )
        }

        if (hud.targetPrompt != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 30.dp)
                    .clip(CircleShape)
                    .background(GlassHudBg)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = hud.targetPrompt,
                    color = AmberGold,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Safe-area responsive HUD container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val isPortraitOrNarrow = maxHeight > maxWidth || maxWidth < 500.dp
            val isCompactLandscape = !isPortraitOrNarrow && maxWidth < 840.dp

            // TOP BAR(S)
            if (isPortraitOrNarrow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        VitalsCard(
                            hud = hud,
                            survivalStatus = survivalStatus,
                            playerHunger = playerHunger,
                            playerThirst = playerThirst,
                            modifier = Modifier.weight(1.05f)
                        )
                        ClockCompassCard(
                            hud = hud,
                            compact = true,
                            onToggleMute = onToggleMute,
                            onTogglePause = onTogglePause,
                            modifier = Modifier.weight(0.95f)
                        )
                    }
                    ResourceAndCraftBar(
                        hud = hud,
                        compact = true,
                        onOpenCrafting = onOpenCrafting,
                        onRefuelCampfire = onRefuelCampfire
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    VitalsCard(
                        hud = hud,
                        survivalStatus = survivalStatus,
                        playerHunger = playerHunger,
                        playerThirst = playerThirst,
                        modifier = Modifier.width(if (isCompactLandscape) 190.dp else 245.dp)
                    )
                    ResourceAndCraftBar(
                        hud = hud,
                        compact = isCompactLandscape,
                        onOpenCrafting = onOpenCrafting,
                        onRefuelCampfire = onRefuelCampfire
                    )
                    ClockCompassCard(
                        hud = hud,
                        compact = isCompactLandscape,
                        onToggleMute = onToggleMute,
                        onTogglePause = onTogglePause
                    )
                }
            }

            // Floating Toast Pill
            AnimatedVisibility(
                visible = toastVisible,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isPortraitOrNarrow) 118.dp else 66.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GlassHudBg)
                        .border(1.dp, ForestEmerald.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = hud.toastMessage,
                        color = Color(0xFFA7F3D0),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // TOP-RIGHT CORNER (below Clock/Resources): Circular Navigation Minimap
            val minimapSize = if (isPortraitOrNarrow || isCompactLandscape) 86.dp else 98.dp
            MinimapOverlay(
                hud = hud,
                sizeDp = minimapSize,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = if (isPortraitOrNarrow) 124.dp else 54.dp,
                        end = 2.dp
                    )
            )

            // BOTTOM-LEFT: Virtual Touch Joystick
            val joySize = if (isPortraitOrNarrow) 104.dp else if (isCompactLandscape) 104.dp else 116.dp
            VirtualJoystick(
                zoneSizeDp = joySize,
                onMove = onJoystickMove,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 4.dp)
            )

            // BOTTOM-CENTER: 6-Slot Quick Hotbar
            val slotW = if (isPortraitOrNarrow || isCompactLandscape) 42.dp else 48.dp
            val slotH = if (isPortraitOrNarrow || isCompactLandscape) 45.dp else 50.dp
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isPortraitOrNarrow) 118.dp else 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassHudBg)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HotbarSlotItem(0, "1", "👊", "අත", null, hud.selectedSlot == 0, true, AmberGold, slotW, slotH) { onSelectSlot(0) }
                HotbarSlotItem(1, "2", "🪓", "පොරව", null, hud.selectedSlot == 1, hud.hasAxe, AmberGold, slotW, slotH) { onSelectSlot(1) }
                HotbarSlotItem(2, "3", "🔱", "හෙල්ල", null, hud.selectedSlot == 2, hud.hasSpear, AmberGold, slotW, slotH) { onSelectSlot(2) }
                HotbarSlotItem(3, "4", "🔦", "පන්දම", hud.torches, hud.selectedSlot == 3, hud.torches > 0, AmberGold, slotW, slotH) { onSelectSlot(3) }
                HotbarSlotItem(4, "5", "🍒", "පලතුරු", hud.berries, hud.selectedSlot == 4, hud.berries > 0, RoseCrimson, slotW, slotH) { onSelectSlot(4) }
                HotbarSlotItem(5, "6", "🩹", "බෙහෙත්", hud.bandages, hud.selectedSlot == 5, hud.bandages > 0, ForestEmerald, slotW, slotH) { onSelectSlot(5) }
            }

            // BOTTOM-RIGHT: Sprint, Jump & Action Buttons
            val smallBtnSize = if (isPortraitOrNarrow || isCompactLandscape) 48.dp else 56.dp
            val actionBtnSize = if (isPortraitOrNarrow || isCompactLandscape) 66.dp else 76.dp
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(smallBtnSize)
                        .clip(CircleShape)
                        .background(
                            if (hud.isSprinting) ForestEmerald.copy(alpha = 0.85f) else GlassHudBg
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (hud.isSprinting) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.30f),
                            shape = CircleShape
                        )
                        .clickable(onClick = onToggleSprint)
                        .testTag("sprint_button"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🏃", fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.sprint_label),
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(smallBtnSize)
                        .clip(CircleShape)
                        .background(GlassHudBg)
                        .border(1.5.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        .clickable(onClick = onJump)
                        .testTag("jump_button"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("⬆️", fontSize = 14.sp)
                    Text(
                        text = stringResource(R.string.jump_label),
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val actionIcon = when (hud.selectedSlot) {
                    1 -> "🪓"
                    2 -> "🔱"
                    3 -> "🔦"
                    4 -> "🍒"
                    5 -> "🩹"
                    else -> "👊"
                }
                Column(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(actionBtnSize)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AmberOrange, FlameOrange)))
                        .border(2.dp, Color(0xFFFDE68A).copy(alpha = 0.80f), CircleShape)
                        .clickable(onClick = onPrimaryAction)
                        .testTag("primary_action_button"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(actionIcon, fontSize = if (isPortraitOrNarrow || isCompactLandscape) 20.sp else 24.sp)
                    Text(
                        text = stringResource(R.string.action_label),
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalsCard(
    hud: HudSnapshot,
    survivalStatus: SurvivalStatus,
    playerHunger: Float,
    playerThirst: Float,
    modifier: Modifier = Modifier
) {
    val statusColor = when (survivalStatus) {
        SurvivalStatus.HEALTHY -> ForestEmerald
        SurvivalStatus.WARM_BY_FIRE -> FlameOrange
        SurvivalStatus.HUNGRY -> AmberGold
        SurvivalStatus.THIRSTY -> SkyWater
        SurvivalStatus.CRITICAL, SurvivalStatus.DEFEATED -> RoseCrimson
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassHudBg)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${survivalStatus.icon} ${survivalStatus.labelSi}",
                color = statusColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("survival_status_label")
            )
            Text(
                text = survivalStatus.labelEn,
                color = statusColor.copy(alpha = 0.85f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VitalBar(
                label = stringResource(R.string.health_label),
                valueText = "${kotlin.math.ceil(hud.hp).toInt()}",
                progress = (hud.hp / 100f).coerceIn(0f, 1f),
                labelColor = Color(0xFFFDA4AF),
                gradient = listOf(Color(0xFFE11D48), Color(0xFFF87171)),
                modifier = Modifier.weight(1f)
            )
            VitalBar(
                label = stringResource(R.string.hunger_label),
                valueText = "${kotlin.math.ceil(playerHunger).toInt()}%",
                progress = (playerHunger / 100f).coerceIn(0f, 1f),
                labelColor = Color(0xFFFCD34D),
                gradient = listOf(Color(0xFFD97706), Color(0xFFFACC15)),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VitalBar(
                label = stringResource(R.string.thirst_label),
                valueText = "${kotlin.math.ceil(playerThirst).toInt()}%",
                progress = (playerThirst / 100f).coerceIn(0f, 1f),
                labelColor = Color(0xFF7DD3FC),
                gradient = listOf(Color(0xFF2563EB), Color(0xFF22D3EE)),
                modifier = Modifier.weight(1f)
            )
            VitalBar(
                label = stringResource(R.string.stamina_label),
                valueText = "${kotlin.math.ceil(hud.stamina).toInt()}%",
                progress = (hud.stamina / 100f).coerceIn(0f, 1f),
                labelColor = Color(0xFF6EE7B7),
                gradient = listOf(Color(0xFF059669), Color(0xFF4ADE80)),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResourceAndCraftBar(
    hud: HudSnapshot,
    compact: Boolean,
    onOpenCrafting: () -> Unit,
    onRefuelCampfire: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(GlassHudBg)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            ResourceCounter("🪵", hud.wood, Color(0xFFFDE68A))
            ResourceCounter("🪨", hud.stone, Color(0xFFE7E5E4))
            ResourceCounter("🍒", hud.berries, Color(0xFFFECDD3))
            ResourceCounter("🥩", hud.rawMeat, Color(0xFFFCA5A5))

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )

            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(AmberOrange, FlameOrange)))
                    .border(1.dp, AmberGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenCrafting)
                    .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 5.dp)
                    .testTag("crafting_book_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("📖", fontSize = 12.sp)
                Text(
                    text = stringResource(R.string.crafting_book_btn),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (hud.nearCampfireForRefuel) {
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(FlameOrange.copy(alpha = 0.92f))
                    .border(1.dp, AmberGold, CircleShape)
                    .clickable(onClick = onRefuelCampfire)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .testTag("refuel_campfire_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("🔥", fontSize = 12.sp)
                Text(
                    text = "${stringResource(R.string.refuel_label)} (🪵 2)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ClockCompassCard(
    hud: HudSnapshot,
    compact: Boolean,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clockInfo = formatClockInfo(hud.timeOfDay)
    val compassDir = formatCompass(hud.yaw)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GlassHudBg)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp)
    ) {
        Text(clockInfo.icon, fontSize = if (compact) 15.sp else 18.sp)
        Column(modifier = if (compact) Modifier.weight(1f, fill = false) else Modifier) {
            Text(
                text = "දිනය ${hud.dayCount} - ${clockInfo.phase}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontSize = if (compact) 9.5.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${clockInfo.clockStr} | ${hud.score.toInt()}",
                color = Color(0xFFD6D3D1),
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (compact) 9.sp else 10.sp,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                .padding(horizontal = 5.dp, vertical = 3.dp)
        ) {
            Text(
                text = "🧭$compassDir",
                color = AmberGold,
                fontSize = if (compact) 9.5.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(if (compact) 30.dp else 34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onToggleMute)
                .testTag("mute_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(if (hud.isMuted) "🔇" else "🔊", fontSize = 13.sp)
        }

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(if (compact) 30.dp else 34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onTogglePause)
                .testTag("pause_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(if (hud.isPaused) "▶" else "⏸", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VitalBar(
    label: String,
    valueText: String,
    progress: Float,
    labelColor: Color,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(valueText, color = labelColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF030712))
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Brush.horizontalGradient(gradient))
            )
        }
    }
}

@Composable
private fun ResourceCounter(icon: String, count: Int, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 12.sp)
        Text(
            text = "$count",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun HotbarSlotItem(
    index: Int,
    keyNumber: String,
    icon: String,
    label: String,
    count: Int?,
    selected: Boolean,
    unlocked: Boolean,
    countColor: Color,
    slotWidth: Dp,
    slotHeight: Dp,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = tween(120),
        label = "hotbar_scale"
    )
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(width = slotWidth, height = slotHeight)
            .scale(scale)
            .alpha(if (unlocked) 1f else 0.45f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) AmberOrange.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.06f)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AmberGold else Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag("hotbar_slot_$index")
    ) {
        Text(
            text = keyNumber,
            color = Color(0xFF9CA3AF),
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 3.dp, top = 1.dp)
        )
        if (count != null) {
            Text(
                text = "$count",
                color = countColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp, top = 1.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 15.sp)
            Text(
                text = label,
                color = Color.White,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun VirtualJoystick(
    zoneSizeDp: Dp = 110.dp,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val knobSizeDp = 42.dp
    val maxDistPx = with(LocalDensity.current) { (zoneSizeDp * 0.32f).toPx() }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(zoneSizeDp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(2.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            .pointerInput(maxDistPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)

                    fun updateFromPos(pos: Offset) {
                        val dx = pos.x - center.x
                        val dy = pos.y - center.y
                        val dist = hypot(dx, dy)
                        val clamped = dist.coerceAtMost(maxDistPx)
                        val nx = if (dist > 0.001f) dx / dist else 0f
                        val ny = if (dist > 0.001f) dy / dist else 0f
                        knobOffset = Offset(nx * clamped, ny * clamped)
                        onMove(
                            (nx * (clamped / maxDistPx)).coerceIn(-1f, 1f),
                            (ny * (clamped / maxDistPx)).coerceIn(-1f, 1f)
                        )
                    }

                    updateFromPos(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        updateFromPos(change.position)
                    }
                    knobOffset = Offset.Zero
                    onMove(0f, 0f)
                }
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size(knobSizeDp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.80f),
                            Color(0xFFC8DCD2).copy(alpha = 0.35f)
                        )
                    )
                )
        )
    }
}

private data class ClockInfo(val icon: String, val phase: String, val clockStr: String)

private fun formatClockInfo(timeOfDay: Float): ClockInfo {
    val totalHours = (timeOfDay * 24f).toInt().coerceIn(0, 23)
    val totalMins = (((timeOfDay * 24f) - totalHours) * 60f).toInt().coerceIn(0, 59)
    val clockStr = "%02d:%02d".format(totalHours, totalMins)
    return when {
        timeOfDay in 0.50f..0.649f -> ClockInfo("🌅", "සැන්දෑව", clockStr)
        timeOfDay in 0.65f..0.90f -> ClockInfo("🌙", "රාත්‍රිය", clockStr)
        timeOfDay > 0.90f -> ClockInfo("🌤️", "උදෑසන", clockStr)
        else -> ClockInfo("☀️", "දහවල්", clockStr)
    }
}

private fun formatCompass(yaw: Float): String {
    val degRaw = ((-yaw * 180.0 / PI) % 360.0 + 360.0) % 360.0
    val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = (degRaw / 45.0).roundToInt() % 8
    return dirs[idx]
}
