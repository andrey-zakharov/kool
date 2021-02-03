package de.fabmax.kool.physics

import de.fabmax.kool.math.Mat4f
import physx.PxRigidStatic

actual class RigidStatic actual constructor(pose: Mat4f) : RigidActor() {

    init {
        Physics.checkIsLoaded()
    }

    private val pxRigidStatic: PxRigidStatic

    init {
        pose.toPxTransform(pxPose)
        pxRigidStatic = Physics.physics.createRigidStatic(pxPose)
        pxRigidActor = pxRigidStatic
    }
}