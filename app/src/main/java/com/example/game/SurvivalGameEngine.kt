package com.example.game

import com.example.audio.SfxType
import com.example.audio.SoundSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class SurvivalGameEngine(
    private val sfx: SoundSynthesizer
) {
    companion object {
        const val ISLAND_RADIUS = 88f
        const val DAY_LENGTH_SEC = 85f
        const val POND_X = -14f
        const val POND_Z = -12f
        const val POND_Y = 0.06f
        const val POND_RADIUS = 10.5f
        const val EYE_HEIGHT = 1.75f
        const val RAY_MAX_DIST = 4.5f
    }

    val worldLock = Any()

    // Core game state
    var gamePhase: GamePhase = GamePhase.START
        private set
    var isCraftingOpen: Boolean = false
        private set
    var isPaused: Boolean = false
        private set
    var currentCraftTab: CraftCategory = CraftCategory.ALL
        private set

    // Time & score
    var score: Float = 0f
        private set
    var dayCount: Int = 1
        private set
    var timeOfDay: Float = 0.25f
        private set
    var elapsedSec: Float = 0f
        private set

    // Player transform & motion
    val playerPos = Vec3(0f, EYE_HEIGHT, 10f)
    var playerVelY: Float = 0f
        private set
    var yaw: Float = 0f
        private set
    var pitch: Float = 0f
        private set
    var onGround: Boolean = true
        private set

    // Player vitals & inventory
    var hp: Float = 100f
        private set
    var hunger: Float = 100f
        private set
    var thirst: Float = 100f
        private set
    var stamina: Float = 100f
        private set

    var wood: Int = 2
        private set
    var stone: Int = 1
        private set
    var berries: Int = 2
        private set
    var rawMeat: Int = 0
        private set
    var torches: Int = 0
        private set
    var torchFuel: Float = 100f
        private set
    var bandages: Int = 0
        private set
    var hasAxe: Boolean = false
        private set
    var hasSpear: Boolean = false
        private set
    var selectedSlot: Int = 0
        private set

    // Animations
    var swingTimer: Float = 0f
        private set
    var swingCooldown: Float = 0f
        private set
    var bobTimer: Float = 0f
        private set
    var isMovingNow: Boolean = false
        private set
    private var stepTimer: Float = 0f

    // Input state
    private var joyX: Float = 0f
    private var joyY: Float = 0f
    private var keyForward: Float = 0f
    private var keyRight: Float = 0f
    private var sprintToggle: Boolean = false
    private var keySprint: Boolean = false

    // World entities
    val trees = mutableListOf<TreeEntity>()
    val rocks = mutableListOf<RockEntity>()
    val bushes = mutableListOf<BushEntity>()
    val campfires = mutableListOf<CampfireEntity>()
    val wolves = mutableListOf<WolfEntity>()
    val particles = mutableListOf<ParticleEntity>()

    // Starfield coordinates (700 stars on upper sky hemisphere)
    val starPositions: FloatArray = FloatArray(700 * 3).also { arr ->
        val rng = Random(42)
        for (i in 0 until 700) {
            val theta = rng.nextFloat() * (2f * PI.toFloat())
            val cosPhi = rng.nextFloat() * 0.92f
            val phi = kotlin.math.acos(cosPhi.toDouble()).toFloat()
            val r = 160f
            arr[i * 3] = r * sin(phi) * cos(theta)
            arr[i * 3 + 1] = r * cos(phi)
            arr[i * 3 + 2] = r * sin(phi) * sin(theta)
        }
    }

    // Dynamic sky/light parameters for renderer
    var skyR = 0.494f
    var skyG = 0.784f
    var skyB = 0.890f
    var sunIntensity = 1.15f
    var hemiIntensity = 0.75f
    var starOpacity = 0f
    var sunDirX = 0.45f
    var sunDirY = 0.75f
    var sunDirZ = 0.35f

    private var currentTarget: RayHit? = null
    private var nearCampfireRefuel: Boolean = false
    private var nearWarmFire: Boolean = false
    private var toastMessage: String = ""
    private var toastSeq: Int = 0
    private var damageFlashSeq: Int = 0
    private var deathReason: String = ""

    private var hudEmitAccum: Float = 0f

    private val _hudState = MutableStateFlow(HudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    init {
        synchronized(worldLock) {
            buildWorld()
            updateLighting()
        }
        emitHudImmediate()
    }

    fun startGame() {
        synchronized(worldLock) {
            playerPos.set(0f, EYE_HEIGHT, 10f)
            playerVelY = 0f
            yaw = 0f
            pitch = 0f
            onGround = true
            hp = 100f
            hunger = 100f
            thirst = 100f
            stamina = 100f
            wood = 2
            stone = 1
            berries = 2
            rawMeat = 0
            torches = 0
            torchFuel = 100f
            bandages = 0
            hasAxe = false
            hasSpear = false
            selectedSlot = 0
            swingTimer = 0f
            swingCooldown = 0f
            bobTimer = 0f
            stepTimer = 0f
            sprintToggle = false
            isCraftingOpen = false
            isPaused = false
            currentCraftTab = CraftCategory.ALL
            score = 0f
            dayCount = 1
            timeOfDay = 0.25f
            deathReason = ""

            buildWorld()
            updateLighting()
            gamePhase = GamePhase.PLAYING
            postToast("📖 නිර්මාණ පොත (Crafting Book) විවෘත කර ආයුධ සාදන්න!")
        }
        emitHudImmediate()
    }

    private fun buildWorld() {
        trees.clear()
        rocks.clear()
        bushes.clear()
        campfires.clear()
        wolves.clear()
        particles.clear()

        fun isPositionValid(x: Float, z: Float, minClear: Float): Boolean {
            if (hypot(x, z) < 7f) return false
            if (hypot(x - POND_X, z - POND_Z) < POND_RADIUS + minClear) return false
            if (hypot(x, z) > ISLAND_RADIUS - 6f) return false
            return true
        }

        // 65 Pine & Oak Trees
        for (i in 0 until 65) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val dist = 8f + Random.nextFloat() * (ISLAND_RADIUS - 15f)
            val x = cos(ang) * dist
            val z = sin(ang) * dist
            if (!isPositionValid(x, z, 3.0f)) continue
            val scale = 0.85f + Random.nextFloat() * 0.55f
            val isPine = Random.nextFloat() > 0.4f
            trees.add(TreeEntity(x, z, scale, isPine, 0.85f * scale))
        }

        // 32 Rocks
        for (i in 0 until 32) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val dist = 7f + Random.nextFloat() * (ISLAND_RADIUS - 14f)
            val x = cos(ang) * dist
            val z = sin(ang) * dist
            if (!isPositionValid(x, z, 2.5f)) continue
            val scale = 1.1f + Random.nextFloat() * 0.5f
            rocks.add(
                RockEntity(
                    x = x,
                    z = z,
                    scale = scale,
                    rotX = Random.nextFloat() * 3.14f,
                    rotY = Random.nextFloat() * 3.14f,
                    rotZ = Random.nextFloat() * 3.14f,
                    radius = 1.1f
                )
            )
        }

        // 28 Berry Bushes
        for (i in 0 until 28) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val dist = 6f + Random.nextFloat() * (ISLAND_RADIUS - 14f)
            val x = cos(ang) * dist
            val z = sin(ang) * dist
            if (!isPositionValid(x, z, 2.0f)) continue
            bushes.add(BushEntity(x, z))
        }

        // Starter Campfire near spawn
        campfires.add(CampfireEntity(3.5f, 5.5f, 80f))
    }

    fun setJoystick(x: Float, y: Float) {
        joyX = x.coerceIn(-1f, 1f)
        joyY = y.coerceIn(-1f, 1f)
    }

    fun setKeyMovement(forward: Float, right: Float) {
        keyForward = forward.coerceIn(-1f, 1f)
        keyRight = right.coerceIn(-1f, 1f)
    }

    fun setKeySprint(active: Boolean) {
        keySprint = active
    }

    fun toggleSprint() {
        sprintToggle = !sprintToggle
        emitHudImmediate()
    }

    fun addLookDelta(dx: Float, dy: Float) {
        synchronized(worldLock) {
            if (gamePhase != GamePhase.PLAYING || isCraftingOpen || isPaused) return
            val sensitivity = 0.0042f
            yaw -= dx * sensitivity
            pitch -= dy * sensitivity
            val maxPitch = (PI / 2.0 - 0.08).toFloat()
            pitch = pitch.coerceIn(-maxPitch, maxPitch)
        }
    }

    fun jump() {
        synchronized(worldLock) {
            if (gamePhase != GamePhase.PLAYING || isCraftingOpen || isPaused) return
            if (onGround) {
                playerVelY = 7.2f
                onGround = false
            }
        }
    }

    fun setPaused(paused: Boolean) {
        synchronized(worldLock) {
            if (gamePhase != GamePhase.PLAYING) {
                isPaused = false
            } else {
                isPaused = paused
            }
        }
        emitHudImmediate()
    }

    fun toggleMute() {
        sfx.isMuted = !sfx.isMuted
        emitHudImmediate()
    }

    fun selectHotbarSlot(slotIdx: Int) {
        synchronized(worldLock) {
            when (slotIdx) {
                1 -> if (!hasAxe) {
                    postToast("🔒 ගල් පොරව තවම සාදා නැත! නිර්මාණ පොත බලන්න.")
                    emitHudImmediate()
                    return
                }
                2 -> if (!hasSpear) {
                    postToast("🔒 දඩයම් හෙල්ල තවම සාදා නැත! නිර්මාණ පොත බලන්න.")
                    emitHudImmediate()
                    return
                }
                3 -> if (torches <= 0) {
                    postToast("🔒 ඔබ ළඟ පන්දම් නැත! නිර්මාණ පොතෙන් සාදාගන්න.")
                    emitHudImmediate()
                    return
                }
                4 -> if (berries <= 0) {
                    postToast("🔒 පලතුරු නැත! පඳුරු වලින් කඩාගන්න.")
                    emitHudImmediate()
                    return
                }
                5 -> if (bandages <= 0) {
                    postToast("🔒 ඖෂධ වෙළුම් පටි නැත! නිර්මාණ පොතෙන් සාදාගන්න.")
                    emitHudImmediate()
                    return
                }
            }
            selectedSlot = slotIdx.coerceIn(0, 5)
        }
        emitHudImmediate()
    }

    fun toggleCraftingBook(forceState: Boolean? = null) {
        synchronized(worldLock) {
            if (gamePhase != GamePhase.PLAYING) return
            isCraftingOpen = forceState ?: !isCraftingOpen
        }
        emitHudImmediate()
    }

    fun setCraftTab(tab: CraftCategory) {
        synchronized(worldLock) {
            currentCraftTab = tab
        }
        emitHudImmediate()
    }

    fun isRecipeOwned(recipeId: String): Boolean {
        return when (recipeId) {
            "axe" -> hasAxe
            "spear" -> hasSpear
            else -> false
        }
    }

    fun canAfford(cost: RecipeCost): Boolean {
        return wood >= cost.wood &&
            stone >= cost.stone &&
            berries >= cost.berries &&
            rawMeat >= cost.rawMeat
    }

    fun executeCraft(recipeId: String) {
        synchronized(worldLock) {
            val recipe = CRAFTING_RECIPES.find { it.id == recipeId } ?: return
            if (isRecipeOwned(recipeId) || !canAfford(recipe.cost)) return

            wood -= recipe.cost.wood
            stone -= recipe.cost.stone
            berries -= recipe.cost.berries
            rawMeat -= recipe.cost.rawMeat

            sfx.play(SfxType.CRAFT)

            when (recipeId) {
                "axe" -> {
                    hasAxe = true
                    selectedSlot = 1
                    score += 100f
                    postToast("🪓 ගල් පොරව සෑදුවා! Hotbar [2] තෝරා ගන්නා ලදී.")
                }
                "spear" -> {
                    hasSpear = true
                    selectedSlot = 2
                    score += 180f
                    postToast("🔱 දඩයම් හෙල්ල සෑදුවා! Hotbar [3] තෝරා ගන්නා ලදී.")
                }
                "torch" -> {
                    torches += 1
                    torchFuel = 100f
                    selectedSlot = 3
                    score += 60f
                    postToast("🔦 අත් පන්දමක් සෑදුවා! රාත්‍රියේ ආලෝකය සඳහා [4] භාවිතා කරන්න.")
                }
                "campfire" -> {
                    val fx = playerPos.x + (-sin(yaw)) * 2.6f
                    val fz = playerPos.z + (-cos(yaw)) * 2.6f
                    campfires.add(CampfireEntity(fx, fz, 100f))
                    score += 90f
                    isCraftingOpen = false
                    postToast("🔥 ඔබ ඉදිරියෙන් නව ගිනි මැලයක් දල්වන ලදී!")
                }
                "bandage" -> {
                    bandages += 1
                    selectedSlot = 5
                    score += 50f
                    postToast("🩹 ඖෂධ වෙළුම් පටියක් සෑදුවා! [6] තෝරා භාවිතා කරන්න.")
                }
                "cooked_meat" -> {
                    hunger = min(100f, hunger + 65f)
                    hp = min(100f, hp + 30f)
                    sfx.play(SfxType.BERRY)
                    score += 120f
                    postToast("🍖 පිසූ මස් අනුභව කළා! (+65 කුසගින්න | +30 ❤️)")
                }
            }
        }
        emitHudImmediate()
    }

    fun refuelNearbyCampfire() {
        synchronized(worldLock) {
            if (gamePhase != GamePhase.PLAYING) return
            for (cf in campfires) {
                if (hypot(playerPos.x - cf.x, playerPos.z - cf.z) < 3.6f) {
                    if (wood >= 2) {
                        wood -= 2
                        cf.fuel = min(100f, cf.fuel + 55f)
                        sfx.play(SfxType.CRAFT)
                        postToast("🔥 ගිනි මැලයට දර 2ක් දමා අලුත් කළා!")
                    } else {
                        postToast("❌ ගිනි මැලයට දැමීමට දර 2ක් අවශ්‍යයි.")
                    }
                    emitHudImmediate()
                    return
                }
            }
        }
    }

    fun performPrimaryAction() {
        synchronized(worldLock) {
            if (gamePhase != GamePhase.PLAYING || isCraftingOpen || isPaused) return
            if (swingCooldown > 0f) return

            // Eating Berries (Slot 4)
            if (selectedSlot == 4) {
                if (berries > 0) {
                    berries--
                    hunger = min(100f, hunger + 30f)
                    hp = min(100f, hp + 10f)
                    swingTimer = 0.2f
                    swingCooldown = 0.35f
                    sfx.play(SfxType.BERRY)
                    postToast("🍒 පලතුරක් අනුභව කළා (+30 කුසගින්න | +10 ❤️)")
                    if (berries == 0) selectedSlot = 0
                    emitHudImmediate()
                }
                return
            }

            // Using Bandage (Slot 5)
            if (selectedSlot == 5) {
                if (bandages > 0) {
                    bandages--
                    hp = min(100f, hp + 45f)
                    swingTimer = 0.2f
                    swingCooldown = 0.35f
                    sfx.play(SfxType.CRAFT)
                    postToast("🩹 ඖෂධ වෙළුම් පටි භාවිතා කළා (+45 ❤️ සෞඛ්‍යය)")
                    if (bandages == 0) selectedSlot = 0
                    emitHudImmediate()
                }
                return
            }

            swingTimer = 0.24f
            swingCooldown = 0.34f
            sfx.play(SfxType.SWING)

            val hit = currentTarget ?: return
            val hpPt = hit.hitPoint

            when (hit.type) {
                TargetType.TREE -> {
                    val tree = trees.getOrNull(hit.index) ?: return
                    if (tree.hp <= 0f) return
                    val power = when (selectedSlot) {
                        1 -> 48f
                        2 -> 28f
                        else -> 18f
                    }
                    tree.hp -= power
                    sfx.play(SfxType.CHOP)
                    spawnParticles(hpPt.x, hpPt.y, hpPt.z, 0.47f, 0.21f, 0.06f, 8)
                    val gained = if (selectedSlot == 1) 2 else 1
                    wood += gained
                    score += 10f
                    postToast("+$gained 🪵 දර එකතු කරගත්තා")
                    if (tree.hp <= 0f) {
                        tree.respawnTimer = 45f
                        wood += 2
                        postToast("🌲 ගස කපා දැමුවා! +2 🪵")
                    }
                }
                TargetType.ROCK -> {
                    val rock = rocks.getOrNull(hit.index) ?: return
                    if (rock.hp <= 0f) return
                    val power = when (selectedSlot) {
                        1 -> 38f
                        2 -> 28f
                        else -> 12f
                    }
                    rock.hp -= power
                    sfx.play(SfxType.MINE)
                    spawnParticles(hpPt.x, hpPt.y, hpPt.z, 0.61f, 0.64f, 0.69f, 8)
                    val gained = if (selectedSlot == 1) 2 else 1
                    stone += gained
                    score += 15f
                    postToast("+$gained 🪨 ගල් එකතු කරගත්තා")
                    if (rock.hp <= 0f) {
                        rock.respawnTimer = 50f
                        stone += 2
                        postToast("🪨 ගල කැඩුවා! +2 🪨")
                    }
                }
                TargetType.BUSH -> {
                    val bush = bushes.getOrNull(hit.index) ?: return
                    if (!bush.hasBerries) {
                        postToast("⏳ මෙම පඳුරේ පලතුරු තවම වැඩෙමින් පවතී...")
                        emitHudImmediate()
                        return
                    }
                    bush.hasBerries = false
                    bush.regrowTimer = 35f
                    berries += 2
                    score += 10f
                    sfx.play(SfxType.BERRY)
                    spawnParticles(hpPt.x, hpPt.y, hpPt.z, 0.96f, 0.25f, 0.37f, 7)
                    postToast("+2 🍒 පලතුරු කඩාගත්තා")
                }
                TargetType.WATER -> {
                    thirst = min(100f, thirst + 40f)
                    stamina = min(100f, stamina + 20f)
                    sfx.play(SfxType.DRINK)
                    spawnParticles(hpPt.x, hpPt.y, hpPt.z, 0.22f, 0.74f, 0.97f, 8)
                    postToast("💧 පිරිසිදු ජලය පානය කළා (+40 පිපාසය)")
                }
                TargetType.WOLF -> {
                    val wolf = wolves.getOrNull(hit.index) ?: return
                    val dmg = when (selectedSlot) {
                        2 -> 52f
                        1 -> 34f
                        3 -> 26f
                        else -> 14f
                    }
                    wolf.hp -= dmg
                    sfx.play(SfxType.HURT)
                    spawnParticles(hpPt.x, hpPt.y, hpPt.z, 0.94f, 0.27f, 0.27f, 10)

                    val dx = wolf.x - playerPos.x
                    val dz = wolf.z - playerPos.z
                    val len = hypot(dx, dz).coerceAtLeast(0.001f)
                    wolf.knockbackX = (dx / len) * 9f
                    wolf.knockbackZ = (dz / len) * 9f

                    if (wolf.hp <= 0f) {
                        wolves.removeAt(hit.index)
                        score += 150f
                        rawMeat += 1
                        postToast("🐺 වෘකයා පරාජය කළා! +1 🥩 අමු මස් (නිර්මාණ පොතෙන් පිසගන්න)")
                    } else {
                        postToast("⚔️ වෘකයාට පහර දුන්නා (-${dmg.toInt()} HP)")
                    }
                }
            }
        }
        emitHudImmediate()
    }

    fun step(rawDt: Float) {
        val dt = rawDt.coerceIn(0f, 0.05f)
        var shouldEmitNow = false

        synchronized(worldLock) {
            elapsedSec += dt
            if (gamePhase != GamePhase.PLAYING || isCraftingOpen || isPaused) {
                return
            }

            // 1. Day/Night Cycle
            timeOfDay += dt / DAY_LENGTH_SEC
            if (timeOfDay >= 1.0f) {
                timeOfDay -= 1.0f
                dayCount++
                score += 200f
                postToast("☀️ දිනය $dayCount උදා විය! (+200 ලකුණු)")
                shouldEmitNow = true
            }
            updateLighting()

            // 2. Movement & Physics
            val moveForward = (keyForward + (-joyY)).coerceIn(-1f, 1f)
            val moveRight = (keyRight + joyX).coerceIn(-1f, 1f)

            val fx = -sin(yaw)
            val fz = -cos(yaw)
            val rx = cos(yaw)
            val rz = -sin(yaw)

            var wishX = fx * moveForward + rx * moveRight
            var wishZ = fz * moveForward + rz * moveRight
            val wishLenSq = wishX * wishX + wishZ * wishZ
            isMovingNow = wishLenSq > 0.01f
            if (isMovingNow) {
                val len = sqrt(wishLenSq)
                wishX /= len
                wishZ /= len
            }

            val wantsSprint = (sprintToggle || keySprint) && isMovingNow && stamina > 5f
            val moveSpeed = if (wantsSprint) 10.2f else 6.2f

            if (wantsSprint) {
                stamina = max(0f, stamina - 22f * dt)
                if (stamina <= 1f) sprintToggle = false
            } else {
                stamina = min(100f, stamina + 14f * dt)
            }

            playerPos.x += wishX * moveSpeed * dt
            playerPos.z += wishZ * moveSpeed * dt

            // Gravity & Jump
            playerVelY -= 18f * dt
            playerPos.y += playerVelY * dt
            if (playerPos.y <= EYE_HEIGHT) {
                playerPos.y = EYE_HEIGHT
                playerVelY = 0f
                onGround = true
            }

            // Keep inside Island perimeter
            val distFromCenter = hypot(playerPos.x, playerPos.z)
            if (distFromCenter > ISLAND_RADIUS - 2f) {
                val ang = kotlin.math.atan2(playerPos.z, playerPos.x)
                playerPos.x = cos(ang) * (ISLAND_RADIUS - 2f)
                playerPos.z = sin(ang) * (ISLAND_RADIUS - 2f)
            }

            // Solid Obstacle Collision (Trees & Rocks)
            for (t in trees) {
                if (t.hp <= 0f) continue
                val dx = playerPos.x - t.x
                val dz = playerPos.z - t.z
                val d = hypot(dx, dz)
                val minD = t.radius + 0.45f
                if (d < minD && d > 0.001f) {
                    playerPos.x = t.x + (dx / d) * minD
                    playerPos.z = t.z + (dz / d) * minD
                }
            }
            for (r in rocks) {
                if (r.hp <= 0f) continue
                val dx = playerPos.x - r.x
                val dz = playerPos.z - r.z
                val d = hypot(dx, dz)
                val minD = r.radius + 0.4f
                if (d < minD && d > 0.001f) {
                    playerPos.x = r.x + (dx / d) * minD
                    playerPos.z = r.z + (dz / d) * minD
                }
            }

            // Head Bob & Footsteps
            if (isMovingNow && onGround) {
                bobTimer += dt * (if (wantsSprint) 13f else 9f)
                stepTimer -= dt * (if (wantsSprint) 1.5f else 1.0f)
                if (stepTimer <= 0f) {
                    sfx.play(SfxType.STEP)
                    stepTimer = 0.44f
                }
            }

            if (swingCooldown > 0f) swingCooldown -= dt
            if (swingTimer > 0f) swingTimer = max(0f, swingTimer - dt)

            // 3. Hand Torch Fuel & Sparks
            if (selectedSlot == 3 && torches > 0) {
                torchFuel -= 2.2f * dt
                if (Random.nextFloat() < 0.2f) {
                    val tx = playerPos.x + (-sin(yaw)) * 0.6f + cos(yaw) * 0.35f
                    val ty = playerPos.y - 0.1f
                    val tz = playerPos.z + (-cos(yaw)) * 0.6f + (-sin(yaw)) * 0.35f
                    spawnParticles(tx, ty, tz, 0.98f, 0.75f, 0.14f, 1)
                }
                if (torchFuel <= 0f) {
                    torches--
                    if (torches > 0) {
                        torchFuel = 100f
                    } else {
                        postToast("🔦 පන්දම නිවී ගියේය!")
                        selectedSlot = 0
                        shouldEmitNow = true
                    }
                }
            }

            // 4. Raycast Target Detection
            currentTarget = performRaycast()

            // 5. Campfire Warmth & Fuel
            var isNearFire = false
            var canRefuel = false
            val cfIter = campfires.listIterator()
            while (cfIter.hasNext()) {
                val cf = cfIter.next()
                cf.fuel -= 1.8f * dt
                if (Random.nextFloat() < 0.25f) {
                    spawnParticles(cf.x, 0.6f, cf.z, 0.98f, 0.75f, 0.14f, 1)
                }
                val distToPlayer = hypot(playerPos.x - cf.x, playerPos.z - cf.z)
                if (distToPlayer < 5.5f) isNearFire = true
                if (distToPlayer < 3.6f) canRefuel = true
                if (cf.fuel <= 0f) {
                    cfIter.remove()
                }
            }
            nearWarmFire = isNearFire
            nearCampfireRefuel = canRefuel

            if (isNearFire && hp < 100f) {
                hp = min(100f, hp + 3.0f * dt)
            }

            // 6. Resource Respawn Timers
            for (t in trees) {
                if (t.hp <= 0f) {
                    t.respawnTimer -= dt
                    if (t.respawnTimer <= 0f) t.hp = 100f
                }
            }
            for (r in rocks) {
                if (r.hp <= 0f) {
                    r.respawnTimer -= dt
                    if (r.respawnTimer <= 0f) r.hp = 100f
                }
            }
            for (b in bushes) {
                if (!b.hasBerries) {
                    b.regrowTimer -= dt
                    if (b.regrowTimer <= 0f) b.hasBerries = true
                }
            }

            // 7. Nighttime Wolves AI
            val nightNow = timeOfDay in 0.62f..0.92f
            if (nightNow) {
                val maxWolves = 2 + dayCount
                if (wolves.size < maxWolves && Random.nextFloat() < 0.35f * dt) {
                    spawnWolf()
                }
            } else if (wolves.isNotEmpty()) {
                wolves.clear()
            }

            val holdingTorch = (selectedSlot == 3 && torches > 0)
            for (w in wolves) {
                if (w.attackCooldown > 0f) w.attackCooldown -= dt

                // Apply knockback decay
                if (hypot(w.knockbackX, w.knockbackZ) > 0.05f) {
                    w.x += w.knockbackX * dt
                    w.z += w.knockbackZ * dt
                    w.knockbackX *= (1f - 7f * dt).coerceAtLeast(0f)
                    w.knockbackZ *= (1f - 7f * dt).coerceAtLeast(0f)
                }

                var targetX = playerPos.x
                var targetZ = playerPos.z
                var speedMult = 1.0f
                val distToPlayerNow = hypot(playerPos.x - w.x, playerPos.z - w.z)

                if (holdingTorch && distToPlayerNow < 5.2f) {
                    val awayX = w.x - playerPos.x
                    val awayZ = w.z - playerPos.z
                    targetX = w.x + awayX
                    targetZ = w.z + awayZ
                    speedMult = 0.7f
                } else {
                    for (cf in campfires) {
                        val dFire = hypot(w.x - cf.x, w.z - cf.z)
                        if (dFire < 6.0f) {
                            targetX = w.x + (w.x - cf.x)
                            targetZ = w.z + (w.z - cf.z)
                            speedMult = 0.75f
                            break
                        }
                    }
                }

                val dx = targetX - w.x
                val dz = targetZ - w.z
                val dist = hypot(dx, dz)
                if (dist > 1.65f) {
                    val nx = dx / dist
                    val nz = dz / dist
                    w.x += nx * w.speed * speedMult * dt
                    w.z += nz * w.speed * speedMult * dt
                    w.yaw = kotlin.math.atan2(nx, nz)
                    w.walkAnim += dt * 10f
                }

                val distAfter = hypot(playerPos.x - w.x, playerPos.z - w.z)
                if (distAfter < 2.1f && w.attackCooldown <= 0f) {
                    w.attackCooldown = 1.2f
                    val dmg = 14f + dayCount * 2f
                    hp -= dmg
                    sfx.play(SfxType.HURT)
                    damageFlashSeq++
                    postToast("🐺 වෘකයෙක් පහර දුන්නා! (-${dmg.toInt()} ❤️)")
                    shouldEmitNow = true

                    if (hp <= 0f) {
                        endGame("රාත්‍රී අඳුරේ පැමිණි වෘකයන්ගේ ප්‍රහාරයකින් මිය ගියේය.")
                        return@synchronized
                    }
                }
            }

            // 8. Particles
            val pIter = particles.listIterator()
            while (pIter.hasNext()) {
                val p = pIter.next()
                p.vy -= 9.8f * dt
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += p.vz * dt
                p.life -= dt
                if (p.life <= 0f) pIter.remove()
            }

            score += 2f * dt

            hudEmitAccum += dt
            if (hudEmitAccum >= 0.08f) {
                hudEmitAccum = 0f
                shouldEmitNow = true
            }
        }

        if (shouldEmitNow) {
            emitHudImmediate()
        }
    }

    private fun spawnWolf() {
        val ang = Random.nextFloat() * 2f * PI.toFloat()
        val dist = 26f + Random.nextFloat() * 16f
        val wx = (playerPos.x + cos(ang) * dist).coerceIn(-ISLAND_RADIUS + 8f, ISLAND_RADIUS - 8f)
        val wz = (playerPos.z + sin(ang) * dist).coerceIn(-ISLAND_RADIUS + 8f, ISLAND_RADIUS - 8f)
        wolves.add(
            WolfEntity(
                x = wx,
                z = wz,
                hp = 50f + dayCount * 15f,
                speed = 5.4f + min(2.5f, dayCount * 0.35f)
            )
        )
        sfx.play(SfxType.WOLF)
    }

    private fun spawnParticles(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, count: Int) {
        for (i in 0 until count) {
            particles.add(
                ParticleEntity(
                    x = x,
                    y = y,
                    z = z,
                    vx = (Random.nextFloat() - 0.5f) * 4.5f,
                    vy = 1.5f + Random.nextFloat() * 3.5f,
                    vz = (Random.nextFloat() - 0.5f) * 4.5f,
                    life = 0.45f + Random.nextFloat() * 0.25f,
                    r = r,
                    g = g,
                    b = b
                )
            )
        }
    }

    private fun updateLighting() {
        val dayR = 0.494f; val dayG = 0.784f; val dayB = 0.890f
        val setR = 0.851f; val setG = 0.467f; val setB = 0.024f
        val nightR = 0.020f; val nightG = 0.035f; val nightB = 0.078f

        when {
            timeOfDay < 0.50f -> {
                skyR = dayR; skyG = dayG; skyB = dayB
                sunIntensity = 1.15f; hemiIntensity = 0.75f; starOpacity = 0f
            }
            timeOfDay < 0.62f -> {
                val t = (timeOfDay - 0.50f) / 0.12f
                skyR = lerp(dayR, setR, t)
                skyG = lerp(dayG, setG, t)
                skyB = lerp(dayB, setB, t)
                sunIntensity = 1.15f - t * 0.6f
                hemiIntensity = 0.75f - t * 0.35f
                starOpacity = 0f
            }
            timeOfDay < 0.68f -> {
                val t = (timeOfDay - 0.62f) / 0.06f
                skyR = lerp(setR, nightR, t)
                skyG = lerp(setG, nightG, t)
                skyB = lerp(setB, nightB, t)
                sunIntensity = 0.55f - t * 0.42f
                hemiIntensity = 0.40f - t * 0.25f
                starOpacity = t
            }
            timeOfDay <= 0.90f -> {
                skyR = nightR; skyG = nightG; skyB = nightB
                sunIntensity = 0.14f; hemiIntensity = 0.16f; starOpacity = 0.95f
            }
            else -> {
                val t = (timeOfDay - 0.90f) / 0.10f
                skyR = lerp(nightR, dayR, t)
                skyG = lerp(nightG, dayG, t)
                skyB = lerp(nightB, dayB, t)
                sunIntensity = 0.14f + t * 1.01f
                hemiIntensity = 0.16f + t * 0.59f
                starOpacity = 1f - t
            }
        }

        val sunAngle = (timeOfDay - 0.25f) * 2f * PI.toFloat()
        val sx = cos(sunAngle) * 60f
        val sy = sin(sunAngle) * 70f + 25f
        val sz = 30f
        val slen = sqrt(sx * sx + sy * sy + sz * sz).coerceAtLeast(1f)
        sunDirX = sx / slen
        sunDirY = sy / slen
        sunDirZ = sz / slen
    }

    private fun performRaycast(): RayHit? {
        val bobY = if (isMovingNow) sin(bobTimer) * 0.055f else 0f
        val ox = playerPos.x
        val oy = playerPos.y + bobY
        val oz = playerPos.z

        val cp = cos(pitch)
        val dx = -sin(yaw) * cp
        val dy = sin(pitch)
        val dz = -cos(yaw) * cp

        var best: RayHit? = null
        var bestDist = RAY_MAX_DIST

        // 1. Trees (vertical cylinder approx)
        for (i in trees.indices) {
            val t = trees[i]
            if (t.hp <= 0f) continue
            val hitDist = intersectVerticalCylinder(ox, oy, oz, dx, dy, dz, t.x, t.z, 0.9f * t.scale, 0f, 4.5f * t.scale)
            if (hitDist != null && hitDist < bestDist) {
                bestDist = hitDist
                best = RayHit(
                    TargetType.TREE, i, hitDist,
                    Vec3(ox + dx * hitDist, oy + dy * hitDist, oz + dz * hitDist),
                    "ගස කපන්න (Chop Tree)"
                )
            }
        }

        // 2. Rocks (sphere)
        for (i in rocks.indices) {
            val r = rocks[i]
            if (r.hp <= 0f) continue
            val hitDist = intersectSphere(ox, oy, oz, dx, dy, dz, r.x, 0.65f * r.scale, r.z, 1.15f * r.scale)
            if (hitDist != null && hitDist < bestDist) {
                bestDist = hitDist
                best = RayHit(
                    TargetType.ROCK, i, hitDist,
                    Vec3(ox + dx * hitDist, oy + dy * hitDist, oz + dz * hitDist),
                    "ගල කඩන්න (Mine Stone)"
                )
            }
        }

        // 3. Bushes (sphere)
        for (i in bushes.indices) {
            val b = bushes[i]
            val hitDist = intersectSphere(ox, oy, oz, dx, dy, dz, b.x, 0.65f, b.z, 0.92f)
            if (hitDist != null && hitDist < bestDist) {
                bestDist = hitDist
                best = RayHit(
                    TargetType.BUSH, i, hitDist,
                    Vec3(ox + dx * hitDist, oy + dy * hitDist, oz + dz * hitDist),
                    "පලතුරු කඩන්න (Pick Berries)"
                )
            }
        }

        // 4. Pond water plane
        if (kotlin.math.abs(dy) > 0.001f) {
            val t = (POND_Y - oy) / dy
            if (t > 0.1f && t < bestDist) {
                val hx = ox + dx * t
                val hz = oz + dz * t
                if (hypot(hx - POND_X, hz - POND_Z) <= POND_RADIUS) {
                    bestDist = t
                    best = RayHit(
                        TargetType.WATER, 0, t,
                        Vec3(hx, POND_Y, hz),
                        "වතුර බොන්න (Drink Water)"
                    )
                }
            }
        }

        // 5. Wolves (sphere hitbox)
        for (i in wolves.indices) {
            val w = wolves[i]
            val hitDist = intersectSphere(ox, oy, oz, dx, dy, dz, w.x, 0.7f, w.z, 1.05f)
            if (hitDist != null && hitDist < bestDist) {
                bestDist = hitDist
                best = RayHit(
                    TargetType.WOLF, i, hitDist,
                    Vec3(ox + dx * hitDist, oy + dy * hitDist, oz + dz * hitDist),
                    "පහර දෙන්න (Attack Wolf)"
                )
            }
        }

        return best
    }

    private fun intersectSphere(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        cx: Float, cy: Float, cz: Float,
        radius: Float
    ): Float? {
        val fx = ox - cx
        val fy = oy - cy
        val fz = oz - cz
        val b = 2f * (fx * dx + fy * dy + fz * dz)
        val c = fx * fx + fy * fy + fz * fz - radius * radius
        val disc = b * b - 4f * c
        if (disc < 0f) return null
        val sqrtD = sqrt(disc)
        val t1 = (-b - sqrtD) * 0.5f
        if (t1 > 0.05f) return t1
        val t2 = (-b + sqrtD) * 0.5f
        if (t2 > 0.05f) return t2
        return null
    }

    private fun intersectVerticalCylinder(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        cx: Float, cz: Float, radius: Float,
        minY: Float, maxY: Float
    ): Float? {
        val fx = ox - cx
        val fz = oz - cz
        val a = dx * dx + dz * dz
        if (a < 0.0001f) return null
        val b = 2f * (fx * dx + fz * dz)
        val c = fx * fx + fz * fz - radius * radius
        val disc = b * b - 4f * a * c
        if (disc < 0f) return null
        val sqrtD = sqrt(disc)
        val t1 = (-b - sqrtD) / (2f * a)
        if (t1 > 0.05f) {
            val hy = oy + dy * t1
            if (hy in minY..maxY) return t1
        }
        val t2 = (-b + sqrtD) / (2f * a)
        if (t2 > 0.05f) {
            val hy = oy + dy * t2
            if (hy in minY..maxY) return t2
        }
        return null
    }

    fun updateVitalsFromViewModel(newHunger: Float, newThirst: Float) {
        val shouldEmit = synchronized(worldLock) {
            hunger = newHunger.coerceIn(0f, 100f)
            thirst = newThirst.coerceIn(0f, 100f)
            computeSurvivalStatus() != _hudState.value.survivalStatus
        }
        if (shouldEmit) {
            emitHudImmediate()
        }
    }

    fun triggerGameOver(reason: String) {
        synchronized(worldLock) {
            if (gamePhase == GamePhase.GAMEOVER) return
            endGame(reason)
        }
        emitHudImmediate()
    }

    private fun endGame(reason: String) {
        gamePhase = GamePhase.GAMEOVER
        isCraftingOpen = false
        isPaused = false
        deathReason = reason
    }

    private fun postToast(msg: String) {
        toastMessage = msg
        toastSeq++
    }

    private fun computeSurvivalStatus(): SurvivalStatus {
        return when {
            gamePhase == GamePhase.GAMEOVER || hp <= 0f -> SurvivalStatus.DEFEATED
            hp <= 30f || hunger <= 15f || thirst <= 15f -> SurvivalStatus.CRITICAL
            nearWarmFire && hp < 100f -> SurvivalStatus.WARM_BY_FIRE
            hunger <= 40f -> SurvivalStatus.HUNGRY
            thirst <= 40f -> SurvivalStatus.THIRSTY
            else -> SurvivalStatus.HEALTHY
        }
    }

    private fun emitHudImmediate() {
        val snap = synchronized(worldLock) {
            val mapPoints = ArrayList<MinimapPoint>(
                trees.size + rocks.size + bushes.size + campfires.size + wolves.size
            )
            for (t in trees) {
                if (t.hp > 0f) mapPoints.add(MinimapPoint(t.x, t.z, MinimapKind.TREE))
            }
            for (r in rocks) {
                if (r.hp > 0f) mapPoints.add(MinimapPoint(r.x, r.z, MinimapKind.ROCK))
            }
            for (b in bushes) {
                if (b.hasBerries) mapPoints.add(MinimapPoint(b.x, b.z, MinimapKind.BUSH))
            }
            for (cf in campfires) {
                mapPoints.add(MinimapPoint(cf.x, cf.z, MinimapKind.CAMPFIRE))
            }
            for (w in wolves) {
                mapPoints.add(MinimapPoint(w.x, w.z, MinimapKind.WOLF))
            }

            HudSnapshot(
                gamePhase = gamePhase,
                isCraftingOpen = isCraftingOpen,
                isPaused = isPaused,
                currentCraftTab = currentCraftTab,
                hp = hp,
                hunger = hunger,
                thirst = thirst,
                stamina = stamina,
                survivalStatus = computeSurvivalStatus(),
                wood = wood,
                stone = stone,
                berries = berries,
                rawMeat = rawMeat,
                torches = torches,
                torchFuel = torchFuel,
                bandages = bandages,
                hasAxe = hasAxe,
                hasSpear = hasSpear,
                selectedSlot = selectedSlot,
                score = score,
                dayCount = dayCount,
                timeOfDay = timeOfDay,
                yaw = yaw,
                playerX = playerPos.x,
                playerZ = playerPos.z,
                minimapPoints = mapPoints,
                targetPrompt = currentTarget?.label,
                nearCampfireForRefuel = nearCampfireRefuel,
                nearWarmFire = nearWarmFire,
                isSprinting = sprintToggle || keySprint,
                isMuted = sfx.isMuted,
                toastMessage = toastMessage,
                toastSeq = toastSeq,
                damageFlashSeq = damageFlashSeq,
                deathReason = deathReason
            )
        }
        _hudState.value = snap
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
