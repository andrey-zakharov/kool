package de.fabmax.kool.util

import de.fabmax.kool.platform.Math
import de.fabmax.kool.scene.Node

/**
 * @author fabmax
 */

class Ray {
    val origin = MutableVec3f()
    val direction = MutableVec3f()

    fun setFromLookAt(origin: Vec3f, lookAt: Vec3f) {
        this.origin.set(origin)
        direction.set(lookAt).subtract(origin).norm()
    }

    fun nearestPointOnRay(result: MutableVec3f, point: Vec3f) {
        val d = (point.dot(direction) - origin.dot(direction)) / direction.dot(direction)
        if (d > 0) {
            result.set(direction).scale(d).add(origin)
        } else {
            result.set(origin)
        }
    }
}

class RayTest {

    val ray = Ray()

    val hitPosition = MutableVec3f()
    val hitPositionLocal = MutableVec3f()
    var hitNode: Node? = null
    var hitDistanceSqr = Float.POSITIVE_INFINITY
    val isHit: Boolean
        get() = hitDistanceSqr < Float.POSITIVE_INFINITY

    fun clear() {
        hitPosition.set(Vec3f.ZERO)
        hitPositionLocal.set(Vec3f.ZERO)
        hitNode = null
        hitDistanceSqr = Float.POSITIVE_INFINITY
    }

    fun computeHitPosition() {
        if (isHit) {
            val dist = Math.sqrt(hitDistanceSqr.toDouble()).toFloat()
            hitPosition.set(ray.direction).norm().scale(dist).add(ray.origin)
        }
    }
}