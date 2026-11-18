package com.example.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.GamePhase
import com.example.game.SurvivalGameEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class WildSurvivalRenderer(
    private val engine: SurvivalGameEngine
) : GLSurfaceView.Renderer {

    private lateinit var worldShader: WorldShaderProgram
    private lateinit var starShader: StarShaderProgram

    // World Meshes
    private lateinit var oceanMesh: GpuMesh
    private lateinit var islandMesh: GpuMesh
    private lateinit var beachRingMesh: GpuMesh
    private lateinit var pondShoreMesh: GpuMesh
    private lateinit var pondWaterMesh: GpuMesh
    private lateinit var trunkMesh: GpuMesh
    private lateinit var pineCone0: GpuMesh
    private lateinit var pineCone1: GpuMesh
    private lateinit var pineCone2: GpuMesh
    private lateinit var oakCrownMesh: GpuMesh
    private lateinit var rockMesh: GpuMesh
    private lateinit var bushMesh: GpuMesh
    private lateinit var berryMesh: GpuMesh
    private lateinit var fireStoneMesh: GpuMesh
    private lateinit var fireLogMesh: GpuMesh
    private lateinit var fireFlameMesh: GpuMesh
    private lateinit var wolfBodyMesh: GpuMesh
    private lateinit var wolfHeadMesh: GpuMesh
    private lateinit var wolfSnoutMesh: GpuMesh
    private lateinit var wolfEarMesh: GpuMesh
    private lateinit var wolfEyeMesh: GpuMesh
    private lateinit var wolfLegMesh: GpuMesh
    private lateinit var particleMesh: GpuMesh

    // Viewmodel Meshes
    private lateinit var forearmMesh: GpuMesh
    private lateinit var fistMesh: GpuMesh
    private lateinit var axeHandleMesh: GpuMesh
    private lateinit var axeBladeMesh: GpuMesh
    private lateinit var spearShaftMesh: GpuMesh
    private lateinit var spearTipMesh: GpuMesh
    private lateinit var torchStickMesh: GpuMesh
    private lateinit var torchHeadMesh: GpuMesh
    private lateinit var heldBerryMesh: GpuMesh
    private lateinit var heldBandageMesh: GpuMesh

    private var starVboId = 0

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val pointLightPos = FloatArray(6 * 3)
    private val pointLightColor = FloatArray(6 * 3)
    private val pointLightIntensity = FloatArray(6)

    private var lastFrameNs = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        worldShader = WorldShaderProgram()
        starShader = StarShaderProgram()

        // Upload World Meshes
        oceanMesh = MeshBuilder.upload(MeshBuilder.createPlaneXZ(550f, 550f))
        islandMesh = MeshBuilder.upload(
            MeshBuilder.createCylinder(
                SurvivalGameEngine.ISLAND_RADIUS,
                SurvivalGameEngine.ISLAND_RADIUS + 4f,
                1.2f,
                48
            )
        )
        beachRingMesh = MeshBuilder.upload(
            MeshBuilder.createRingXZ(
                SurvivalGameEngine.ISLAND_RADIUS - 3f,
                SurvivalGameEngine.ISLAND_RADIUS + 1.5f,
                48
            )
        )
        pondShoreMesh = MeshBuilder.upload(MeshBuilder.createCircleXZ(SurvivalGameEngine.POND_RADIUS + 1.2f, 32))
        pondWaterMesh = MeshBuilder.upload(MeshBuilder.createCircleXZ(SurvivalGameEngine.POND_RADIUS, 32))

        trunkMesh = MeshBuilder.upload(MeshBuilder.createCylinder(0.28f, 0.42f, 3.2f, 7))
        pineCone0 = MeshBuilder.upload(MeshBuilder.createCone(1.9f, 2.4f, 7))
        pineCone1 = MeshBuilder.upload(MeshBuilder.createCone(1.48f, 2.4f, 7))
        pineCone2 = MeshBuilder.upload(MeshBuilder.createCone(1.06f, 2.4f, 7))
        oakCrownMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(1.9f, 5, 7))

        rockMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(1.0f, 4, 6))
        bushMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(0.85f, 4, 6))
        berryMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(0.11f, 3, 5))

        fireStoneMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(0.22f, 3, 5))
        fireLogMesh = MeshBuilder.upload(MeshBuilder.createCylinder(0.09f, 0.1f, 1.1f, 6))
        fireFlameMesh = MeshBuilder.upload(MeshBuilder.createCone(0.35f, 0.85f, 6))

        wolfBodyMesh = MeshBuilder.upload(MeshBuilder.createBox(0.65f, 0.65f, 1.35f))
        wolfHeadMesh = MeshBuilder.upload(MeshBuilder.createBox(0.52f, 0.5f, 0.55f))
        wolfSnoutMesh = MeshBuilder.upload(MeshBuilder.createBox(0.28f, 0.24f, 0.35f))
        wolfEarMesh = MeshBuilder.upload(MeshBuilder.createCone(0.1f, 0.25f, 4))
        wolfEyeMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(0.055f, 3, 5))
        wolfLegMesh = MeshBuilder.upload(MeshBuilder.createBox(0.16f, 0.45f, 0.16f))

        particleMesh = MeshBuilder.upload(MeshBuilder.createBox(0.1f, 0.1f, 0.1f))

        // Upload Viewmodel Meshes
        forearmMesh = MeshBuilder.upload(MeshBuilder.createBox(0.14f, 0.14f, 0.45f))
        fistMesh = MeshBuilder.upload(MeshBuilder.createBox(0.15f, 0.15f, 0.16f))
        axeHandleMesh = MeshBuilder.upload(MeshBuilder.createCylinder(0.022f, 0.026f, 0.68f, 8))
        axeBladeMesh = MeshBuilder.upload(MeshBuilder.createBox(0.04f, 0.16f, 0.22f))
        spearShaftMesh = MeshBuilder.upload(MeshBuilder.createCylinder(0.018f, 0.02f, 1.35f, 8))
        spearTipMesh = MeshBuilder.upload(MeshBuilder.createCone(0.045f, 0.22f, 6))
        torchStickMesh = MeshBuilder.upload(MeshBuilder.createCylinder(0.024f, 0.028f, 0.55f, 8))
        torchHeadMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(0.055f, 4, 6))
        heldBerryMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(0.065f, 4, 6))
        heldBandageMesh = MeshBuilder.upload(MeshBuilder.createCylinder(0.06f, 0.06f, 0.11f, 10))

        // Upload Starfield VBO
        val stars = engine.starPositions
        val sBuf = IntArray(1)
        GLES20.glGenBuffers(1, sBuf, 0)
        starVboId = sBuf[0]
        val fb: FloatBuffer = ByteBuffer.allocateDirect(stars.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(stars)
        fb.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, starVboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, stars.size * 4, fb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 72f, aspect, 0.1f, 250f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val dt = ((nowNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        lastFrameNs = nowNs

        engine.step(dt)

        synchronized(engine.worldLock) {
            renderSceneLocked()
        }
    }

    private fun renderSceneLocked() {
        val skyR = engine.skyR
        val skyG = engine.skyG
        val skyB = engine.skyB

        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val bobY = if (engine.isMovingNow) sin(engine.bobTimer) * 0.055f else 0f
        val bobX = if (engine.isMovingNow) cos(engine.bobTimer * 0.5f) * 0.035f else 0f
        val camX = engine.playerPos.x
        val camY = engine.playerPos.y + bobY
        val camZ = engine.playerPos.z

        // View Matrix: Rx(-pitch) * Ry(-yaw) * T(-camPos)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.rotateM(viewMatrix, 0, -Math.toDegrees(engine.pitch.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(viewMatrix, 0, -Math.toDegrees(engine.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.translateM(viewMatrix, 0, -camX, -camY, -camZ)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Starfield at Night
        if (engine.starOpacity > 0.02f) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDepthMask(false)
            GLES20.glUseProgram(starShader.programId)
            val starView = FloatArray(16)
            Matrix.setIdentityM(starView, 0)
            Matrix.rotateM(starView, 0, -Math.toDegrees(engine.pitch.toDouble()).toFloat(), 1f, 0f, 0f)
            Matrix.rotateM(starView, 0, -Math.toDegrees(engine.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
            val starMvp = FloatArray(16)
            Matrix.multiplyMM(starMvp, 0, projMatrix, 0, starView, 0)
            GLES20.glUniformMatrix4fv(starShader.uMVPMatrix, 1, false, starMvp, 0)
            GLES20.glUniform1f(starShader.uOpacity, engine.starOpacity)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, starVboId)
            GLES20.glEnableVertexAttribArray(starShader.aPosition)
            GLES20.glVertexAttribPointer(starShader.aPosition, 3, GLES20.GL_FLOAT, false, 12, 0)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 700)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glDepthMask(true)
            GLES20.glDisable(GLES20.GL_BLEND)
        }

        // 2. Setup World Shader & Dynamic Point Lights
        GLES20.glUseProgram(worldShader.programId)
        GLES20.glUniform3f(worldShader.uSunDir, engine.sunDirX, engine.sunDirY, engine.sunDirZ)
        GLES20.glUniform1f(worldShader.uSunIntensity, engine.sunIntensity)
        GLES20.glUniform1f(worldShader.uHemiIntensity, engine.hemiIntensity)
        GLES20.glUniform3f(worldShader.uSkyColor, skyR, skyG, skyB)
        GLES20.glUniform1f(worldShader.uFogDensity, 0.014f)
        GLES20.glUniform3f(worldShader.uCameraPos, camX, camY, camZ)

        var lightCount = 0
        val tSec = engine.elapsedSec
        // Hand Torch Point Light
        if (engine.selectedSlot == 3 && engine.torches > 0) {
            pointLightPos[0] = camX + (-sin(engine.yaw)) * 0.7f
            pointLightPos[1] = camY + 0.1f
            pointLightPos[2] = camZ + (-cos(engine.yaw)) * 0.7f
            pointLightColor[0] = 1.0f; pointLightColor[1] = 0.55f; pointLightColor[2] = 0.12f
            pointLightIntensity[0] = 2.1f + sin(tSec * 25f) * 0.45f
            lightCount++
        }
        // Nearest Campfire Point Lights (up to total 6)
        for (i in engine.campfires.indices) {
            if (lightCount >= 6) break
            val cf = engine.campfires[i]
            val idx = lightCount
            pointLightPos[idx * 3] = cf.x
            pointLightPos[idx * 3 + 1] = 1.3f
            pointLightPos[idx * 3 + 2] = cf.z
            pointLightColor[idx * 3] = 1.0f
            pointLightColor[idx * 3 + 1] = 0.48f
            pointLightColor[idx * 3 + 2] = 0.05f
            pointLightIntensity[idx] = (1.8f + sin(tSec * 19f + i * 1.7f) * 0.4f) * (cf.fuel / 100f)
            lightCount++
        }
        GLES20.glUniform1i(worldShader.uPointLightCount, lightCount)
        if (lightCount > 0) {
            GLES20.glUniform3fv(worldShader.uPointLightPos, lightCount, pointLightPos, 0)
            GLES20.glUniform3fv(worldShader.uPointLightColor, lightCount, pointLightColor, 0)
            GLES20.glUniform1fv(worldShader.uPointLightIntensity, lightCount, pointLightIntensity, 0)
        }

        // 3. Draw Terrain & Water
        // Ocean
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.35f, 0f)
        drawMesh(oceanMesh, 0.012f, 0.412f, 0.631f, 0.05f)

        // Island Grass Disc
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.6f, 0f)
        drawMesh(islandMesh, 0.141f, 0.333f, 0.165f)

        // Sandy Beach Rim
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.02f, 0f)
        drawMesh(beachRingMesh, 0.839f, 0.722f, 0.486f)

        // Pond Shore & Glowing Water
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, SurvivalGameEngine.POND_X, 0.03f, SurvivalGameEngine.POND_Z)
        drawMesh(pondShoreMesh, 0.839f, 0.722f, 0.486f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, SurvivalGameEngine.POND_X, SurvivalGameEngine.POND_Y, SurvivalGameEngine.POND_Z)
        drawMesh(pondWaterMesh, 0.055f, 0.647f, 0.914f, 0.32f)

        // 4. Draw Trees
        for (t in engine.trees) {
            if (t.hp <= 0f) continue
            // Trunk
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, t.x, 1.6f * t.scale, t.z)
            Matrix.scaleM(modelMatrix, 0, t.scale, t.scale, t.scale)
            drawMesh(trunkMesh, 0.361f, 0.227f, 0.129f)

            if (t.isPine) {
                for (tier in 0..2) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, t.x, (3.2f + tier * 1.3f) * t.scale, t.z)
                    Matrix.scaleM(modelMatrix, 0, t.scale, t.scale, t.scale)
                    val cone = when (tier) {
                        0 -> pineCone0
                        1 -> pineCone1
                        else -> pineCone2
                    }
                    drawMesh(cone, 0.078f, 0.325f, 0.176f)
                }
            } else {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, t.x, 4.0f * t.scale, t.z)
                Matrix.scaleM(modelMatrix, 0, t.scale, t.scale, t.scale)
                drawMesh(oakCrownMesh, 0.086f, 0.396f, 0.204f)
            }
        }

        // 5. Draw Rocks
        for (r in engine.rocks) {
            if (r.hp <= 0f) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, r.x, 0.65f * r.scale, r.z)
            Matrix.rotateM(modelMatrix, 0, Math.toDegrees(r.rotY.toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, Math.toDegrees(r.rotX.toDouble()).toFloat(), 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, r.scale, r.scale, r.scale)
            drawMesh(rockMesh, 0.420f, 0.447f, 0.502f)
        }

        // 6. Draw Berry Bushes
        val berryOffsets = arrayOf(
            floatArrayOf(0.5f, 0.8f, 0.4f),
            floatArrayOf(-0.5f, 0.75f, 0.45f),
            floatArrayOf(0.1f, 1.1f, -0.5f),
            floatArrayOf(-0.35f, 0.9f, -0.45f),
            floatArrayOf(0.4f, 0.6f, -0.4f)
        )
        for (b in engine.bushes) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, b.x, 0.65f, b.z)
            drawMesh(bushMesh, 0.082f, 0.502f, 0.239f)

            if (b.hasBerries) {
                for (off in berryOffsets) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, b.x + off[0], off[1], b.z + off[2])
                    drawMesh(berryMesh, 0.957f, 0.247f, 0.369f, 0.28f)
                }
            }
        }

        // 7. Draw Campfires
        for (i in engine.campfires.indices) {
            val cf = engine.campfires[i]
            for (s in 0 until 8) {
                val a = (s / 8f) * 2f * PI.toFloat()
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cf.x + cos(a) * 0.85f, 0.12f, cf.z + sin(a) * 0.85f)
                drawMesh(fireStoneMesh, 0.341f, 0.325f, 0.306f)
            }
            for (l in 0 until 3) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cf.x, 0.22f, cf.z)
                Matrix.rotateM(modelMatrix, 0, (l / 3f) * 360f, 0f, 1f, 0f)
                Matrix.rotateM(modelMatrix, 0, 63f, 0f, 0f, 1f)
                drawMesh(fireLogMesh, 0.271f, 0.102f, 0.012f)
            }
            val flicker = 0.85f + sin(tSec * 20f + i) * 0.2f
            val flickerH = 0.85f + abs(cos(tSec * 27f + i)) * 0.4f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cf.x, 0.48f, cf.z)
            Matrix.scaleM(modelMatrix, 0, flicker, flickerH, flicker)
            drawMesh(fireFlameMesh, 0.976f, 0.451f, 0.086f, 0.85f)
        }

        // 8. Draw Nighttime Wolves
        val legPositions = arrayOf(
            floatArrayOf(-0.22f, 0.22f, 0.45f, 1f),
            floatArrayOf(0.22f, 0.22f, 0.45f, -1f),
            floatArrayOf(-0.22f, 0.22f, -0.45f, -1f),
            floatArrayOf(0.22f, 0.22f, -0.45f, 1f)
        )
        for (w in engine.wolves) {
            val yawDeg = Math.toDegrees(w.yaw.toDouble()).toFloat()

            // Body
            setWolfTransform(w.x, 0.65f, w.z, yawDeg, 0f, 0f, 0f)
            drawMesh(wolfBodyMesh, 0.094f, 0.094f, 0.106f)

            // Head
            setWolfTransform(w.x, 0.92f, w.z, yawDeg, 0f, 0f, 0.75f)
            drawMesh(wolfHeadMesh, 0.094f, 0.094f, 0.106f)

            // Snout
            setWolfTransform(w.x, 0.82f, w.z, yawDeg, 0f, 0f, 1.12f)
            drawMesh(wolfSnoutMesh, 0.094f, 0.094f, 0.106f)

            // Ears
            setWolfTransform(w.x, 1.25f, w.z, yawDeg, -0.16f, 0f, 0.68f)
            drawMesh(wolfEarMesh, 0.094f, 0.094f, 0.106f)
            setWolfTransform(w.x, 1.25f, w.z, yawDeg, 0.16f, 0f, 0.68f)
            drawMesh(wolfEarMesh, 0.094f, 0.094f, 0.106f)

            // Glowing Red Eyes
            setWolfTransform(w.x, 0.98f, w.z, yawDeg, -0.14f, 0f, 1.03f)
            drawMesh(wolfEyeMesh, 0.937f, 0.267f, 0.267f, 0.95f)
            setWolfTransform(w.x, 0.98f, w.z, yawDeg, 0.14f, 0f, 1.03f)
            drawMesh(wolfEyeMesh, 0.937f, 0.267f, 0.267f, 0.95f)

            // 4 Animated Legs
            for (lp in legPositions) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, w.x, 0f, w.z)
                Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, lp[0], lp[1], lp[2])
                Matrix.rotateM(modelMatrix, 0, sin(w.walkAnim) * 24f * lp[3], 1f, 0f, 0f)
                drawMesh(wolfLegMesh, 0.094f, 0.094f, 0.106f)
            }
        }

        // 9. Draw 3D Particles
        for (p in engine.particles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            drawMesh(particleMesh, p.r, p.g, p.b, 0.3f)
        }

        // 10. Draw First-Person Viewmodel (Camera Space)
        if (engine.gamePhase == GamePhase.PLAYING) {
            renderViewmodelLocked(bobX, bobY)
        }
    }

    private fun renderViewmodelLocked(bobX: Float, bobY: Float) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        // Camera-space VP is just Projection Matrix (View = Identity)
        System.arraycopy(projMatrix, 0, vpMatrix, 0, 16)
        GLES20.glUniform3f(worldShader.uCameraPos, 0f, 0f, 0f)
        GLES20.glUniform1f(worldShader.uFogDensity, 0f)

        val baseArm = FloatArray(16)
        Matrix.setIdentityM(baseArm, 0)
        Matrix.translateM(baseArm, 0, 0.36f + bobX * 0.6f, -0.32f + abs(bobY) * 0.6f, -0.45f)

        if (engine.swingTimer > 0f) {
            val swingProgress = 1f - (engine.swingTimer / 0.24f).coerceIn(0f, 1f)
            val swingArc = sin(swingProgress * PI.toFloat())
            Matrix.translateM(baseArm, 0, 0f, 0f, -swingArc * 0.22f)
            Matrix.rotateM(baseArm, 0, Math.toDegrees((swingArc * 0.35f).toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(baseArm, 0, Math.toDegrees((-swingArc * 1.15f).toDouble()).toFloat(), 1f, 0f, 0f)
        }

        // Forearm (blue sleeve)
        System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0f, -0.02f, 0.18f)
        drawMesh(forearmMesh, 0.114f, 0.306f, 0.847f)

        // Fist (skin)
        System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -0.08f)
        drawMesh(fistMesh, 0.851f, 0.608f, 0.400f)

        when {
            engine.selectedSlot == 1 && engine.hasAxe -> {
                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.16f, -0.14f)
                Matrix.rotateM(modelMatrix, 0, 27f, 1f, 0f, 0f)
                drawMesh(axeHandleMesh, 0.471f, 0.208f, 0.059f)

                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.42f, -0.25f)
                Matrix.rotateM(modelMatrix, 0, 27f, 1f, 0f, 0f)
                drawMesh(axeBladeMesh, 0.612f, 0.639f, 0.686f)
            }
            engine.selectedSlot == 2 && engine.hasSpear -> {
                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.02f, -0.35f)
                Matrix.rotateM(modelMatrix, 0, 86.4f, 1f, 0f, 0f)
                drawMesh(spearShaftMesh, 0.471f, 0.208f, 0.059f)

                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.05f, -1.08f)
                Matrix.rotateM(modelMatrix, 0, -93.6f, 1f, 0f, 0f)
                drawMesh(spearTipMesh, 0.612f, 0.639f, 0.686f)
            }
            engine.selectedSlot == 3 && engine.torches > 0 -> {
                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.14f, -0.12f)
                Matrix.rotateM(modelMatrix, 0, 21.6f, 1f, 0f, 0f)
                drawMesh(torchStickMesh, 0.471f, 0.208f, 0.059f)

                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.41f, -0.22f)
                drawMesh(torchHeadMesh, 1.0f, 0.596f, 0.0f, 0.9f)
            }
            engine.selectedSlot == 4 && engine.berries > 0 -> {
                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.08f, -0.12f)
                drawMesh(heldBerryMesh, 0.957f, 0.247f, 0.369f, 0.2f)
            }
            engine.selectedSlot == 5 && engine.bandages > 0 -> {
                System.arraycopy(baseArm, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.08f, -0.12f)
                Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
                drawMesh(heldBandageMesh, 0.655f, 0.953f, 0.816f)
            }
        }
    }

    private fun setWolfTransform(
        wx: Float, wy: Float, wz: Float,
        yawDeg: Float,
        ox: Float, oy: Float, oz: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, wx, 0f, wz)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, ox, wy + oy, oz)
    }

    private fun drawMesh(mesh: GpuMesh, r: Float, g: Float, b: Float, emissive: Float = 0f) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uMVPMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(worldShader.uColor, r, g, b)
        GLES20.glUniform1f(worldShader.uEmissive, emissive)
        mesh.draw(worldShader.aPosition, worldShader.aNormal)
    }
}
