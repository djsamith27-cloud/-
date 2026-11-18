package com.example.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.game.SurvivalGameEngine
import com.example.gl.WildSurvivalRenderer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import kotlin.math.hypot

@Composable
fun GameViewport(
    engine: SurvivalGameEngine,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tapSlopPx = with(LocalDensity.current) { 12.dp.toPx() }

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(SafeEglConfigChooser())
            preserveEGLContextOnPause = true
            setRenderer(WildSurvivalRenderer(engine))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier
            .fillMaxSize()
            .pointerInput(engine, tapSlopPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val activeId = down.id
                    var lastPos = down.position
                    var totalMoved = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == activeId } ?: break
                        if (!change.pressed) {
                            if (!change.isConsumed && totalMoved < tapSlopPx) {
                                engine.performPrimaryAction()
                            }
                            change.consume()
                            break
                        }
                        if (!change.isConsumed) {
                            val delta = change.position - lastPos
                            lastPos = change.position
                            totalMoved += hypot(delta.x, delta.y)
                            if (delta.x != 0f || delta.y != 0f) {
                                engine.addLookDelta(delta.x, delta.y)
                                change.consume()
                            }
                        } else {
                            lastPos = change.position
                        }
                    }
                }
            }
    )
}

/**
 * Chooses an OpenGL ES 2.0 EGLConfig with safe fallbacks across physical devices
 * and virtualized SwiftShader emulators so GLThread never fails with "No config chosen".
 */
private class SafeEglConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val eglOpenglEs2Bit = 4
        val candidates = arrayOf(
            // Preferred: RGBA8888 + 16-bit depth
            intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_RENDERABLE_TYPE, eglOpenglEs2Bit,
                EGL10.EGL_NONE
            ),
            // Fallback 1: RGB888 + 16-bit depth
            intArrayOf(
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_RENDERABLE_TYPE, eglOpenglEs2Bit,
                EGL10.EGL_NONE
            ),
            // Fallback 2: RGB565 + 16-bit depth
            intArrayOf(
                EGL10.EGL_RED_SIZE, 5,
                EGL10.EGL_GREEN_SIZE, 6,
                EGL10.EGL_BLUE_SIZE, 5,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_RENDERABLE_TYPE, eglOpenglEs2Bit,
                EGL10.EGL_NONE
            ),
            // Fallback 3: Any ES2 config with depth >= 8
            intArrayOf(
                EGL10.EGL_DEPTH_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, eglOpenglEs2Bit,
                EGL10.EGL_NONE
            ),
            // Last resort: Any ES2 config
            intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, eglOpenglEs2Bit,
                EGL10.EGL_NONE
            )
        )

        val numConfigs = IntArray(1)
        for (spec in candidates) {
            if (egl.eglChooseConfig(display, spec, null, 0, numConfigs) && numConfigs[0] > 0) {
                val configs = arrayOfNulls<EGLConfig>(numConfigs[0])
                if (egl.eglChooseConfig(display, spec, configs, numConfigs[0], numConfigs)) {
                    val chosen = configs.firstOrNull { it != null }
                    if (chosen != null) return chosen
                }
            }
        }

        // Ultimate fallback: query all configs available on display
        if (egl.eglGetConfigs(display, null, 0, numConfigs) && numConfigs[0] > 0) {
            val all = arrayOfNulls<EGLConfig>(numConfigs[0])
            if (egl.eglGetConfigs(display, all, numConfigs[0], numConfigs)) {
                val chosen = all.firstOrNull { it != null }
                if (chosen != null) return chosen
            }
        }

        throw IllegalStateException("No EGLConfig available on display")
    }
}
