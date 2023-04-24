package de.fabmax.kool.math

import de.fabmax.kool.KoolException
import de.fabmax.kool.lock
import de.fabmax.kool.util.Float32Buffer
import kotlin.math.*

open class Mat4d {

    val array = DoubleArray(16)

    init {
        setIdentity()
    }

    fun translate(tx: Float, ty: Float, tz: Float): Mat4d = translate(tx.toDouble(), ty.toDouble(), tz.toDouble())

    fun translate(tx: Double, ty: Double, tz: Double): Mat4d {
        for (i in 0..3) {
            array[12 + i] += array[i] * tx + array[4 + i] * ty + array[8 + i] * tz
        }
        return this
    }

    fun translate(t: Vec3f): Mat4d = translate(t.x.toDouble(), t.y.toDouble(), t.z.toDouble())

    fun translate(t: Vec3d): Mat4d = translate(t.x, t.y, t.z)

    fun translate(tx: Double, ty: Double, tz: Double, result: Mat4d): Mat4d {
        for (i in 0..11) {
            result.array[i] = array[i]
        }
        for (i in 0..3) {
            result.array[12 + i] = array[i] * tx + array[4 + i] * ty + array[8 + i] * tz + array[12 + i]
        }
        return result
    }

    fun rotate(angleDeg: Double, axX: Double, axY: Double, axZ: Double): Mat4d {
        return lock(tmpMatLock) {
            tmpMatA.setRotate(angleDeg, axX, axY, axZ)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(angleDeg: Float, axis: Vec3f) = rotate(angleDeg.toDouble(), axis.x.toDouble(), axis.y.toDouble(), axis.z.toDouble())

    fun rotate(angleDeg: Double, axis: Vec3d) = rotate(angleDeg, axis.x, axis.y, axis.z)

    fun rotate(eulerX: Double, eulerY: Double, eulerZ: Double): Mat4d {
        return lock(tmpMatLock) {
            tmpMatA.setRotate(eulerX, eulerY, eulerZ)
            set(mul(tmpMatA, tmpMatB))
        }
    }

    fun rotate(angleDeg: Double, axX: Double, axY: Double, axZ: Double, result: Mat4d): Mat4d {
        return lock(tmpMatLock) {
            tmpMatA.setRotate(angleDeg, axX, axY, axZ)
            mul(tmpMatA, result)
        }
    }

    fun rotate(angleDeg: Double, axis: Vec3d, result: Mat4d) = rotate(angleDeg, axis.x, axis.y, axis.z, result)

    fun rotate(eulerX: Double, eulerY: Double, eulerZ: Double, result: Mat4d): Mat4d {
        result.set(this)
        result.rotate(eulerX, eulerY, eulerZ)
        return result
    }

    fun scale(s: Double) = scale(s, s, s)

    fun scale(sx: Double, sy: Double, sz: Double): Mat4d {
        for (i in 0..3) {
            array[i] *= sx
            array[4 + i] *= sy
            array[8 + i] *= sz
        }
        return this
    }

    fun scale(scale: Vec3d): Mat4d = scale(scale.x, scale.y, scale.z)

    fun scale(sx: Double, sy: Double, sz: Double, result: Mat4d): Mat4d {
        for (i in 0..3) {
            result.array[0] = array[0] * sx
            result.array[4 + i] = array[4 + i] * sy
            result.array[8 + i] = array[8 + i] * sz
            result.array[12 + i] = array[12 + i]
        }
        return result
    }

    fun resetScale(): Mat4d {
        val s0 = 1.0 / sqrt(this[0, 0] * this[0, 0] + this[1, 0] * this[1, 0] + this[2, 0] * this[2, 0])
        val s1 = 1.0 / sqrt(this[0, 1] * this[0, 1] + this[1, 1] * this[1, 1] + this[2, 1] * this[2, 1])
        val s2 = 1.0 / sqrt(this[0, 2] * this[0, 2] + this[1, 2] * this[1, 2] + this[2, 2] * this[2, 2])
        scale(s0, s1, s2)
        return this
    }

    fun transpose(): Mat4d {
        return lock(tmpMatLock) {
            set(transpose(tmpMatA))
        }
    }

    fun transpose(result: Mat4d): Mat4d {
        for (i in 0..3) {
            val mBase = i * 4
            result.array[i] = array[mBase]
            result.array[i + 4] = array[mBase + 1]
            result.array[i + 8] = array[mBase + 2]
            result.array[i + 12] = array[mBase + 3]
        }
        return result
    }

    fun invert(eps: Double = 0.0): Boolean {
        return lock(tmpMatLock) { invert(tmpMatA, eps).also { if (it) set(tmpMatA) } }
    }

    fun invert(result: Mat4d, eps: Double = 0.0): Boolean {
        // Invert a 4 x 4 matrix using Cramer's Rule

        // transpose matrix
        val src0 = array[0]
        val src4 = array[1]
        val src8 = array[2]
        val src12 = array[3]

        val src1 = array[4]
        val src5 = array[5]
        val src9 = array[6]
        val src13 = array[7]

        val src2 = array[8]
        val src6 = array[9]
        val src10 = array[10]
        val src14 = array[11]

        val src3 = array[12]
        val src7 = array[13]
        val src11 = array[14]
        val src15 = array[15]

        // calculate pairs for first 8 elements (cofactors)
        val atmp0 = src10 * src15
        val atmp1 = src11 * src14
        val atmp2 = src9 * src15
        val atmp3 = src11 * src13
        val atmp4 = src9 * src14
        val atmp5 = src10 * src13
        val atmp6 = src8 * src15
        val atmp7 = src11 * src12
        val atmp8 = src8 * src14
        val atmp9 = src10 * src12
        val atmp10 = src8 * src13
        val atmp11 = src9 * src12

        // calculate first 8 elements (cofactors)
        val dst0 = atmp0 * src5 + atmp3 * src6 + atmp4 * src7 - (atmp1 * src5 + atmp2 * src6 + atmp5 * src7)
        val dst1 = atmp1 * src4 + atmp6 * src6 + atmp9 * src7 - (atmp0 * src4 + atmp7 * src6 + atmp8 * src7)
        val dst2 = atmp2 * src4 + atmp7 * src5 + atmp10 * src7 - (atmp3 * src4 + atmp6 * src5 + atmp11 * src7)
        val dst3 = atmp5 * src4 + atmp8 * src5 + atmp11 * src6 - (atmp4 * src4 + atmp9 * src5 + atmp10 * src6)
        val dst4 = atmp1 * src1 + atmp2 * src2 + atmp5 * src3 - (atmp0 * src1 + atmp3 * src2 + atmp4 * src3)
        val dst5 = atmp0 * src0 + atmp7 * src2 + atmp8 * src3 - (atmp1 * src0 + atmp6 * src2 + atmp9 * src3)
        val dst6 = atmp3 * src0 + atmp6 * src1 + atmp11 * src3 - (atmp2 * src0 + atmp7 * src1 + atmp10 * src3)
        val dst7 = atmp4 * src0 + atmp9 * src1 + atmp10 * src2 - (atmp5 * src0 + atmp8 * src1 + atmp11 * src2)

        // calculate pairs for second 8 elements (cofactors)
        val btmp0 = src2 * src7
        val btmp1 = src3 * src6
        val btmp2 = src1 * src7
        val btmp3 = src3 * src5
        val btmp4 = src1 * src6
        val btmp5 = src2 * src5
        val btmp6 = src0 * src7
        val btmp7 = src3 * src4
        val btmp8 = src0 * src6
        val btmp9 = src2 * src4
        val btmp10 = src0 * src5
        val btmp11 = src1 * src4

        // calculate second 8 elements (cofactors)
        val dst8 = btmp0 * src13 + btmp3 * src14 + btmp4 * src15 - (btmp1 * src13 + btmp2 * src14 + btmp5 * src15)
        val dst9 = btmp1 * src12 + btmp6 * src14 + btmp9 * src15 - (btmp0 * src12 + btmp7 * src14 + btmp8 * src15)
        val dst10 = btmp2 * src12 + btmp7 * src13 + btmp10 * src15 - (btmp3 * src12 + btmp6 * src13 + btmp11 * src15)
        val dst11 = btmp5 * src12 + btmp8 * src13 + btmp11 * src14 - (btmp4 * src12 + btmp9 * src13 + btmp10 * src14)
        val dst12 = btmp2 * src10 + btmp5 * src11 + btmp1 * src9 - (btmp4 * src11 + btmp0 * src9 + btmp3 * src10)
        val dst13 = btmp8 * src11 + btmp0 * src8 + btmp7 * src10 - (btmp6 * src10 + btmp9 * src11 + btmp1 * src8)
        val dst14 = btmp6 * src9 + btmp11 * src11 + btmp3 * src8 - (btmp10 * src11 + btmp2 * src8 + btmp7 * src9)
        val dst15 = btmp10 * src10 + btmp4 * src8 + btmp9 * src9 - (btmp8 * src9 + btmp11 * src10 + btmp5 * src8)

        // calculate determinant
        val det = src0 * dst0 + src1 * dst1 + src2 * dst2 + src3 * dst3

        if (det.isFuzzyZero(eps)) {
            return false
        }

        // calculate matrix inverse
        val invdet = 1.0 / det
        result.array[0] = dst0 * invdet
        result.array[1] = dst1 * invdet
        result.array[2] = dst2 * invdet
        result.array[3] = dst3 * invdet

        result.array[4] = dst4 * invdet
        result.array[5] = dst5 * invdet
        result.array[6] = dst6 * invdet
        result.array[7] = dst7 * invdet

        result.array[8] = dst8 * invdet
        result.array[9] = dst9 * invdet
        result.array[10] = dst10 * invdet
        result.array[11] = dst11 * invdet

        result.array[12] = dst12 * invdet
        result.array[13] = dst13 * invdet
        result.array[14] = dst14 * invdet
        result.array[15] = dst15 * invdet

        return true
    }

    fun transform(vec: MutableVec3f, w: Float = 1.0f) = transform(vec, w, vec)

    fun transform(vec: Vec3f, w: Float, result: MutableVec3f): MutableVec3f {
       val x = (vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + w * this[0, 3]).toFloat()
       val y = (vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + w * this[1, 3]).toFloat()
       val z = (vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + w * this[2, 3]).toFloat()
        return result.set(x, y, z)
    }

    fun transform(vec: MutableVec4f) = transform(vec, vec)

    fun transform(vec: Vec4f, result: MutableVec4f): MutableVec4f {
        val x = (vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + vec.w * this[0, 3]).toFloat()
        val y = (vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + vec.w * this[1, 3]).toFloat()
        val z = (vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + vec.w * this[2, 3]).toFloat()
        val w = (vec.x * this[3, 0] + vec.y * this[3, 1] + vec.z * this[3, 2] + vec.w * this[3, 3]).toFloat()
        return result.set(x, y, z, w)
    }

    fun transform(vec: MutableVec3d, w: Double = 1.0) = transform(vec, w, vec)

    fun transform(vec: Vec3d, w: Double = 1.0, result: MutableVec3d): MutableVec3d {
        val x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + w * this[0, 3]
        val y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + w * this[1, 3]
        val z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + w * this[2, 3]
        return result.set(x, y, z)
    }

    fun transform(vec: MutableVec4d) = transform(vec, vec)

    fun transform(vec: Vec4d, result: MutableVec4d): MutableVec4d {
        val x = vec.x * this[0, 0] + vec.y * this[0, 1] + vec.z * this[0, 2] + vec.w * this[0, 3]
        val y = vec.x * this[1, 0] + vec.y * this[1, 1] + vec.z * this[1, 2] + vec.w * this[1, 3]
        val z = vec.x * this[2, 0] + vec.y * this[2, 1] + vec.z * this[2, 2] + vec.w * this[2, 3]
        val w = vec.x * this[3, 0] + vec.y * this[3, 1] + vec.z * this[3, 2] + vec.w * this[3, 3]
        return result.set(x, y, z, w)
    }

    fun add(other: Mat4d): Mat4d {
        for (i in 0..15) {
            array[i] += other.array[i]
        }
        return this
    }

    fun mul(other: Mat4d): Mat4d {
        return lock(tmpMatLock) {
            mul(other, tmpMatA)
            set(tmpMatA)
        }
    }

    fun mul(other: Mat4d, result: Mat4d): Mat4d {
        for (i in 0..3) {
            for (j in 0..3) {
                var x = 0.0
                for (k in 0..3) {
                    x += array[j + k * 4] * other.array[i * 4 + k]
                }
                result.array[i * 4 + j] = x
            }
        }
        return result
    }

    fun set(other: Mat4d): Mat4d {
        for (i in 0..15) {
            array[i] = other.array[i]
        }
        return this
    }

    fun set(other: Mat4f): Mat4d {
        for (i in 0..15) {
            array[i] = other.array[i].toDouble()
        }
        return this
    }

    fun set(doubles: List<Double>): Mat4d {
        for (i in 0..15) {
            array[i] = doubles[i]
        }
        return this
    }

    fun setZero(): Mat4d {
        for (i in 0..15) {
            array[i] = 0.0
        }
        return this
    }

    fun setIdentity(): Mat4d {
        for (i in 1..15) {
            array[i] = 0.0
        }
        array[0] = 1.0
        array[5] = 1.0
        array[10] = 1.0
        array[15] = 1.0
        return this
    }

    fun setRotate(eulerX: Double, eulerY: Double, eulerZ: Double): Mat4d {
        val a = eulerX.toRad()
        val b = eulerY.toRad()
        val c = eulerZ.toRad()

        val ci = cos(a)
        val cj = cos(b)
        val ch = cos(c)
        val si = sin(a)
        val sj = sin(b)
        val sh = sin(c)
        val cc = ci * ch
        val cs = ci * sh
        val sc = si * ch
        val ss = si * sh

        array[0] = cj * ch
        array[4] = sj * sc - cs
        array[8] = sj * cc + ss
        array[12] = 0.0

        array[1] = cj * sh
        array[5] = sj * ss + cc
        array[9] = sj * cs - sc
        array[13] = 0.0

        array[2] = -sj
        array[6] = cj * si
        array[10] = cj * ci
        array[14] = 0.0

        array[3] = 0.0
        array[7] = 0.0
        array[11] = 0.0
        array[15] = 1.0

        return this
    }

    fun setRotate(rotA: Double, axX: Double, axY: Double, axZ: Double): Mat4d {
        val a = rotA.toRad()
        var x = axX
        var y = axY
        var z = axZ
        array[3] = 0.0
        array[7] = 0.0
        array[11] = 0.0
        array[12] = 0.0
        array[13] = 0.0
        array[14] = 0.0
        array[15] = 1.0
        val s = sin(a)
        val c = cos(a)
        if (x > 0.0 && y == 0.0 && z == 0.0) {
            array[5] = c
            array[10] = c
            array[6] = s
            array[9] = -s
            array[1] = 0.0
            array[2] = 0.0
            array[4] = 0.0
            array[8] = 0.0
            array[0] = 1.0
        } else if (x == 0.0 && y > 0.0 &&z == 0.0) {
            array[0] = c
            array[10] = c
            array[8] = s
            array[2] = -s
            array[1] = 0.0
            array[4] = 0.0
            array[6] = 0.0
            array[9] = 0.0
            array[5] = 1.0
        } else if (x == 0.0 && y == 0.0 && z > 0.0) {
            array[0] = c
            array[5] = c
            array[1] = s
            array[4] = -s
            array[2] = 0.0
            array[6] = 0.0
            array[8] = 0.0
            array[9] = 0.0
            array[10] = 1.0
        } else {
            val recipLen = 1.0f / sqrt(x*x + y*y + z*z)
            x *= recipLen
            y *= recipLen
            z *= recipLen

            val nc = 1.0 - c
            val xy = x * y
            val yz = y * z
            val zx = z * x
            val xs = x * s
            val ys = y * s
            val zs = z * s
            array[0] = x * x * nc + c
            array[4] = xy * nc - zs
            array[8] = zx * nc + ys
            array[1] = xy * nc + zs
            array[5] = y * y * nc + c
            array[9] = yz * nc - xs
            array[2] = zx * nc - ys
            array[6] = yz * nc + xs
            array[10] = z * z * nc + c
        }
        return this
    }

    fun setRotate(quaternion: Vec4d): Mat4d {
        val r = quaternion.w
        val i = quaternion.x
        val j = quaternion.y
        val k = quaternion.z

        var s = sqrt(r*r + i*i + j*j + k*k)
        s = 1.0 / (s * s)

        this[0, 0] = 1.0 - 2*s*(j*j + k*k)
        this[0, 1] = 2.0*s*(i*j - k*r)
        this[0, 2] = 2.0*s*(i*k + j*r)
        this[0, 3] = 0.0

        this[1, 0] = 2.0*s*(i*j + k*r)
        this[1, 1] = 1.0 - 2*s*(i*i + k*k)
        this[1, 2] = 2.0*s*(j*k - i*r)
        this[1, 3] = 0.0

        this[2, 0] = 2.0*s*(i*k - j*r)
        this[2, 1] = 2.0*s*(j*k + i*r)
        this[2, 2] = 1.0 - 2*s*(i*i + j*j)
        this[2, 3] = 0.0

        this[3, 0] = 0.0
        this[3, 1] = 0.0
        this[3, 2] = 0.0
        this[3, 3] = 1.0

        return this
    }

    fun setRotate(mat3: Mat3f): Mat4d {
        for (row in 0..2) {
            for (col in 0..2) {
                this[row, col] = mat3[row, col].toDouble()
            }
        }
        val l0 = this[0, 0] * this[0, 0] + this[1, 0] * this[1, 0] + this[2, 0] * this[2, 0] + this[3, 0] * this[3, 0]
        val s = 1f / sqrt(l0)
        scale(s, s, s)
        return this
    }

    fun setRotate(mat4: Mat4d): Mat4d {
        for (row in 0..2) {
            for (col in 0..2) {
                this[row, col] = mat4[row, col]
            }
        }
        val l0 = this[0, 0] * this[0, 0] + this[1, 0] * this[1, 0] + this[2, 0] * this[2, 0] + this[3, 0] * this[3, 0]
        val s = 1f / sqrt(l0)
        scale(s, s, s)
        return this
    }

    fun setTranslate(translation: Vec3d) = setTranslate(translation.x, translation.y, translation.z)

    fun setTranslate(x: Double, y: Double, z: Double): Mat4d {
        for (i in 1..15) {
            array[i] = 0.0
        }
        array[12] = x
        array[13] = y
        array[14] = z
        array[0] = 1.0
        array[5] = 1.0
        array[10] = 1.0
        array[15] = 1.0
        return this
    }

    fun setLookAt(position: Vec3f, lookAt: Vec3f, up: Vec3f) = setLookAt(
            position.x.toDouble(), position.y.toDouble(), position.z.toDouble(),
            lookAt.x.toDouble(), lookAt.y.toDouble(), lookAt.z.toDouble(),
            up.x.toDouble(), up.y.toDouble(), up.z.toDouble())

    fun setLookAt(position: Vec3d, lookAt: Vec3d, up: Vec3d) =
            setLookAt(position.x, position.y, position.z, lookAt.x, lookAt.y, lookAt.z, up.x, up.y, up.z)

    private fun setLookAt(px: Double, py: Double, pz: Double, lx: Double, ly: Double, lz: Double, upx: Double, upy: Double, upz: Double): Mat4d {
        // See the OpenGL GLUT documentation for gluLookAt for a description
        // of the algorithm. We implement it in a straightforward way:
        var fx = lx - px
        var fy = ly - py
        var fz = lz - pz

        // Normalize f
        val rlf = 1.0 / sqrt(fx*fx + fy*fy + fz*fz)
        fx *= rlf
        fy *= rlf
        fz *= rlf

        // compute s = f x up (x means "cross product")
        var sx = fy * upz - fz * upy
        var sy = fz * upx - fx * upz
        var sz = fx * upy - fy * upx

        // and normalize s
        val rls = 1.0 / sqrt(sx*sx + sy*sy + sz*sz)
        sx *= rls
        sy *= rls
        sz *= rls

        // compute u = s x f
        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx

        array[0] = sx
        array[1] = ux
        array[2] = -fx
        array[3] = 0.0

        array[4] = sy
        array[5] = uy
        array[6] = -fy
        array[7] = 0.0

        array[8] = sz
        array[9] = uz
        array[10] = -fz
        array[11] = 0.0

        array[12] = 0.0
        array[13] = 0.0
        array[14] = 0.0
        array[15] = 1.0

        return translate(-px, -py, -pz)
    }

    fun setOrthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) =
            setOrthographic(left.toDouble(), right.toDouble(), bottom.toDouble(), top.toDouble(), near.toDouble(), far.toDouble())

    fun setOrthographic(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Mat4d {
        if (left == right) {
            throw IllegalArgumentException("left == right")
        }
        if (bottom == top) {
            throw IllegalArgumentException("bottom == top")
        }
        if (near == far) {
            throw IllegalArgumentException("near == far")
        }

        val width = 1.0 / (right - left)
        val height = 1.0 / (top - bottom)
        val depth = 1.0 / (far - near)
        val x = 2.0 * width
        val y = 2.0 * height
        val z = -2.0 * depth
        val tx = -(right + left) * width
        val ty = -(top + bottom) * height
        val tz = -(far + near) * depth
        array[0] = x
        array[5] = y
        array[10] = z
        array[12] = tx
        array[13] = ty
        array[14] = tz
        array[15] = 1.0
        array[1] = 0.0
        array[2] = 0.0
        array[3] = 0.0
        array[4] = 0.0
        array[6] = 0.0
        array[7] = 0.0
        array[8] = 0.0
        array[9] = 0.0
        array[11] = 0.0

        return this
    }

    fun setPerspective(fovy: Float, aspect: Float, near: Float, far: Float) =
            setPerspective(fovy.toDouble(), aspect.toDouble(), near.toDouble(), far.toDouble())

    fun setPerspective(fovy: Double, aspect: Double, near: Double, far: Double): Mat4d {
        val f = 1.0 / tan(fovy * (PI / 360.0))
        val rangeReciprocal = 1.0 / (near - far)

        array[0] = f / aspect
        array[1] = 0.0
        array[2] = 0.0
        array[3] = 0.0

        array[4] = 0.0
        array[5] = f
        array[6] = 0.0
        array[7] = 0.0

        array[8] = 0.0
        array[9] = 0.0
        array[10] = (far + near) * rangeReciprocal
        array[11] = -1.0

        array[12] = 0.0
        array[13] = 0.0
        array[14] = 2.0 * far * near * rangeReciprocal
        array[15] = 0.0

        return this
    }

    operator fun get(i: Int): Double = array[i]

    operator fun get(row: Int, col: Int): Double = array[col * 4 + row]

    operator fun set(i: Int, value: Double) {
        array[i] = value
    }

    operator fun set(row: Int, col: Int, value: Double) {
        array[col * 4 + row] = value
    }

    fun setRow(row: Int, vec: Vec3d, w: Double) {
        this[row, 0] = vec.x
        this[row, 1] = vec.y
        this[row, 2] = vec.z
        this[row, 3] = w
    }

    fun setRow(row: Int, value: Vec4d) {
        this[row, 0] = value.x
        this[row, 1] = value.y
        this[row, 2] = value.z
        this[row, 3] = value.w
    }

    fun getRow(row: Int, result: MutableVec4d): MutableVec4d {
        result.x = this[row, 0]
        result.y = this[row, 1]
        result.z = this[row, 2]
        result.w = this[row, 3]
        return result
    }

    fun setCol(col: Int, vec: Vec3d, w: Double) {
        this[0, col] = vec.x
        this[1, col] = vec.y
        this[2, col] = vec.z
        this[3, col] = w
    }

    fun setCol(col: Int, value: Vec4d) {
        this[0, col] = value.x
        this[1, col] = value.y
        this[2, col] = value.z
        this[3, col] = value.w
    }

    fun getCol(col: Int, result: MutableVec4d): MutableVec4d {
        result.x = this[0, col]
        result.y = this[1, col]
        result.z = this[2, col]
        result.w = this[3, col]
        return result
    }

    fun getOrigin(result: MutableVec3d): MutableVec3d {
        result.x = this[0, 3]
        result.y = this[1, 3]
        result.z = this[2, 3]
        return result
    }

    fun setOrigin(origin: Vec3d): Mat4d {
        this[0, 3] = origin.x
        this[1, 3] = origin.y
        this[2, 3] = origin.z
        return this
    }

    fun getRotation(result: Mat3d): Mat3d {
        result[0, 0] = this[0, 0]
        result[0, 1] = this[0, 1]
        result[0, 2] = this[0, 2]

        result[1, 0] = this[1, 0]
        result[1, 1] = this[1, 1]
        result[1, 2] = this[1, 2]

        result[2, 0] = this[2, 0]
        result[2, 1] = this[2, 1]
        result[2, 2] = this[2, 2]

        return result
    }

    fun getRotation(result: Mat3f): Mat3f {
        result[0, 0] = this[0, 0].toFloat()
        result[0, 1] = this[0, 1].toFloat()
        result[0, 2] = this[0, 2].toFloat()

        result[1, 0] = this[1, 0].toFloat()
        result[1, 1] = this[1, 1].toFloat()
        result[1, 2] = this[1, 2].toFloat()

        result[2, 0] = this[2, 0].toFloat()
        result[2, 1] = this[2, 1].toFloat()
        result[2, 2] = this[2, 2].toFloat()

        return result
    }

    fun getRotationTransposed(result: Mat3f): Mat3f {
        result[0, 0] = this[0, 0].toFloat()
        result[0, 1] = this[1, 0].toFloat()
        result[0, 2] = this[2, 0].toFloat()

        result[1, 0] = this[0, 1].toFloat()
        result[1, 1] = this[1, 1].toFloat()
        result[1, 2] = this[2, 1].toFloat()

        result[2, 0] = this[0, 2].toFloat()
        result[2, 1] = this[1, 2].toFloat()
        result[2, 2] = this[2, 2].toFloat()

        return result
    }

    fun getRotation(result: MutableVec4d): MutableVec4d {
        val trace = this[0, 0] + this[1, 1] + this[2, 2]

        if (trace > 0f) {
            var s = sqrt(trace + 1f)
            result.w = s * 0.5
            s = 0.5f / s

            result.x = (this[2, 1] - this[1, 2]) * s
            result.y = (this[0, 2] - this[2, 0]) * s
            result.z = (this[1, 0] - this[0, 1]) * s

        } else {
            val i = if (this[0, 0] < this[1, 1]) {
                if (this[1, 1] < this[2, 2]) { 2 } else { 1 }
            } else {
                if (this[0, 0] < this[2, 2]) { 2 } else { 0 }
            }
            val j = (i + 1) % 3
            val k = (i + 2) % 3

            var s = sqrt(this[i, i] - this[j, j] - this[k, k] + 1f)
            result[i] = s * 0.5f
            s = 0.5f / s

            result.w = (this[k, j] - this[j, k]) * s
            result[j] = (this[j, i] + this[i, j]) * s
            result[k] = (this[k, i] + this[i, k]) * s
        }
        return result
    }

    fun getScale(result: MutableVec3d): MutableVec3d {
        val ax = this[0, 0]
        val ay = this[1, 0]
        val az = this[2, 0]

        val bx = this[0, 1]
        val by = this[1, 1]
        val bz = this[2, 1]

        val cx = this[0, 2]
        val cy = this[1, 2]
        val cz = this[2, 2]

        return result.set(
            sqrt(ax*ax + ay*ay + az*az),
            sqrt(bx*bx + by*by + bz*bz),
            sqrt(cx*cx + cy*cy + cz*cz)
        )
    }

    fun toBuffer(buffer: Float32Buffer): Float32Buffer {
        for (i in 0 until 16) {
            buffer.put(array[i].toFloat())
        }
        buffer.flip()
        return buffer
    }

    fun toList(): List<Double> {
        val list = mutableListOf<Double>()
        for (i in 0..15) {
            list += array[i]
        }
        return list
    }

    fun dump() {
        for (r in 0..3) {
            for (c in 0..3) {
                print("${this[r, c]} ")
            }
            println()
        }
    }

    companion object {
        private val tmpMatLock = Any()
        private val tmpMatA = Mat4d()
        private val tmpMatB = Mat4d()
    }
}

class Mat4dStack(val stackSize: Int = DEFAULT_STACK_SIZE) : Mat4d() {
    companion object {
        const val DEFAULT_STACK_SIZE = 32
    }

    private var stackIndex = 0
    private val stack = DoubleArray(16 * stackSize)

    fun push(): Mat4dStack {
        if (stackIndex >= stackSize) {
            throw KoolException("Matrix stack overflow")
        }
        val offset = stackIndex * 16
        for (i in 0 .. 15) {
            stack[offset + i] = array[i]
        }
        stackIndex++
        return this
    }

    fun pop(): Mat4dStack {
        if (stackIndex <= 0) {
            throw KoolException("Matrix stack underflow")
        }
        stackIndex--
        val offset = stackIndex * 16
        for (i in 0 .. 15) {
            array[i] = stack[offset + i]
        }
        return this
    }

    fun reset(): Mat4dStack {
        stackIndex = 0
        setIdentity()
        return this
    }
}
