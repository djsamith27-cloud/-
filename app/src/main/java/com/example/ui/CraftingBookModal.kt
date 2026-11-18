package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.CRAFTING_RECIPES
import com.example.game.CraftCategory
import com.example.game.CraftRecipe
import com.example.game.HudSnapshot
import com.example.game.RecipeCost
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberOrange
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.RoseCrimson
import com.example.ui.theme.WoodBookBorder
import com.example.ui.theme.WoodBookDark
import com.example.ui.theme.WoodBookLight

@Composable
fun CraftingBookModal(
    hud: HudSnapshot,
    onSelectTab: (CraftCategory) -> Unit,
    onCraft: (String) -> Unit,
    onClose: () -> Unit
) {
    val filteredRecipes = CRAFTING_RECIPES.filter {
        hud.currentCraftTab == CraftCategory.ALL || it.category == hud.currentCraftTab
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        val isNarrow = maxWidth < 600.dp

        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(WoodBookLight, WoodBookDark)))
                .border(2.5.dp, WoodBookBorder, RoundedCornerShape(22.dp))
                .padding(if (isNarrow) 12.dp else 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "📖", fontSize = if (isNarrow) 22.sp else 26.sp)
                    Column {
                        Text(
                            text = stringResource(R.string.crafting_book_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = if (isNarrow) 13.sp else 15.sp,
                            color = AmberGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!isNarrow) {
                            Text(
                                text = stringResource(R.string.crafting_book_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFEF3C7).copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                if (!isNarrow) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, AmberOrange.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪵 ${hud.wood}", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("🪨 ${hud.stone}", color = Color(0xFFD6D3D1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("🍒 ${hud.berries}", color = Color(0xFFFDA4AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("🥩 ${hud.rawMeat}", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RoseCrimson.copy(alpha = 0.85f))
                        .clickable(onClick = onClose)
                        .testTag("close_crafting_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }

            if (isNarrow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, AmberOrange.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪵 ${hud.wood}", color = AmberGold, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Text("🪨 ${hud.stone}", color = Color(0xFFD6D3D1), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Text("🍒 ${hud.berries}", color = Color(0xFFFDA4AF), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Text("🥩 ${hud.rawMeat}", color = Color(0xFFFCA5A5), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = WoodBookBorder.copy(alpha = 0.45f)
            )

            // Category Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CraftCategory.entries.forEach { tab ->
                    val selected = hud.currentCraftTab == tab
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) AmberOrange else Color.White.copy(alpha = 0.1f)
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("craft_tab_${tab.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.labelSi,
                            color = if (selected) Color.White else Color(0xFFFEF3C7),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Recipes Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 240.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredRecipes, key = { it.id }) { recipe ->
                    val owned = recipe.oneTime && (
                        (recipe.id == "axe" && hud.hasAxe) ||
                            (recipe.id == "spear" && hud.hasSpear)
                        )
                    val affordable = canAfford(hud, recipe.cost)

                    RecipeCard(
                        recipe = recipe,
                        hud = hud,
                        owned = owned,
                        affordable = affordable,
                        onCraft = { onCraft(recipe.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCard(
    recipe: CraftRecipe,
    hud: HudSnapshot,
    owned: Boolean,
    affordable: Boolean,
    onCraft: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF5E8CE).copy(alpha = 0.11f),
                        Color(0xFFD6BE94).copy(alpha = 0.06f)
                    )
                )
            )
            .border(1.dp, Color(0xFFD9B382).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, AmberOrange.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(recipe.icon, fontSize = 20.sp)
                }
                Column {
                    Text(
                        text = recipe.nameSi,
                        color = AmberGold,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = recipe.nameEn,
                        color = Color(0xFFFEF3C7).copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = recipe.descriptionSi,
                color = Color(0xFFE7E5E4).copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (recipe.cost.wood > 0) {
                    CostChip("🪵", hud.wood, recipe.cost.wood)
                }
                if (recipe.cost.stone > 0) {
                    CostChip("🪨", hud.stone, recipe.cost.stone)
                }
                if (recipe.cost.berries > 0) {
                    CostChip("🍒", hud.berries, recipe.cost.berries)
                }
                if (recipe.cost.rawMeat > 0) {
                    CostChip("🥩", hud.rawMeat, recipe.cost.rawMeat)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onCraft,
            enabled = !owned && affordable,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FlameOrange,
                contentColor = Color.White,
                disabledContainerColor = if (owned) {
                    ForestEmerald.copy(alpha = 0.28f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                disabledContentColor = if (owned) {
                    Color(0xFF6EE7B7)
                } else {
                    Color(0xFF78716C)
                }
            ),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("craft_button_${recipe.id}")
        ) {
            Text(
                text = when {
                    owned -> "✅ සාදා ඇත (Crafted)"
                    affordable -> "🔨 සාදන්න (Craft)"
                    else -> "අවශ්‍ය ද්‍රව්‍ය මදි"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CostChip(icon: String, have: Int, need: Int) {
    val ok = have >= need
    val bg = if (ok) Color(0xFF064E3B).copy(alpha = 0.85f) else Color(0xFF881337).copy(alpha = 0.85f)
    val border = if (ok) ForestEmerald.copy(alpha = 0.4f) else RoseCrimson.copy(alpha = 0.4f)
    val fg = if (ok) Color(0xFF6EE7B7) else Color(0xFFFDA4AF)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$icon $have/$need",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun canAfford(hud: HudSnapshot, cost: RecipeCost): Boolean {
    return hud.wood >= cost.wood &&
        hud.stone >= cost.stone &&
        hud.berries >= cost.berries &&
        hud.rawMeat >= cost.rawMeat
}
