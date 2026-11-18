package com.example.gl

import android.opengl.GLES20

class WorldShaderProgram {
    val programId: Int

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
    val uPointLightCount: Int
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
                vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
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

            uniform int uPointLightCount;
            uniform vec3 uPointLightPos[6];
            uniform vec3 uPointLightColor[6];
            uniform float uPointLightIntensity[6];

            void main() {
                vec3 N = normalize(vWorldNormal);

                // Hemisphere ambient light
                float hemiFactor = N.y * 0.5 + 0.5;
                vec3 skyAmb = vec3(0.86, 0.92, 1.0);
                vec3 groundAmb = vec3(0.12, 0.25, 0.13);
                vec3 ambient = mix(groundAmb, skyAmb, hemiFactor) * uHemiIntensity;

                // Directional sun light
                float ndotl = max(dot(N, normalize(uSunDir)), 0.0);
                vec3 sun = vec3(1.0, 0.98, 0.92) * ndotl * uSunIntensity;

                // Dynamic point lights (campfires + hand torch)
                vec3 pointTotal = vec3(0.0);
                for (int i = 0; i < 6; i++) {
                    if (i >= uPointLightCount) break;
                    vec3 toLight = uPointLightPos[i] - vWorldPos;
                    float dist = length(toLight);
                    if (dist < 18.0) {
                        vec3 L = toLight / max(dist, 0.001);
                        float diff = max(dot(N, L), 0.15);
                        float atten = clamp(1.0 - (dist / 18.0), 0.0, 1.0);
                        atten = atten * atten;
                        pointTotal += uPointLightColor[i] * diff * atten * uPointLightIntensity[i];
                    }
                }

                vec3 litColor = uColor * (ambient + sun + pointTotal + vec3(uEmissive));

                // Exponential FogExp2
                float camDist = length(vWorldPos - uCameraPos);
                float fogFactor = 1.0 - exp(-pow(camDist * uFogDensity, 2.0));
                fogFactor = clamp(fogFactor, 0.0, 1.0);
                vec3 finalColor = mix(litColor, uSkyColor, fogFactor);

                gl_FragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        programId = linkProgram(vs, fs)
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
        uPointLightCount = GLES20.glGetUniformLocation(programId, "uPointLightCount")
        uPointLightPos = GLES20.glGetUniformLocation(programId, "uPointLightPos")
        uPointLightColor = GLES20.glGetUniformLocation(programId, "uPointLightColor")
        uPointLightIntensity = GLES20.glGetUniformLocation(programId, "uPointLightIntensity")
    }
}

class StarShaderProgram {
    val programId: Int
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

        programId = linkProgram(vs, fs)
        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        uMVPMatrix = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uOpacity = GLES20.glGetUniformLocation(programId, "uOpacity")
    }
}

private fun compileShader(type: Int, src: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, src)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        val log = GLES20.glGetShaderInfoLog(shader)
        GLES20.glDeleteShader(shader)
        throw RuntimeException("Shader compile failed: $log")
    }
    return shader
}

private fun linkProgram(vsSrc: String, fsSrc: String): Int {
    val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc)
    val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc)
    val prog = GLES20.glCreateProgram()
    GLES20.glAttachShader(prog, vs)
    GLES20.glAttachShader(prog, fs)
    GLES20.glLinkProgram(prog)
    GLES20.glDeleteShader(vs)
    GLES20.glDeleteShader(fs)
    return prog
}
