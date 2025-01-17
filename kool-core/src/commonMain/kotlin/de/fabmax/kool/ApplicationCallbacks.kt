package de.fabmax.kool

interface ApplicationCallbacks {

    /**
     * Called when the app (window / browser tab) is about to close. Can return true to proceed with closing the app
     * or false to stop it.
     * This is particular useful to implement a dialogs like "There is unsaved stuff, are you sure you want to close
     * this app? Yes / no / maybe"
     * The default implementation simply returns true.
     */
    fun onWindowCloseRequest(ctx: KoolContext): Boolean = true

    /**
     * Called when the screen scale changes, e.g. because the window is moved onto another monitor with a different
     * scale. Returns the actual screen scale to apply.
     * This can be used to limit the actual screen scale to a reasonable value, e.g. something like
     *    return max(1f, newScale)
     * The default implementation simply returns newScale.
     */
    fun onWindowScaleChange(newScale: Float, ctx: KoolContext): Float = newScale

}