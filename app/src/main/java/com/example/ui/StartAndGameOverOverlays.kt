package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.HudSnapshot
import com.example.ui.theme.AmberGold
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.ForestTeal
import com.example.ui.theme.GlassHudBg
import com.example.ui.theme.RoseCrimson

@Composable
fun StartOverlay(
    onStartGame: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(enabled = false) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val compactHeight = maxHeight < 420.dp
        val isNarrow = maxWidth < 560.dp

        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassHudBg)
                .border(1.5.dp, ForestEmerald.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isNarrow) 16.dp else 22.dp,
                    vertical = if (compactHeight) 12.dp else 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌲🔥🐺📖", fontSize = if (compactHeight) 22.sp else 28.sp)
            Spacer(modifier = Modifier.height(if (compactHeight) 2.dp else 4.dp))
            Text(
                text = stringResource(R.string.title_game_si),
                style = MaterialTheme.typography.headlineLarge,
                fontSize = if (compactHeight || isNarrow) 20.sp else 25.sp,
                lineHeight = if (compactHeight || isNarrow) 26.sp else 32.sp,
                color = AmberGold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.start_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center,
                fontSize = if (compactHeight) 11.sp else 12.sp,
                lineHeight = if (compactHeight) 15.sp else 17.sp
            )

            Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 12.dp))

            // Controls Cheat-sheet (2-col on portrait, 3-col on wide)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = if (compactHeight) 8.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (isNarrow) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ControlItem("Joystick:", "ඇවිදින්න", Modifier.weight(1f))
                        ControlItem("Swipe:", "වටපිට බලන්න", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ControlItem("Tap / Action:", "කපන්න / පහර දෙන්න", Modifier.weight(1f))
                        ControlItem("📖 Book:", "නිර්මාණ පොත", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ControlItem("Hotbar 1-6:", "ආයුධ තෝරන්න", Modifier.weight(1f))
                        ControlItem("Sprint / Jump:", "දුවන්න / පනින්න", Modifier.weight(1f))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlItem("Joystick / WASD:", "ඇවිදින්න", Modifier.weight(1f))
                        ControlItem("Swipe Screen:", "වටපිට බලන්න", Modifier.weight(1f))
                        ControlItem("Action / Tap:", "කපන්න / පහර", Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlItem("Hotbar 1 - 6:", "ආයුධ තෝරන්න", Modifier.weight(1f))
                        ControlItem("📖 Book [C]:", "නිර්මාණ පොත", Modifier.weight(1f))
                        ControlItem("Sprint / Jump:", "දුවන්න / පනින්න", Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 10.dp else 14.dp))

            Button(
                onClick = onStartGame,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth(if (isNarrow) 0.92f else 0.72f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(ForestEmerald, ForestTeal)))
                    .testTag("start_game_button")
            ) {
                Text(
                    text = stringResource(R.string.start_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ControlItem(keyLabel: String, desc: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$keyLabel ",
            color = AmberGold,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            color = Color(0xFFE5E7EB),
            fontSize = 10.5.sp
        )
    }
}

@Composable
fun GameOverOverlay(
    hud: HudSnapshot,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(GlassHudBg)
                .border(1.5.dp, RoseCrimson.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "💀", fontSize = 34.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.game_over_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFB7185),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hud.deathReason.ifEmpty { "වනාන්තරයේ දරුණු තත්වයන්ට ගොදුරු විය." },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.50f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.days_survived_label),
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${hud.dayCount}",
                        color = AmberGold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.total_score_label),
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${hud.score.toInt()}",
                        color = Color(0xFF6EE7B7),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(RoseCrimson, FlameOrange)))
                    .testTag("restart_game_button")
            ) {
                Text(
                    text = stringResource(R.string.restart_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun PauseOverlay(
    hud: HudSnapshot,
    onResume: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GlassHudBg)
                .border(1.5.dp, AmberGold.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "⏸️ ක්‍රීඩාව නවතා ඇත (Paused)", style = MaterialTheme.typography.headlineMedium, color = AmberGold)
            Text(
                text = "${hud.survivalStatus.icon} ${hud.survivalStatus.labelSi} • ❤️ ${hud.hp.toInt()} • දිනය ${hud.dayCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE5E7EB)
            )
            Button(
                onClick = onResume,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestEmerald, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("resume_game_button")
            ) {
                Text("දිගටම කරගෙන යන්න (Resume) ▶", fontWeight = FontWeight.ExtraBold)
            }
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pause_restart_button")
            ) {
                Text("නැවත අරඹන්න (Restart) ↻", fontWeight = FontWeight.Bold)
            }
        }
    }
}

