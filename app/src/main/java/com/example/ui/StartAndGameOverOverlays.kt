package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(GlassHudBg)
                .border(1.5.dp, ForestEmerald.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🌲🔥🐺📖", fontSize = 30.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.title_game_si),
                style = MaterialTheme.typography.headlineLarge,
                color = AmberGold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.start_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Controls Cheat-sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ControlItem("Joystick / WASD:", "ඇවිදින්න", Modifier.weight(1f))
                    ControlItem("Swipe Screen:", "වටපිට බලන්න", Modifier.weight(1f))
                    ControlItem("Action / Tap:", "කපන්න / පහර දෙන්න", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ControlItem("Hotbar 1 - 6:", "ආයුධ තෝරන්න", Modifier.weight(1f))
                    ControlItem("📖 Book [C]:", "නිර්මාණ පොත", Modifier.weight(1f))
                    ControlItem("Sprint / Jump:", "දුවන්න / පනින්න", Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartGame,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            color = Color(0xFFE5E7EB),
            fontSize = 11.sp
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "💀", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(6.dp))
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
