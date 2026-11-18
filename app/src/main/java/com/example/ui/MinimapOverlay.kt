package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.HudSnapshot
import com.example.game.MinimapKind
import com.example.game.SurvivalGameEngine
import com.example.ui.theme.AmberGold
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.GlassHudBg
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.SkyWater
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val RADAR_WORLD_RADIUS = 52f

@Composable
fun MinimapOverlay(
    hud: HudSnapshot,
    sizeDp: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(GlassHudBg)
            .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .semantics {
                contentDescription = "Island navigation minimap showing player position and nearby landmarks"
            }
            .testTag("minimap_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusPx = size.minDimension * 0.5f
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val scale = radiusPx / RADAR_WORLD_RADIUS

            val circleClip = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center.x - radiusPx,
                        center.y - radiusPx,
                        center.x + radiusPx,
                        center.y + radiusPx
                    )
                )
            }

            clipPath(circleClip) {
                // Ocean backdrop
                drawCircle(
                    color = Color(0xFF063759),
                    radius = radiusPx,
                    center = center
                )

                // Island Center mapped relative to Player (North-Up: +X right, +Z down)
                val islandCenterOffset = Offset(
                    x = center.x + (0f - hud.playerX) * scale,
                    y = center.y + (0f - hud.playerZ) * scale
                )
                val islandScreenR = SurvivalGameEngine.ISLAND_RADIUS * scale

                // Sandy beach rim
                drawCircle(
                    color = Color(0xFFD6B87C).copy(alpha = 0.72f),
                    radius = (SurvivalGameEngine.ISLAND_RADIUS + 1.5f) * scale,
                    center = islandCenterOffset
                )
                // Forest island terrain
                drawCircle(
                    color = Color(0xFF1A3F23),
                    radius = (islandScreenR - 1.5f * scale).coerceAtLeast(1f),
                    center = islandCenterOffset
                )

                // Subtle radar distance rings
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = radiusPx * 0.5f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Freshwater Pond landmark (shore + glowing water)
                val pondScreen = Offset(
                    x = center.x + (SurvivalGameEngine.POND_X - hud.playerX) * scale,
                    y = center.y + (SurvivalGameEngine.POND_Z - hud.playerZ) * scale
                )
                drawCircle(
                    color = Color(0xFFD6B87C).copy(alpha = 0.75f),
                    radius = (SurvivalGameEngine.POND_RADIUS + 1.2f) * scale,
                    center = pondScreen
                )
                drawCircle(
                    color = SkyWater.copy(alpha = 0.90f),
                    radius = SurvivalGameEngine.POND_RADIUS * scale,
                    center = pondScreen
                )

                // Dynamic Landmarks: Trees, Rocks, Bushes, Campfires, Wolves
                for (pt in hud.minimapPoints) {
                    val sx = center.x + (pt.x - hud.playerX) * scale
                    val sy = center.y + (pt.z - hud.playerZ) * scale
                    val distPx = hypot(sx - center.x, sy - center.y)

                    when (pt.kind) {
                        MinimapKind.TREE -> {
                            if (distPx <= radiusPx) {
                                drawCircle(
                                    color = Color(0xFF22C55E).copy(alpha = 0.55f),
                                    radius = 1.8.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                        }
                        MinimapKind.ROCK -> {
                            if (distPx <= radiusPx) {
                                drawCircle(
                                    color = Color(0xFF9CA3AF).copy(alpha = 0.85f),
                                    radius = 1.9.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                        }
                        MinimapKind.BUSH -> {
                            if (distPx <= radiusPx) {
                                drawCircle(
                                    color = RoseCrimson.copy(alpha = 0.85f),
                                    radius = 1.8.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                        }
                        MinimapKind.CAMPFIRE -> {
                            if (distPx <= radiusPx - 3.dp.toPx()) {
                                drawCircle(
                                    color = FlameOrange.copy(alpha = 0.35f),
                                    radius = 6.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                                drawCircle(
                                    color = AmberGold,
                                    radius = 3.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            } else {
                                drawEdgeBeacon(center, radiusPx - 4.dp.toPx(), sx, sy, FlameOrange)
                            }
                        }
                        MinimapKind.WOLF -> {
                            if (distPx <= radiusPx) {
                                drawCircle(
                                    color = Color(0xFFEF4444),
                                    radius = 2.7.dp.toPx(),
                                    center = Offset(sx, sy)
                                )
                            }
                        }
                    }
                }

                // Off-radar edge beacon for Pond if player walked far away
                val pondDist = hypot(pondScreen.x - center.x, pondScreen.y - center.y)
                if (pondDist > radiusPx) {
                    drawEdgeBeacon(center, radiusPx - 4.dp.toPx(), pondScreen.x, pondScreen.y, SkyWater)
                }

                // Player FOV Cone + Directional Arrow
                // Forward in XZ is (-sin(yaw), -cos(yaw)); North-Up canvas maps +X->right, +Z->down
                val fx = -sin(hud.yaw)
                val fy = -cos(hud.yaw)
                val rx = -fy
                val ry = fx

                val fovLen = 14.dp.toPx()
                val fovHalfW = 7.5.dp.toPx()
                val fovPath = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + fx * fovLen - rx * fovHalfW, center.y + fy * fovLen - ry * fovHalfW)
                    lineTo(center.x + fx * fovLen + rx * fovHalfW, center.y + fy * fovLen + ry * fovHalfW)
                    close()
                }
                drawPath(fovPath, color = Color.White.copy(alpha = 0.22f))

                val tipLen = 6.5.dp.toPx()
                val tailLen = 3.8.dp.toPx()
                val wingW = 4.2.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(center.x + fx * tipLen, center.y + fy * tipLen)
                    lineTo(center.x - fx * tailLen + rx * wingW, center.y - fy * tailLen + ry * wingW)
                    lineTo(center.x - fx * (tailLen * 0.45f), center.y - fy * (tailLen * 0.45f))
                    lineTo(center.x - fx * tailLen - rx * wingW, center.y - fy * tailLen - ry * wingW)
                    close()
                }
                drawPath(arrowPath, color = Color.White)
                drawPath(arrowPath, color = AmberGold, style = Stroke(width = 1.dp.toPx()))
            }
        }

        // North cardinal indicator
        Text(
            text = "N",
            color = AmberGold,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 1.dp)
        )
    }
}

private fun DrawScope.drawEdgeBeacon(
    center: Offset,
    edgeRadius: Float,
    targetX: Float,
    targetY: Float,
    color: Color
) {
    val ang = atan2(targetY - center.y, targetX - center.x)
    val bx = center.x + cos(ang) * edgeRadius
    val by = center.y + sin(ang) * edgeRadius
    drawCircle(
        color = color,
        radius = 2.6.dp.toPx(),
        center = Offset(bx, by)
    )
}
