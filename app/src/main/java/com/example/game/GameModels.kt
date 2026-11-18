package com.example.game

import kotlin.math.sqrt

data class Vec3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun set(nx: Float, ny: Float, nz: Float) {
        x = nx; y = ny; z = nz
    }

    fun copyFrom(other: Vec3) {
        x = other.x; y = other.y; z = other.z
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)
}

enum class GamePhase {
    START, PLAYING, GAMEOVER
}

enum class CraftCategory(val labelSi: String) {
    ALL("සියල්ල (All)"),
    TOOLS("⚔️ ආයුධ (Tools)"),
    CAMP("🔥 කඳවුරු (Camp)"),
    FOOD("🍖 ආහාර (Food)")
}

data class RecipeCost(
    val wood: Int = 0,
    val stone: Int = 0,
    val berries: Int = 0,
    val rawMeat: Int = 0
)

data class CraftRecipe(
    val id: String,
    val category: CraftCategory,
    val icon: String,
    val nameSi: String,
    val nameEn: String,
    val descriptionSi: String,
    val cost: RecipeCost,
    val oneTime: Boolean
)

val CRAFTING_RECIPES = listOf(
    CraftRecipe(
        id = "axe",
        category = CraftCategory.TOOLS,
        icon = "🪓",
        nameSi = "ගල් පොරව",
        nameEn = "Stone Axe",
        descriptionSi = "ගස් කැපීම සහ ගල් කැඩීම දෙගුණයකින් වේගවත් කරයි. (Hotbar [2])",
        cost = RecipeCost(wood = 3, stone = 2),
        oneTime = true
    ),
    CraftRecipe(
        id = "spear",
        category = CraftCategory.TOOLS,
        icon = "🔱",
        nameSi = "දඩයම් හෙල්ල",
        nameEn = "Hunting Spear",
        descriptionSi = "රාත්‍රී වෘකයන්ට දැඩි ප්‍රහාර (52 DMG) එල්ල කළ හැකි දිගු ආයුධයකි. (Hotbar [3])",
        cost = RecipeCost(wood = 4, stone = 2),
        oneTime = true
    ),
    CraftRecipe(
        id = "torch",
        category = CraftCategory.CAMP,
        icon = "🔦",
        nameSi = "අත් පන්දම",
        nameEn = "Hand Torch",
        descriptionSi = "අඳුරු වනාන්තරයේ ඇවිදින විට ආලෝකය ලබා දෙයි. (Hotbar [4])",
        cost = RecipeCost(wood = 2, berries = 1),
        oneTime = false
    ),
    CraftRecipe(
        id = "campfire",
        category = CraftCategory.CAMP,
        icon = "🔥",
        nameSi = "ගිනි මැලය",
        nameEn = "Campfire",
        descriptionSi = "ඔබ ඉදිරියෙන් ගිනි මැලයක් දල්වයි. සෞඛ්‍යය වැඩි කර වෘකයන් පලවා හරියි.",
        cost = RecipeCost(wood = 5, stone = 2),
        oneTime = false
    ),
    CraftRecipe(
        id = "bandage",
        category = CraftCategory.FOOD,
        icon = "🩹",
        nameSi = "ඖෂධ වෙළුම් පටි",
        nameEn = "Herbal Bandage",
        descriptionSi = "තුවාල සුව කර සෞඛ්‍යය (+45 HP) ක්ෂණිකව වැඩි කරයි. (Hotbar [6])",
        cost = RecipeCost(berries = 3, wood = 1),
        oneTime = false
    ),
    CraftRecipe(
        id = "cooked_meat",
        category = CraftCategory.FOOD,
        icon = "🍖",
        nameSi = "පිසූ වෘක මස්",
        nameEn = "Cooked Meat Feast",
        descriptionSi = "වෘකයන්ගෙන් ලබාගත් මස් පිස ආහාරයට ගැනීමෙන් කුසගින්න (+65) සහ HP (+30) ලැබේ.",
        cost = RecipeCost(rawMeat = 1, wood = 1),
        oneTime = false
    )
)

enum class TargetType {
    TREE, ROCK, BUSH, WATER, WOLF
}

data class RayHit(
    val type: TargetType,
    val index: Int,
    val distance: Float,
    val hitPoint: Vec3,
    val label: String
)

data class TreeEntity(
    val x: Float,
    val z: Float,
    val scale: Float,
    val isPine: Boolean,
    val radius: Float,
    var hp: Float = 100f,
    var respawnTimer: Float = 0f
)

data class RockEntity(
    val x: Float,
    val z: Float,
    val scale: Float,
    val rotX: Float,
    val rotY: Float,
    val rotZ: Float,
    val radius: Float,
    var hp: Float = 100f,
    var respawnTimer: Float = 0f
)

data class BushEntity(
    val x: Float,
    val z: Float,
    var hasBerries: Boolean = true,
    var regrowTimer: Float = 0f
)

data class CampfireEntity(
    val x: Float,
    val z: Float,
    var fuel: Float = 100f,
    val maxFuel: Float = 100f
)

data class WolfEntity(
    var x: Float,
    var z: Float,
    var yaw: Float = 0f,
    var hp: Float,
    val speed: Float,
    var attackCooldown: Float = 0f,
    var knockbackX: Float = 0f,
    var knockbackZ: Float = 0f,
    var walkAnim: Float = 0f
)

data class ParticleEntity(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    val r: Float,
    val g: Float,
    val b: Float
)

enum class SurvivalStatus(val icon: String, val labelSi: String, val labelEn: String) {
    HEALTHY("💚", "සුවෙන්", "Healthy"),
    WARM_BY_FIRE("🔥", "උණුසුම් වෙමින්", "Warming"),
    HUNGRY("🍖", "කුසගිනියි", "Hungry"),
    THIRSTY("💧", "පිපාසයි", "Thirsty"),
    CRITICAL("🚨", "අවදානම්!", "Critical"),
    DEFEATED("💀", "පරාජිතයි", "Defeated")
}

enum class MinimapKind {
    CAMPFIRE, WOLF, TREE, ROCK, BUSH
}

data class MinimapPoint(
    val x: Float,
    val z: Float,
    val kind: MinimapKind
)

data class HudSnapshot(
    val gamePhase: GamePhase = GamePhase.START,
    val isCraftingOpen: Boolean = false,
    val isPaused: Boolean = false,
    val currentCraftTab: CraftCategory = CraftCategory.ALL,
    val hp: Float = 100f,
    val hunger: Float = 100f,
    val thirst: Float = 100f,
    val stamina: Float = 100f,
    val survivalStatus: SurvivalStatus = SurvivalStatus.HEALTHY,
    val wood: Int = 2,
    val stone: Int = 1,
    val berries: Int = 2,
    val rawMeat: Int = 0,
    val torches: Int = 0,
    val torchFuel: Float = 100f,
    val bandages: Int = 0,
    val hasAxe: Boolean = false,
    val hasSpear: Boolean = false,
    val selectedSlot: Int = 0,
    val score: Float = 0f,
    val dayCount: Int = 1,
    val timeOfDay: Float = 0.25f,
    val yaw: Float = 0f,
    val playerX: Float = 0f,
    val playerZ: Float = 10f,
    val minimapPoints: List<MinimapPoint> = emptyList(),
    val targetPrompt: String? = null,
    val nearCampfireForRefuel: Boolean = false,
    val nearWarmFire: Boolean = false,
    val isSprinting: Boolean = false,
    val isMuted: Boolean = false,
    val toastMessage: String = "",
    val toastSeq: Int = 0,
    val damageFlashSeq: Int = 0,
    val deathReason: String = ""
)
