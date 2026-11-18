package com.example.gl

import android.opengl.GLES20
import android.util.Log

private const val TAG = "WildSurvivalGL"

class WorldShaderProgram {
    val programId: Int
    val isValid: Boolean

    val aPosition: Int
    val aNormal: Int

    val uMVPMatrix: Int
    val uModelMatrix: Int
    val uColor: Int
    val uEmissive: Int
    val uSunDir: Int
    val uSunIntensity: Int
    val uHemiIntensity: Int
    val uSkyColor: Int
    val uFogDensity: Int
    val uCameraPos: Int
    val uPointLightPos: Int
    val uPointLightColor: Int
    val uPointLightIntensity: Int

    init {
        val vs = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                mat3 normalMat = mat3(
                    uModelMatrix[0].xyz,
                    uModelMatrix[1].xyz,
                    uModelMatrix[2].xyz
                );
                vWorldNormal = normalize(normalMat * aNormal);
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;

            uniform vec3 uColor;
            uniform float uEmissive;
            uniform vec3 uSunDir;
            uniform float uSunIntensity;
            uniform float uHemiIntensity;
            uniform vec3 uSkyColor;
            uniform float uFogDensity;
            uniform vec3 uCameraPos;

            uniform vec3 uPointLightPos[6];
            uniform vec3 uPointLightColor[6];
            uniform float uPointLightIntensity[6];

            void main() {
                vec3 N = normalize(vWorldNormal);

                float hemiFactor = N.y * 0.5 + 0.5;
                vec3 skyAmb = vec3(0.86, 0.92, 1.0);
                vec3 groundAmb = vec3(0.12, 0.25, 0.13);
                vec3 ambient = mix(groundAmb, skyAmb, hemiFactor) * uHemiIntensity;

                float ndotl = max(dot(N, normalize(uSunDir)), 0.0);
                vec3 sun = vec3(1.0, 0.98, 0.92) * ndotl * uSunIntensity;

                vec3 pointTotal = vec3(0.0);
                for (int i = 0; i < 6; i++) {
                    float lightPower = uPointLightIntensity[i];
                    if (lightPower > 0.001) {
                        vec3 toLight = uPointLightPos[i] - vWorldPos;
                        float dist = length(toLight);
                        if (dist < 18.0) {
                            vec3 L = toLight / max(dist, 0.001);
                            float diff = max(dot(N, L), 0.15);
                            float atten = clamp(1.0 - (dist / 18.0), 0.0, 1.0);
                            atten = atten * atten;
                            pointTotal += uPointLightColor[i] * diff * atten * lightPower;
                        }
                    }
                }

                vec3 litColor = uColor * (ambient + sun + pointTotal + vec3(uEmissive));

                float camDist = length(vWorldPos - uCameraPos);
                float fogArg = camDist * uFogDensity;
                float fogFactor = clamp(1.0 - exp(-(fogArg * fogArg)), 0.0, 1.0);
                vec3 finalColor = mix(litColor, uSkyColor, fogFactor);

                gl_FragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        programId = linkProgram(vs, fs) { prog ->
            GLES20.glBindAttribLocation(prog, 0, "aPosition")
            GLES20.glBindAttribLocation(prog, 1, "aNormal")
        }
        isValid = programId != 0
        if (isValid) {
            aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
            aNormal = GLES20.glGetAttribLocation(programId, "aNormal")

            uMVPMatrix = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
            uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
            uColor = GLES20.glGetUniformLocation(programId, "uColor")
            uEmissive = GLES20.glGetUniformLocation(programId, "uEmissive")
            uSunDir = GLES20.glGetUniformLocation(programId, "uSunDir")
            uSunIntensity = GLES20.glGetUniformLocation(programId, "uSunIntensity")
            uHemiIntensity = GLES20.glGetUniformLocation(programId, "uHemiIntensity")
            uSkyColor = GLES20.glGetUniformLocation(programId, "uSkyColor")
            uFogDensity = GLES20.glGetUniformLocation(programId, "uFogDensity")
            uCameraPos = GLES20.glGetUniformLocation(programId, "uCameraPos")
            uPointLightPos = getArrayUniformLocation(programId, "uPointLightPos")
            uPointLightColor = getArrayUniformLocation(programId, "uPointLightColor")
            uPointLightIntensity = getArrayUniformLocation(programId, "uPointLightIntensity")
        } else {
            aPosition = -1
            aNormal = -1
            uMVPMatrix = -1
            uModelMatrix = -1
            uColor = -1
            uEmissive = -1
            uSunDir = -1
            uSunIntensity = -1
            uHemiIntensity = -1
            uSkyColor = -1
            uFogDensity = -1
            uCameraPos = -1
            uPointLightPos = -1
            uPointLightColor = -1
            uPointLightIntensity = -1
        }
    }
}

class StarShaderProgram {
    val programId: Int
    val isValid: Boolean
    val aPosition: Int
    val uMVPMatrix: Int
    val uOpacity: Int

    init {
        val vs = """
            uniform mat4 uMVPMatrix;
            attribute vec3 aPosition;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
                gl_PointSize = 3.0;
            }
        """.trimIndent()

        val fs = """
            precision mediump float;
            uniform float uOpacity;
            void main() {
                gl_FragColor = vec4(1.0, 1.0, 1.0, uOpacity);
            }
        """.trimIndent()

        programId = linkProgram(vs, fs) { prog ->
            GLES20.glBindAttribLocation(prog, 0, "aPosition")
        }
        isValid = programId != 0
        if (isValid) {
            aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
            uMVPMatrix = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
            uOpacity = GLES20.glGetUniformLocation(programId, "uOpacity")
        } else {
            aPosition = -1
            uMVPMatrix = -1
            uOpacity = -1
        }
    }
}

private fun getArrayUniformLocation(programId: Int, baseName: String): Int {
    val direct = GLES20.glGetUniformLocation(programId, baseName)
    return if (direct >= 0) direct else GLES20.glGetUniformLocation(programId, "$baseName[0]")
}

private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    if (shader == 0) return 0
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        val log = GLES20.glGetShaderInfoLog(shader)
        Log.e(TAG, "Shader compile failed ($type): $log")
        GLES20.glDeleteShader(shader)
        return 0
    }
    return shader
}

private fun linkProgram(
    vsSrc: String,
    fsSrc: String,
    bindAttributes: (Int) -> Unit = {}
): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
    if (vs == 0 || fs == 0) {
        if (vs != 0) GLES20.glDeleteShader(vs)
        if (fs != 0) GLES20.glDeleteShader(fs)
        return 0
    }
    val prog = GLES20.glCreateProgram()
    if (prog == 0) {
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return 0
    }
    GLES20.glAttachShader(prog, vs)
    GLES20.glAttachShader(prog, fs)
    bindAttributes(prog)
    GLES20.glLinkProgram(prog)
    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)

    val linked = IntArray(1)
    GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0)
    if (linked[0] == 0) {
        val log = GLES20.glGetProgramInfoLog(prog)
        Log.e(TAG, "Program link failed: $log")
        GLES20.glDeleteProgram(prog)
        return 0
    }
    return prog
}
