package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.GamePhase
import com.example.ui.CraftingBookModal
import com.example.ui.GameOverOverlay
import com.example.ui.GameViewModel
import com.example.ui.GameViewport
import com.example.ui.StartOverlay
import com.example.ui.SurvivalHud
import com.example.ui.theme.WildSurvivalTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private val pressedKeys = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Immersive landscape fullscreen
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            WildSurvivalTheme {
                val hud by viewModel.hudState.collectAsStateWithLifecycle()

                if (hud.isCraftingOpen) {
                    BackHandler {
                        viewModel.toggleCraftingBook(false)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    GameViewport(engine = viewModel.engine)

                    when (hud.gamePhase) {
                        GamePhase.START -> {
                            StartOverlay(onStartGame = { viewModel.startGame() })
                        }
                        GamePhase.PLAYING -> {
                            SurvivalHud(
                                hud = hud,
                                onJoystickMove = { x, y -> viewModel.setJoystick(x, y) },
                                onPrimaryAction = { viewModel.performPrimaryAction() },
                                onJump = { viewModel.jump() },
                                onToggleSprint = { viewModel.toggleSprint() },
                                onSelectSlot = { viewModel.selectHotbarSlot(it) },
                                onOpenCrafting = { viewModel.toggleCraftingBook(true) },
                                onRefuelCampfire = { viewModel.refuelCampfire() },
                                onToggleMute = { viewModel.toggleMute() }
                            )

                            if (hud.isCraftingOpen) {
                                CraftingBookModal(
                                    hud = hud,
                                    onSelectTab = { viewModel.setCraftTab(it) },
                                    onCraft = { viewModel.craftRecipe(it) },
                                    onClose = { viewModel.toggleCraftingBook(false) }
                                )
                            }
                        }
                        GamePhase.GAMEOVER -> {
                            GameOverOverlay(
                                hud = hud,
                                onRestart = { viewModel.startGame() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        pressedKeys.add(keyCode)
        updateKeyboardMovement()

        val hud = viewModel.hudState.value
        if (hud.gamePhase == GamePhase.PLAYING && event?.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_B -> {
                    viewModel.toggleCraftingBook()
                    return true
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    if (hud.isCraftingOpen) {
                        viewModel.toggleCraftingBook(false)
                        return true
                    }
                }
                KeyEvent.KEYCODE_SPACE -> {
                    viewModel.jump()
                    return true
                }
                KeyEvent.KEYCODE_E -> {
                    viewModel.refuelCampfire()
                    return true
                }
                KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_ENTER -> {
                    viewModel.performPrimaryAction()
                    return true
                }
                KeyEvent.KEYCODE_1 -> { viewModel.selectHotbarSlot(0); return true }
                KeyEvent.KEYCODE_2 -> { viewModel.selectHotbarSlot(1); return true }
                KeyEvent.KEYCODE_3 -> { viewModel.selectHotbarSlot(2); return true }
                KeyEvent.KEYCODE_4 -> { viewModel.selectHotbarSlot(3); return true }
                KeyEvent.KEYCODE_5 -> { viewModel.selectHotbarSlot(4); return true }
                KeyEvent.KEYCODE_6 -> { viewModel.selectHotbarSlot(5); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        pressedKeys.remove(keyCode)
        updateKeyboardMovement()
        return super.onKeyUp(keyCode, event)
    }

    private fun updateKeyboardMovement() {
        var fwd = 0f
        var right = 0f
        if (KeyEvent.KEYCODE_W in pressedKeys || KeyEvent.KEYCODE_DPAD_UP in pressedKeys) fwd += 1f
        if (KeyEvent.KEYCODE_S in pressedKeys || KeyEvent.KEYCODE_DPAD_DOWN in pressedKeys) fwd -= 1f
        if (KeyEvent.KEYCODE_D in pressedKeys || KeyEvent.KEYCODE_DPAD_RIGHT in pressedKeys) right += 1f
        if (KeyEvent.KEYCODE_A in pressedKeys || KeyEvent.KEYCODE_DPAD_LEFT in pressedKeys) right -= 1f
        viewModel.engine.setKeyMovement(fwd, right)
        viewModel.engine.setKeySprint(
            KeyEvent.KEYCODE_SHIFT_LEFT in pressedKeys || KeyEvent.KEYCODE_SHIFT_RIGHT in pressedKeys
        )
    }
}
