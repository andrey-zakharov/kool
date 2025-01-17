package de.fabmax.kool.platform.webgpu

enum class GPUAutoLayoutMode {
    auto
}

enum class GPUBufferBindingType(val typeName: String) {
    uniform("uniform"),
    storage("storage"),
    readOnlyStorage("read-only-storage")
}

enum class GPUCanvasAlphaMode {
    opaque,
    premultiplied
}

enum class GPUIndexFormat {
    uint16, uint32
}

enum class GPULoadOp {
    load,
    clear
}

enum class GPUPredefinedColorSpace {
    srgb
}

enum class GPUPrimitiveTopology(val topoName: String) {
    pointList("point-list"),
    lineList("line-list"),
    lineStrip("line-strip"),
    triangleList("triangle-list"),
    triangleStrip("triangle-strip")
}

enum class GPUStoreOp {
    store,
    discard
}

external class GPUTextureFormat

enum class GPUVertexFormat {
    uint8x2, uint8x4, sint8x2, sint8x4, unorm8x2, unorm8x4, snorm8x2, snorm8x4,
    uint16x2, uint16x4, sint16x2, sint16x4, unorm16x2, unorm16x4, snorm16x2, snorm16x4,
    float16x2, float16x4,
    float32, float32x2, float32x3, float32x4,
    uint32, uint32x2, uint32x3, uint32x4, sint32, sint32x2, sint32x3, sint32x4
}

enum class GPUVertexStepMode {
    vertex,
    instance
}
