package de.fabmax.kool.editor.ui

import de.fabmax.kool.input.PointerInput
import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*

class ContextPopupMenu<T: Any>(menu: SubMenuItem<T>? = null) : Composable {

    val menu = mutableStateOf(menu)

    private var screenPosPx = mutableStateOf(Vec2f.ZERO)
    private var contextItem = mutableStateOf<T?>(null)

    fun show(screenPosPx: Vec2f, contextItem: T) {
        this.screenPosPx.set(screenPosPx)
        this.contextItem.set(contextItem)
    }

    fun hide() {
        contextItem.set(null)
    }

    override fun UiScope.compose() {
        val pos = screenPosPx.use()
        val item = contextItem.use()
        val rootMenu = menu.use()
        if (item != null && rootMenu != null) {
            menu(rootMenu, pos, item)
        }
    }

    private fun UiScope.menu(rootMenu: SubMenuItem<T>, screenPosPx: Vec2f, contextItem: T) = Popup(screenPosPx.x, screenPosPx.y, layout = CellLayout) {
        modifier.backgroundColor(null)

        // close popup menu on any button event outside popup menu
        surface.onEachFrame {
            val ptr = PointerInput.primaryPointer
            if (ptr.isAnyButtonEvent && !uiNode.isInBounds(Vec2f(ptr.x.toFloat(), ptr.y.toFloat()))) {
                hide()
            }
        }

        menuList(rootMenu.menuItems.use(), contextItem, Dp.ZERO, Dp.ZERO, modifier.zLayer)
    }

    private fun UiScope.menuList(items: List<ContextMenuItem<T>>, contextItem: T, x: Dp, y: Dp, z: Int) {
        var subMenu by remember<SubMenuItem<T>?>(null)
        var subMenuNode by remember<UiNode?>(null)

        menuColumn(x, y, z) {
            items.forEach { item ->
                when (item) {
                    is MenuItem -> {
                        Row(width = Grow.Std) {
                            var isHovered by remember(false)
                            modifier
                                .padding(vertical = sizes.smallGap * 0.5f)
                                .onEnter {
                                    isHovered = true
                                    subMenu = null
                                    subMenuNode = null
                                }
                                .onExit { isHovered = false }
                                .onClick {
                                    item.action.invoke(contextItem)
                                    hide()
                                }

                            if (isHovered) {
                                modifier.background(RoundRectBackground(colors.hoverBg, sizes.smallGap))
                            }

                            Text(item.label) {
                                modifier
                                    .width(Grow.MinFit)
                                    .alignY(AlignmentY.Center)
                                    .margin(start = sizes.gap, end = sizes.largeGap)
                            }
                        }
                    }
                    is SubMenuItem -> {
                        Row(width = Grow.Std) {
                            var isHovered by remember(false)
                            modifier
                                .padding(vertical = sizes.smallGap * 0.5f)
                                .onEnter {
                                    isHovered = true
                                    subMenu = item
                                    subMenuNode = uiNode
                                }
                                .onExit { isHovered = false }

                            if (isHovered || subMenu == item) {
                                modifier.background(RoundRectBackground(colors.hoverBg, sizes.smallGap))
                            }

                            Text(item.label ?: "Sub menu") {
                                modifier
                                    .width(Grow.MinFit)
                                    .alignY(AlignmentY.Center)
                                    .margin(horizontal = sizes.gap)
                            }
                            Arrow {
                                modifier
                                    .alignY(AlignmentY.Center)
                                    .margin(horizontal = sizes.gap)
                            }
                        }
                    }
                    is Divider -> {
                        divider(marginTop = sizes.smallGap * 0.5f, marginBottom = sizes.smallGap * 0.5f, thickness = Dp.fromPx(1f))
                    }
                }
            }
        }

        val subMenuItems = subMenu?.menuItems?.use()
        val anchorNode = subMenuNode
        if (!subMenuItems.isNullOrEmpty() && anchorNode != null) {
            val subPos = uiNode.toLocal(anchorNode.rightPx, anchorNode.topPx)
            menuList(
                subMenuItems,
                contextItem,
                Dp.fromPx(subPos.x) - sizes.smallGap,
                Dp.fromPx(subPos.y) - sizes.smallGap,
                z + UiSurface.LAYER_POPUP
            )
        }
    }

    private inline fun UiScope.menuColumn(x: Dp, y: Dp, z: Int, block: ColumnScope.() -> Unit) = Column {
        modifier
            .margin(start = x, top = y)
            .background(RoundRectBackground(colors.background, sizes.smallGap))
            .border(RoundRectBorder(colors.secondaryVariant, sizes.smallGap, Dp.fromPx(1f)))
            .padding(sizes.smallGap)
            .zLayer(z)
        block()
    }
}

sealed class ContextMenuItem<T: Any>

class Divider<T: Any> : ContextMenuItem<T>()

class MenuItem<T: Any>(
    val label: String,
    val action: ((T) -> Unit),
) : ContextMenuItem<T>()

class SubMenuItem<T: Any>(val label: String?) : ContextMenuItem<T>() {
    val menuItems: MutableStateList<ContextMenuItem<T>> = mutableStateListOf()

    fun item(label: String, action: (T) -> Unit) {
        menuItems += MenuItem(label, action)
    }

    fun subMenu(label: String, block: SubMenuItem<T>.() -> Unit) {
        val subMenu = SubMenuItem<T>(label)
        subMenu.block()
        menuItems += subMenu
    }

    fun divider() {
        menuItems += Divider()
    }
}

fun <T: Any> SubMenuItem(label: String? = null, block: SubMenuItem<T>.() -> Unit): SubMenuItem<T> {
    val menu = SubMenuItem<T>(label)
    menu.block()
    return menu
}
