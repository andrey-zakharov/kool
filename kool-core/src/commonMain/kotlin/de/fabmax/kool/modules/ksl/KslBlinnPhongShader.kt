package de.fabmax.kool.modules.ksl

import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ksl.blocks.*
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.util.Color

fun blinnPhongShader(cfgBlock: KslBlinnPhongShader.Config.() -> Unit): KslBlinnPhongShader {
    val cfg = KslBlinnPhongShader.Config().apply(cfgBlock)
    return KslBlinnPhongShader(cfg)
}

class KslBlinnPhongShader(cfg: Config, model: KslProgram = Model(cfg)) : KslShader(model) {

    var uniformDiffuseColor: Vec4f by uniform4f(cfg.colorCfg.primaryUniformColor?.uniformName, cfg.colorCfg.primaryUniformColor?.defaultColor)
    var colorTexture: Texture2d? by texture2d(cfg.colorCfg.primaryTextureColor?.textureName, cfg.colorCfg.primaryTextureColor?.defaultTexture)

    var specularColor: Vec4f by uniform4f("uSpecularColor", cfg.specularColor)
    var ambientColor: Vec4f by uniform4f("uAmbientColor", cfg.ambientColor)
    var shininess: Float by uniform1f("uShininess", cfg.shininess)

    class Config {
        val colorCfg = ColorBlockConfig()

        var isInstanced = false
        var isOutputToSrgbColorSpace = true

        var specularColor = Color.WHITE
        var ambientColor = Color(0.1f, 0.1f, 0.1f).toLinear()
        var shininess = 16f

        fun color(colorBlock: ColorBlockConfig.() -> Unit) {
            colorCfg.apply(colorBlock)
        }
    }

    class Model(cfg: Config) : KslProgram("Blinn-Phong Shader") {
        init {
            val uMvp = mvpMatrix()
            val uModelMat = modelMatrix()

            val uSpecularColor = uniformFloat4("uSpecularColor")
            val uAmbientColor = uniformFloat4("uAmbientColor")
            val uShininess = uniformFloat1("uShininess")

            val positionWorldSpace = interStageFloat3()
            val normalWorldSpace = interStageFloat3()

            vertexStage {
                main {
                    val mvp = mat4Var(uMvp.matrix)
                    val modelMat = mat4Var(uModelMat.matrix)
                    if (cfg.isInstanced) {
                        val instanceModelMat = instanceAttribMat4(Attribute.INSTANCE_MODEL_MAT.name)
                        mvp *= instanceModelMat
                        modelMat *= instanceModelMat
                    }

                    val localPos = constFloat4(vertexAttribFloat3(Attribute.POSITIONS.name), 1f)
                    val localNormal = constFloat4(vertexAttribFloat3(Attribute.NORMALS.name), 0f)

                    positionWorldSpace.input set (modelMat * localPos).xyz
                    normalWorldSpace.input set (modelMat * localNormal).xyz
                    outPosition set mvp * localPos
                }
            }
            fragmentStage {
                val camData = cameraData()
                val lightData = sceneLightData()

                main {
                    val normal = float3Var(normalize(normalWorldSpace.output), "normal")
                    val fragmentPos = positionWorldSpace.output
                    val camPos = camData.position

                    val fragmentColor = fragmentColorBlock(cfg.colorCfg).outColor
                    val ambientColor = float3Var(fragmentColor.rgb * uAmbientColor.rgb)
                    val diffuseColor = float3Var()
                    val specularColor = float3Var()

                    fori (0.const, lightData.lightCount) { i ->
                        val lightDir = float3Var(lightData.lightPositions[i].xyz - fragmentPos, "lightDir")
                        val lightDistance = floatVar(length(lightDir), "lightDistance")
                        lightDistance *= lightDistance
                        lightDir set normalize(lightDir)

                        val lambertian = floatVar(max(dot(lightDir, normal), 0f.const), "lambertian")
                        val specular = floatVar(0f.const, "specular")
                        `if` (lambertian gt 0f.const) {
                            val viewDir = float3Var(normalize(camPos - fragmentPos))
                            val halfDir = float3Var(normalize(lightDir + viewDir))
                            val specAngle = floatVar(max(dot(halfDir, normal), 0f.const))
                            specular set pow(specAngle, uShininess)
                        }

                        val lightStrength = floatVar(lightData.lightColors[i].w / (1f.const + lightDistance))
                        val radiance = float3Var(lightData.lightColors[i].rgb * lightStrength)
                        diffuseColor += fragmentColor.rgb * lambertian * radiance
                        specularColor += uSpecularColor.rgb * specular * radiance
                    }

                    val outColor = float3Var(ambientColor + diffuseColor + specularColor)
                    if (cfg.isOutputToSrgbColorSpace) {
                        outColor set pow(outColor, Vec3f(Color.GAMMA_LINEAR_TO_sRGB).const)
                    }
                    colorOutput(outColor * fragmentColor.a, fragmentColor.a)
                }

                hierarchy.printHierarchy()
            }
        }
    }
}