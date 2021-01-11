package de.fabmax.kool.physics.shapes

import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.math.MutableVec4f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.physics.Physics
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.util.BoundingBox
import de.fabmax.kool.util.IndexedVertexList
import physx.*

actual class ConvexHullShape actual constructor(points: List<Vec3f>) : CommonConvexHullShape(points), CollisionShape {

    private val pxMesh: PxConvexMesh
    private val bounds = BoundingBox()

    override val geometry: IndexedVertexList

    init {
        Physics.checkIsLoaded()

        pxMesh = toConvexMesh(points)
        geometry = makeMeshData(pxMesh)

        bounds.add(points)
    }

    override fun getAabb(result: BoundingBox) = result.set(bounds)
    override fun getBoundingSphere(result: MutableVec4f) = result.set(bounds.center, bounds.size.length() / 2)

    override fun attachTo(actor: PxRigidActor, material: PxMaterial, flags: PxShapeFlags, collisionFilter: PxFilterData): PxShape {
        val scaling = PhysX.PxMeshScale(Vec3f(1f).toPxVec3(), Vec4f(0f, 0f, 0f, 1f).toPxQuat())
        val meshFlags = PhysX.PxConvexMeshGeometryFlags(PhysX.PxConvexMeshGeometryFlag.eTIGHT_BOUNDS)
        val geometry = PhysX.PxConvexMeshGeometry(pxMesh, scaling, meshFlags)
        val shape = PhysX.physics.createShape(geometry, material, true, flags)
        shape.setSimulationFilterData(collisionFilter)
        actor.attachShape(shape)
        return shape
    }

    companion object {
        fun toConvexMesh(points: List<Vec3f>): PxConvexMesh {
            val vec3Vector = points.toVectorPxVec3()
            val desc = PhysX.PxConvexMeshDesc()
            desc.flags = PhysX.PxConvexFlags(PhysX.PxConvexFlag.eCOMPUTE_CONVEX)
            desc.points.count = points.size
            desc.points.stride = 3 * 4      // point consists of 3 floats with 4 bytes each
            desc.points.data = vec3Vector.data()
            return PhysX.cooking.createConvexMesh(desc, PhysX.physics.getPhysicsInsertionCallback())
        }

        fun makeMeshData(convexMesh: PxConvexMesh): IndexedVertexList {
            val geometry = IndexedVertexList(Attribute.POSITIONS, Attribute.NORMALS)

            val v = MutableVec3f()
            val polyIndices = mutableListOf<Int>()
            val poly = PhysX.PxHullPolygon()
            for (i in 0 until convexMesh.getNbPolygons()) {
                polyIndices.clear()

                convexMesh.getPolygonData(i, poly)
                for (j in 0 until poly.mNbVerts) {
                    val vi = PhysX.Px.getU8At(convexMesh.getIndexBuffer(), poly.mIndexBase + j)
                    val pt = PhysX.Px.getVec3At(convexMesh.getVertices(), vi)
                    polyIndices += geometry.addVertex(pt.toVec3f(v))
                }

                for (j in 2 until polyIndices.size) {
                    val v0 = polyIndices[0]
                    val v1 = polyIndices[j - 1]
                    val v2 = polyIndices[j]
                    geometry.addTriIndices(v0, v1, v2)
                }
            }
            geometry.generateNormals()
            return geometry
        }
    }

    override fun estimateInertiaForMass(mass: Float, result: MutableVec3f): MutableVec3f {
        // rough approximation: use inertia of bounding box
        result.x = (mass / 12f) * (bounds.size.y * bounds.size.y + bounds.size.z * bounds.size.z)
        result.z = (mass / 12f) * (bounds.size.x * bounds.size.x + bounds.size.y * bounds.size.y)
        result.y = (mass / 12f) * (bounds.size.x * bounds.size.x + bounds.size.z * bounds.size.z)
        return result
    }
}