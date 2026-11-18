package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynthesizer
import com.example.game.CraftCategory
import com.example.game.GamePhase
import com.example.game.HudSnapshot
import com.example.game.SurvivalGameEngine
import com.example.game.SurvivalStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    companion object {
        const val HUNGER_DECAY_PER_SEC = 1.15f
        const val THIRST_DECAY_PER_SEC = 1.55f
    }

    val soundSynthesizer = SoundSynthesizer()
    val engine = SurvivalGameEngine(soundSynthesizer)
    val hudState: StateFlow<HudSnapshot> = engine.hudState

    // Compose State properties managed directly by the ViewModel game loop
    var playerHealth by mutableFloatStateOf(100f)
        private set
    var playerHunger by mutableFloatStateOf(100f)
        private set
    var playerThirst by mutableFloatStateOf(100f)
        private set
    var survivalStatus by mutableStateOf(SurvivalStatus.HEALTHY)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var isLoopRunning by mutableStateOf(false)
        private set

    private var gameLoopJob: Job? = null

    init {
        startGameLoop()
    }

    fun startGameLoop() {
        if (gameLoopJob?.isActive == true) return
        isLoopRunning = true
        gameLoopJob = viewModelScope.launch {
            var lastTickNs = System.nanoTime()
            while (isActive) {
                val nowNs = System.nanoTime()
                val dt = ((nowNs - lastTickNs) / 1_000_000_000.0)
                    .toFloat()
                    .coerceIn(0.001f, 0.05f)
                lastTickNs = nowNs

                engine.step(dt)
                updateHungerAndThirstDecay(dt)
                syncComposeState()

                delay(16L)
            }
        }
    }

    /**
     * Decreases player hunger and thirst over time during active gameplay and
     * triggers a Game Over state immediately if either stat reaches zero.
     */
    fun updateHungerAndThirstDecay(dt: Float) {
        val snap = hudState.value
        if (snap.gamePhase != GamePhase.PLAYING || snap.isCraftingOpen || snap.isPaused) {
            return
        }

        val sprintMult = if (snap.isSprinting) 1.35f else 1.0f
        val nextHunger = (engine.hunger - HUNGER_DECAY_PER_SEC * sprintMult * dt).coerceAtLeast(0f)
        val nextThirst = (engine.thirst - THIRST_DECAY_PER_SEC * sprintMult * dt).coerceAtLeast(0f)

        engine.updateVitalsFromViewModel(nextHunger, nextThirst)
        playerHunger = nextHunger
        playerThirst = nextThirst

        if (nextHunger <= 0f) {
            engine.triggerGameOver("කුසගින්න අවසන් වීම නිසා ජීවිතය අහිමි විය.")
        } else if (nextThirst <= 0f) {
            engine.triggerGameOver("පිපාසය අවසන් වීම නිසා ජීවිතය අහිමි විය.")
        }
    }

    fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        isLoopRunning = false
    }

    fun startGame() {
        engine.startGame()
        startGameLoop()
        syncComposeState()
    }

    fun pauseGame() {
        engine.setPaused(true)
        syncComposeState()
    }

    fun resumeGame() {
        engine.setPaused(false)
        startGameLoop()
        syncComposeState()
    }

    fun togglePause() {
        engine.setPaused(!engine.isPaused)
        syncComposeState()
    }

    fun selectHotbarSlot(slot: Int) = engine.selectHotbarSlot(slot)
    fun toggleCraftingBook(force: Boolean? = null) = engine.toggleCraftingBook(force)
    fun setCraftTab(tab: CraftCategory) = engine.setCraftTab(tab)
    fun craftRecipe(recipeId: String) = engine.executeCraft(recipeId)
    fun refuelCampfire() = engine.refuelNearbyCampfire()
    fun performPrimaryAction() = engine.performPrimaryAction()
    fun jump() = engine.jump()
    fun toggleSprint() = engine.toggleSprint()
    fun toggleMute() = engine.toggleMute()
    fun setJoystick(x: Float, y: Float) = engine.setJoystick(x, y)

    fun isRecipeOwned(id: String): Boolean = engine.isRecipeOwned(id)

    private fun syncComposeState() {
        val snap = hudState.value
        playerHealth = snap.hp
        playerHunger = engine.hunger
        playerThirst = engine.thirst
        survivalStatus = snap.survivalStatus
        isGameOver = snap.gamePhase == GamePhase.GAMEOVER
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
