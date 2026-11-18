package com.example.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GpuMesh(
    val vboId: Int,
    val vertexCount: Int
) {
    fun draw(posAttr: Int, normAttr: Int) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glEnableVertexAttribArray(posAttr)
        GLES20.glVertexAttribPointer(posAttr, 3, GLES20.GL_FLOAT, false, 24, 0)
        GLES20.glEnableVertexAttribArray(normAttr)
        GLES20.glVertexAttribPointer(normAttr, 3, GLES20.GL_FLOAT, false, 24, 12)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }
}

object MeshBuilder {

    fun upload(data: FloatArray): GpuMesh {
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        val vbo = buffers[0]
        val fb: FloatBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
        fb.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, fb, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return GpuMesh(vbo, data.size / 6)
    }

    private fun pushTri(
        list: MutableList<Float>,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        list.add(ax); list.add(ay); list.add(az); list.add(nx); list.add(ny); list.add(nz)
        list.add(bx); list.add(by); list.add(bz); list.add(nx); list.add(ny); list.add(nz)
        list.add(cx); list.add(cy); list.add(cz); list.add(nx); list.add(ny); list.add(nz)
    }

    private fun pushQuad(
        list: MutableList<Float>,
        ax: Float, ay: Float, az: Float,
        bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float,
        dx: Float, dy: Float, dz: Float,
        nx: Float, ny: Float, nz: Float
    ) {
        pushTri(list, ax, ay, az, bx, by, bz, cx, cy, cz, nx, ny, nz)
        pushTri(list, ax, ay, az, cx, cy, cz, dx, dy, dz, nx, ny, nz)
    }

    fun createPlaneXZ(width: Float, depth: Float): FloatArray {
        val hw = width * 0.5f
        val hd = depth * 0.5f
        val out = ArrayList<Float>(36)
        pushQuad(
            out,
            -hw, 0f, -hd,
            -hw, 0f, hd,
            hw, 0f, hd,
            hw, 0f, -hd,
            0f, 1f, 0f
        )
        return out.toFloatArray()
    }

    fun createCircleXZ(radius: Float, segments: Int = 32): FloatArray {
        val out = ArrayList<Float>(segments * 18)
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            pushTri(
                out,
                0f, 0f, 0f,
                cos(a1) * radius, 0f, sin(a1) * radius,
                cos(a0) * radius, 0f, sin(a0) * radius,
                0f, 1f, 0f
            )
        }
        return out.toFloatArray()
    }

    fun createRingXZ(innerR: Float, outerR: Float, segments: Int = 40): FloatArray {
        val out = ArrayList<Float>(segments * 36)
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            pushQuad(
                out,
                c0 * innerR, 0f, s0 * innerR,
                c1 * innerR, 0f, s1 * innerR,
                c1 * outerR, 0f, s1 * outerR,
                c0 * outerR, 0f, s0 * outerR,
                0f, 1f, 0f
            )
        }
        return out.toFloatArray()
    }

    fun createBox(w: Float, h: Float, d: Float): FloatArray {
        val x = w * 0.5f; val y = h * 0.5f; val z = d * 0.5f
        val out = ArrayList<Float>(36 * 6)
        // +Z front
        pushQuad(out, -x, -y, z, x, -y, z, x, y, z, -x, y, z, 0f, 0f, 1f)
        // -Z back
        pushQuad(out, x, -y, -z, -x, -y, -z, -x, y, -z, x, y, -z, 0f, 0f, -1f)
        // +Y top
        pushQuad(out, -x, y, z, x, y, z, x, y, -z, -x, y, -z, 0f, 1f, 0f)
        // -Y bottom
        pushQuad(out, -x, -y, -z, x, -y, -z, x, -y, z, -x, -y, z, 0f, -1f, 0f)
        // +X right
        pushQuad(out, x, -y, z, x, -y, -z, x, y, -z, x, y, z, 1f, 0f, 0f)
        // -X left
        pushQuad(out, -x, -y, -z, -x, -y, z, -x, y, z, -x, y, -z, -1f, 0f, 0f)
        return out.toFloatArray()
    }

    fun createCylinder(radiusTop: Float, radiusBottom: Float, height: Float, segments: Int = 10): FloatArray {
        val hh = height * 0.5f
        val out = ArrayList<Float>(segments * 72)
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val am = (a0 + a1) * 0.5f
            val slope = (radiusBottom - radiusTop) / height
            val nx = cos(am)
            val nz = sin(am)
            val ny = slope
            val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)

            // Outward CCW winding
            pushQuad(
                out,
                c0 * radiusBottom, -hh, s0 * radiusBottom,
                c0 * radiusTop, hh, s0 * radiusTop,
                c1 * radiusTop, hh, s1 * radiusTop,
                c1 * radiusBottom, -hh, s1 * radiusBottom,
                nx / nl, ny / nl, nz / nl
            )

            // Top cap (+Y normal)
            if (radiusTop > 0.001f) {
                pushTri(
                    out,
                    0f, hh, 0f,
                    c1 * radiusTop, hh, s1 * radiusTop,
                    c0 * radiusTop, hh, s0 * radiusTop,
                    0f, 1f, 0f
                )
            }
            // Bottom cap (-Y normal)
            if (radiusBottom > 0.001f) {
                pushTri(
                    out,
                    0f, -hh, 0f,
                    c0 * radiusBottom, -hh, s0 * radiusBottom,
                    c1 * radiusBottom, -hh, s1 * radiusBottom,
                    0f, -1f, 0f
                )
            }
        }
        return out.toFloatArray()
    }

    fun createCone(radius: Float, height: Float, segments: Int = 7): FloatArray {
        val hh = height * 0.5f
        val out = ArrayList<Float>(segments * 36)
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val am = (a0 + a1) * 0.5f
            val nx = cos(am)
            val nz = sin(am)
            val ny = radius / height
            val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)

            // Outward CCW winding
            pushTri(
                out,
                c0 * radius, -hh, s0 * radius,
                0f, hh, 0f,
                c1 * radius, -hh, s1 * radius,
                nx / nl, ny / nl, nz / nl
            )
            // Base cap (-Y normal)
            pushTri(
                out,
                0f, -hh, 0f,
                c0 * radius, -hh, s0 * radius,
                c1 * radius, -hh, s1 * radius,
                0f, -1f, 0f
            )
        }
        return out.toFloatArray()
    }

    fun createFacetedSphere(radius: Float, latBands: Int = 5, lonBands: Int = 7): FloatArray {
        val out = ArrayList<Float>(latBands * lonBands * 36)
        for (lat in 0 until latBands) {
            val phi0 = (lat.toFloat() / latBands) * PI.toFloat()
            val phi1 = ((lat + 1).toFloat() / latBands) * PI.toFloat()
            val y0 = cos(phi0) * radius
            val r0 = sin(phi0) * radius
            val y1 = cos(phi1) * radius
            val r1 = sin(phi1) * radius

            for (lon in 0 until lonBands) {
                val th0 = (lon.toFloat() / lonBands) * 2f * PI.toFloat()
                val th1 = ((lon + 1).toFloat() / lonBands) * 2f * PI.toFloat()

                val ax = cos(th0) * r0; val az = sin(th0) * r0
                val bx = cos(th0) * r1; val bz = sin(th0) * r1
                val cx = cos(th1) * r1; val cz = sin(th1) * r1
                val dx = cos(th1) * r0; val dz = sin(th1) * r0

                val pm = (phi0 + phi1) * 0.5f
                val tm = (th0 + th1) * 0.5f
                val nx = sin(pm) * cos(tm)
                val ny = cos(pm)
                val nz = sin(pm) * sin(tm)

                // Outward CCW winding
                if (lat > 0) {
                    pushTri(out, ax, y0, az, dx, y0, dz, cx, y1, cz, nx, ny, nz)
                }
                if (lat < latBands - 1) {
                    pushTri(out, ax, y0, az, cx, y1, cz, bx, y1, bz, nx, ny, nz)
                }
            }
        }
        return out.toFloatArray()
    }
}
