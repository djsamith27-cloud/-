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
    private lateinit var celestialMesh: GpuMesh
    private lateinit var cloudMesh: GpuMesh
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

    // Pre-allocated matrices & arrays (zero per-frame GC allocations)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val starViewMatrix = FloatArray(16)
    private val starMvpMatrix = FloatArray(16)
    private val baseArmMatrix = FloatArray(16)

    private val pointLightPos = FloatArray(6 * 3)
    private val pointLightColor = FloatArray(6 * 3)
    private val pointLightIntensity = FloatArray(6)

    private data class CloudInfo(val x: Float, val y: Float, val z: Float, val sx: Float, val sy: Float, val sz: Float, val speed: Float)
    private val clouds = listOf(
        CloudInfo(-55f, 36f, -45f, 9.5f, 2.2f, 5.2f, 0.9f),
        CloudInfo(20f, 40f, -68f, 12.0f, 2.5f, 6.0f, 0.7f),
        CloudInfo(62f, 35f, 12f, 8.5f, 2.0f, 4.8f, 1.1f),
        CloudInfo(-38f, 42f, 52f, 11.0f, 2.4f, 5.5f, 0.8f),
        CloudInfo(5f, 38f, 28f, 7.8f, 1.9f, 4.4f, 1.0f)
    )

    private val berryOffsets = arrayOf(
        floatArrayOf(0.5f, 0.8f, 0.4f),
        floatArrayOf(-0.5f, 0.75f, 0.45f),
        floatArrayOf(0.1f, 1.1f, -0.5f),
        floatArrayOf(-0.35f, 0.9f, -0.45f),
        floatArrayOf(0.4f, 0.6f, -0.4f)
    )
    private val legPositions = arrayOf(
        floatArrayOf(-0.22f, 0.22f, 0.45f, 1f),
        floatArrayOf(0.22f, 0.22f, 0.45f, -1f),
        floatArrayOf(-0.22f, 0.22f, -0.45f, -1f),
        floatArrayOf(0.22f, 0.22f, -0.45f, 1f)
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        worldShader = WorldShaderProgram()
        starShader = StarShaderProgram()

        // Upload World Meshes
        oceanMesh = MeshBuilder.upload(MeshBuilder.createPlaneXZ(560f, 560f))
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
        celestialMesh = MeshBuilder.upload(MeshBuilder.createFacetedSphere(5.5f, 6, 10))
        cloudMesh = MeshBuilder.upload(MeshBuilder.createBox(1f, 1f, 1f))

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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        Matrix.perspectiveM(projMatrix, 0, 72f, aspect, 0.1f, 260f)
    }

    override fun onDrawFrame(gl: GL10?) {
        synchronized(engine.worldLock) {
            renderSceneLocked()
        }
    }

    private fun isVisibleRange(x: Float, z: Float, camX: Float, camZ: Float, fwdX: Float, fwdZ: Float, maxDistSq: Float): Boolean {
        val dx = x - camX
        val dz = z - camZ
        val dSq = dx * dx + dz * dz
        if (dSq > maxDistSq) return false
        if (dSq < 49f) return true // Always draw nearby objects
        return (dx * fwdX + dz * fwdZ) > -7.5f
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
        val camYaw = if (engine.gamePhase == GamePhase.START) {
            sin(engine.elapsedSec * 0.18f) * 0.35f
        } else {
            engine.yaw
        }
        val camPitch = if (engine.gamePhase == GamePhase.START) -0.08f else engine.pitch
        val fwdX = -sin(camYaw)
        val fwdZ = -cos(camYaw)

        // View Matrix: Rx(-pitch) * Ry(-yaw) * T(-camPos)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.rotateM(viewMatrix, 0, -Math.toDegrees(camPitch.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(viewMatrix, 0, -Math.toDegrees(camYaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.translateM(viewMatrix, 0, -camX, -camY, -camZ)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        if (!worldShader.isValid) return

        // 1. Starfield at Night
        if (engine.starOpacity > 0.02f && starShader.isValid && starShader.aPosition >= 0) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDepthMask(false)
            GLES20.glUseProgram(starShader.programId)
            Matrix.setIdentityM(starViewMatrix, 0)
            Matrix.rotateM(starViewMatrix, 0, -Math.toDegrees(camPitch.toDouble()).toFloat(), 1f, 0f, 0f)
            Matrix.rotateM(starViewMatrix, 0, -Math.toDegrees(camYaw.toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.multiplyMM(starMvpMatrix, 0, projMatrix, 0, starViewMatrix, 0)
            GLES20.glUniformMatrix4fv(starShader.uMVPMatrix, 1, false, starMvpMatrix, 0)
            GLES20.glUniform1f(starShader.uOpacity, engine.starOpacity)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, starVboId)
            GLES20.glEnableVertexAttribArray(starShader.aPosition)
            GLES20.glVertexAttribPointer(starShader.aPosition, 3, GLES20.GL_FLOAT, false, 12, 0)
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 700)
            GLES20.glDisableVertexAttribArray(starShader.aPosition)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glDepthMask(true)
            GLES20.glDisable(GLES20.GL_BLEND)
        }

        // 2. Setup World Shader & Dynamic Point Lights
        val tSec = engine.elapsedSec
        GLES20.glUseProgram(worldShader.programId)
        GLES20.glUniform3f(worldShader.uSunDir, engine.sunDirX, engine.sunDirY, engine.sunDirZ)
        GLES20.glUniform1f(worldShader.uSunIntensity, engine.sunIntensity)
        GLES20.glUniform1f(worldShader.uHemiIntensity, engine.hemiIntensity)
        GLES20.glUniform3f(worldShader.uSkyColor, skyR, skyG, skyB)
        GLES20.glUniform1f(worldShader.uFogDensity, 0.0115f)
        GLES20.glUniform3f(worldShader.uCameraPos, camX, camY, camZ)
        if (worldShader.uTime >= 0) GLES20.glUniform1f(worldShader.uTime, tSec)

        for (i in 0 until 6) {
            pointLightIntensity[i] = 0f
        }
        var lightCount = 0
        if (engine.selectedSlot == 3 && engine.torches > 0) {
            pointLightPos[0] = camX + (-sin(engine.yaw)) * 0.7f
            pointLightPos[1] = camY + 0.1f
            pointLightPos[2] = camZ + (-cos(engine.yaw)) * 0.7f
            pointLightColor[0] = 1.0f; pointLightColor[1] = 0.55f; pointLightColor[2] = 0.12f
            pointLightIntensity[0] = 2.2f + sin(tSec * 25f) * 0.45f
            lightCount++
        }
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
            pointLightIntensity[idx] = (1.9f + sin(tSec * 19f + i * 1.7f) * 0.4f) * (cf.fuel / 100f)
            lightCount++
        }
        if (worldShader.uPointLightPos >= 0) {
            GLES20.glUniform3fv(worldShader.uPointLightPos, 6, pointLightPos, 0)
        }
        if (worldShader.uPointLightColor >= 0) {
            GLES20.glUniform3fv(worldShader.uPointLightColor, 6, pointLightColor, 0)
        }
        if (worldShader.uPointLightIntensity >= 0) {
            GLES20.glUniform1fv(worldShader.uPointLightIntensity, 6, pointLightIntensity, 0)
        }

        // 2b. Celestial Sun / Moon & Drifting Sky Clouds
        val isNight = engine.timeOfDay in 0.74f..0.92f
        val bodyDist = 145f
        val bodyX = camX + (if (isNight) -engine.sunDirX else engine.sunDirX) * bodyDist
        val bodyY = (if (isNight) 58f else (engine.sunDirY * bodyDist).coerceAtLeast(18f))
        val bodyZ = camZ + (if (isNight) -engine.sunDirZ else engine.sunDirZ) * bodyDist
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, bodyX, bodyY, bodyZ)
        if (isNight) {
            drawMesh(celestialMesh, 0.85f, 0.90f, 1.0f, emissive = 0.95f)
        } else {
            drawMesh(celestialMesh, 1.0f, 0.93f, 0.62f, emissive = 1.05f)
        }

        val cloudR = (skyR * 0.45f + 0.55f).coerceIn(0.22f, 0.98f)
        val cloudG = (skyG * 0.45f + 0.55f).coerceIn(0.24f, 0.98f)
        val cloudB = (skyB * 0.45f + 0.55f).coerceIn(0.28f, 1.0f)
        for (c in clouds) {
            val wrappedX = ((c.x + tSec * c.speed + 110f) % 220f) - 110f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, wrappedX, c.y, c.z)
            Matrix.scaleM(modelMatrix, 0, c.sx, c.sy, c.sz)
            drawMesh(cloudMesh, cloudR, cloudG, cloudB, emissive = 0.25f)
        }

        // 3. Draw Terrain & Animated Specular Water
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.35f, 0f)
        drawMesh(oceanMesh, 0.015f, 0.425f, 0.660f, emissive = 0.06f, specular = 0.85f, detail = 0.04f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.6f, 0f)
        drawMesh(islandMesh, 0.145f, 0.365f, 0.175f, detail = 0.09f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.02f, 0f)
        drawMesh(beachRingMesh, 0.855f, 0.742f, 0.510f, detail = 0.07f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, SurvivalGameEngine.POND_X, 0.03f, SurvivalGameEngine.POND_Z)
        drawMesh(pondShoreMesh, 0.855f, 0.742f, 0.510f, detail = 0.06f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, SurvivalGameEngine.POND_X, SurvivalGameEngine.POND_Y, SurvivalGameEngine.POND_Z)
        drawMesh(pondWaterMesh, 0.065f, 0.675f, 0.935f, emissive = 0.34f, specular = 0.95f, detail = 0.05f)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        // 4. Draw Trees (distance + frustum culled)
        val maxDrawDistSq = 115f * 115f
        for (t in engine.trees) {
            if (t.hp <= 0f) continue
            if (!isVisibleRange(t.x, t.z, camX, camZ, fwdX, fwdZ, maxDrawDistSq)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, t.x, 1.6f * t.scale, t.z)
            Matrix.scaleM(modelMatrix, 0, t.scale, t.scale, t.scale)
            drawMesh(trunkMesh, 0.380f, 0.238f, 0.135f, detail = 0.08f)

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
                    drawMesh(cone, 0.082f, 0.355f, 0.190f, detail = 0.07f)
                }
            } else {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, t.x, 4.0f * t.scale, t.z)
                Matrix.scaleM(modelMatrix, 0, t.scale, t.scale, t.scale)
                drawMesh(oakCrownMesh, 0.092f, 0.425f, 0.215f, detail = 0.08f)
            }
        }

        // 5. Draw Rocks (distance + frustum culled)
        for (r in engine.rocks) {
            if (r.hp <= 0f) continue
            if (!isVisibleRange(r.x, r.z, camX, camZ, fwdX, fwdZ, maxDrawDistSq)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, r.x, 0.65f * r.scale, r.z)
            Matrix.rotateM(modelMatrix, 0, Math.toDegrees(r.rotY.toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, Math.toDegrees(r.rotX.toDouble()).toFloat(), 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, r.scale, r.scale, r.scale)
            drawMesh(rockMesh, 0.440f, 0.468f, 0.525f, specular = 0.18f, detail = 0.11f)
        }

        // 6. Draw Berry Bushes (distance + frustum culled)
        for (b in engine.bushes) {
            if (!isVisibleRange(b.x, b.z, camX, camZ, fwdX, fwdZ, maxDrawDistSq)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, b.x, 0.65f, b.z)
            drawMesh(bushMesh, 0.088f, 0.525f, 0.248f, detail = 0.08f)

            if (b.hasBerries) {
                for (off in berryOffsets) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, b.x + off[0], off[1], b.z + off[2])
                    drawMesh(berryMesh, 0.965f, 0.255f, 0.375f, emissive = 0.30f, specular = 0.45f)
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
                drawMesh(fireStoneMesh, 0.341f, 0.325f, 0.306f, detail = 0.08f)
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
            drawMesh(fireFlameMesh, 0.985f, 0.480f, 0.095f, emissive = 0.92f)
        }

        // 8. Draw Nighttime Wolves
        for (w in engine.wolves) {
            val yawDeg = Math.toDegrees(w.yaw.toDouble()).toFloat()

            setWolfTransform(w.x, 0.65f, w.z, yawDeg, 0f, 0f, 0f)
            drawMesh(wolfBodyMesh, 0.115f, 0.115f, 0.130f, detail = 0.06f)

            setWolfTransform(w.x, 0.92f, w.z, yawDeg, 0f, 0f, 0.75f)
            drawMesh(wolfHeadMesh, 0.115f, 0.115f, 0.130f)

            setWolfTransform(w.x, 0.82f, w.z, yawDeg, 0f, 0f, 1.12f)
            drawMesh(wolfSnoutMesh, 0.135f, 0.135f, 0.150f)

            setWolfTransform(w.x, 1.25f, w.z, yawDeg, -0.16f, 0f, 0.68f)
            drawMesh(wolfEarMesh, 0.115f, 0.115f, 0.130f)
            setWolfTransform(w.x, 1.25f, w.z, yawDeg, 0.16f, 0f, 0.68f)
            drawMesh(wolfEarMesh, 0.115f, 0.115f, 0.130f)

            setWolfTransform(w.x, 0.98f, w.z, yawDeg, -0.14f, 0f, 1.03f)
            drawMesh(wolfEyeMesh, 0.950f, 0.270f, 0.270f, emissive = 1.0f)
            setWolfTransform(w.x, 0.98f, w.z, yawDeg, 0.14f, 0f, 1.03f)
            drawMesh(wolfEyeMesh, 0.950f, 0.270f, 0.270f, emissive = 1.0f)

            for (lp in legPositions) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, w.x, 0f, w.z)
                Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
                Matrix.translateM(modelMatrix, 0, lp[0], lp[1], lp[2])
                Matrix.rotateM(modelMatrix, 0, sin(w.walkAnim) * 24f * lp[3], 1f, 0f, 0f)
                drawMesh(wolfLegMesh, 0.115f, 0.115f, 0.130f)
            }
        }

        // 9. Draw 3D Particles
        for (p in engine.particles) {
            if (!isVisibleRange(p.x, p.z, camX, camZ, fwdX, fwdZ, 64f * 64f)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            drawMesh(particleMesh, p.r, p.g, p.b, emissive = 0.35f)
        }

        // 10. Draw First-Person Viewmodel (Camera Space)
        if (engine.gamePhase == GamePhase.PLAYING) {
            renderViewmodelLocked(bobX, bobY)
        }
    }

    private fun renderViewmodelLocked(bobX: Float, bobY: Float) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)

        System.arraycopy(projMatrix, 0, vpMatrix, 0, 16)
        GLES20.glUniform3f(worldShader.uCameraPos, 0f, 0f, 0f)
        GLES20.glUniform1f(worldShader.uFogDensity, 0f)

        Matrix.setIdentityM(baseArmMatrix, 0)
        Matrix.translateM(baseArmMatrix, 0, 0.36f + bobX * 0.6f, -0.32f + abs(bobY) * 0.6f, -0.45f)

        if (engine.swingTimer > 0f) {
            val swingProgress = 1f - (engine.swingTimer / 0.24f).coerceIn(0f, 1f)
            val swingArc = sin(swingProgress * PI.toFloat())
            Matrix.translateM(baseArmMatrix, 0, 0f, 0f, -swingArc * 0.22f)
            Matrix.rotateM(baseArmMatrix, 0, Math.toDegrees((swingArc * 0.35f).toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(baseArmMatrix, 0, Math.toDegrees((-swingArc * 1.15f).toDouble()).toFloat(), 1f, 0f, 0f)
        }

        System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0f, -0.02f, 0.18f)
        drawMesh(forearmMesh, 0.114f, 0.306f, 0.847f)

        System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -0.08f)
        drawMesh(fistMesh, 0.851f, 0.608f, 0.400f)

        when {
            engine.selectedSlot == 1 && engine.hasAxe -> {
                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.16f, -0.14f)
                Matrix.rotateM(modelMatrix, 0, 27f, 1f, 0f, 0f)
                drawMesh(axeHandleMesh, 0.471f, 0.208f, 0.059f)

                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.42f, -0.25f)
                Matrix.rotateM(modelMatrix, 0, 27f, 1f, 0f, 0f)
                drawMesh(axeBladeMesh, 0.680f, 0.710f, 0.760f, specular = 0.65f)
            }
            engine.selectedSlot == 2 && engine.hasSpear -> {
                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.02f, -0.35f)
                Matrix.rotateM(modelMatrix, 0, 86.4f, 1f, 0f, 0f)
                drawMesh(spearShaftMesh, 0.471f, 0.208f, 0.059f)

                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.05f, -1.08f)
                Matrix.rotateM(modelMatrix, 0, -93.6f, 1f, 0f, 0f)
                drawMesh(spearTipMesh, 0.680f, 0.710f, 0.760f, specular = 0.70f)
            }
            engine.selectedSlot == 3 && engine.torches > 0 -> {
                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.14f, -0.12f)
                Matrix.rotateM(modelMatrix, 0, 21.6f, 1f, 0f, 0f)
                drawMesh(torchStickMesh, 0.471f, 0.208f, 0.059f)

                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.41f, -0.22f)
                drawMesh(torchHeadMesh, 1.0f, 0.596f, 0.0f, emissive = 0.95f)
            }
            engine.selectedSlot == 4 && engine.berries > 0 -> {
                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
                Matrix.translateM(modelMatrix, 0, 0f, 0.08f, -0.12f)
                drawMesh(heldBerryMesh, 0.957f, 0.247f, 0.369f, emissive = 0.22f, specular = 0.45f)
            }
            engine.selectedSlot == 5 && engine.bandages > 0 -> {
                System.arraycopy(baseArmMatrix, 0, modelMatrix, 0, 16)
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

    private fun drawMesh(
        mesh: GpuMesh,
        r: Float,
        g: Float,
        b: Float,
        emissive: Float = 0f,
        specular: Float = 0f,
        detail: Float = 0f
    ) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uMVPMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(worldShader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(worldShader.uColor, r, g, b)
        GLES20.glUniform1f(worldShader.uEmissive, emissive)
        if (worldShader.uSpecular >= 0) GLES20.glUniform1f(worldShader.uSpecular, specular)
        if (worldShader.uDetailScale >= 0) GLES20.glUniform1f(worldShader.uDetailScale, detail)
        mesh.draw(worldShader.aPosition, worldShader.aNormal)
    }
}
