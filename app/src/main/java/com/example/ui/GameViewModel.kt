package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.audio.SoundSynthesizer
import com.example.game.CraftCategory
import com.example.game.HudSnapshot
import com.example.game.SurvivalGameEngine
import kotlinx.coroutines.flow.StateFlow

class GameViewModel : ViewModel() {
    val soundSynthesizer = SoundSynthesizer()
    val engine = SurvivalGameEngine(soundSynthesizer)
    val hudState: StateFlow<HudSnapshot> = engine.hudState

    fun startGame() = engine.startGame()
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
}
