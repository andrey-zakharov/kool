package de.fabmax.kool.physics

import kotlinx.coroutines.CoroutineScope

expect object Physics : CoroutineScope {

    val isLoaded: Boolean

    val defaultMaterial: Material

    fun loadPhysics()

    suspend fun awaitLoaded()

    val NOTIFY_TOUCH_FOUND: Int
    val NOTIFY_TOUCH_LOST: Int
    val NOTIFY_CONTACT_POINTS: Int

}