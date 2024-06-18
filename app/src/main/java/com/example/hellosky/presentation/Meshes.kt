import glm_.vec3.Vec3

const val BASE_SIZE = 45f
const val ELONGATED_SIZE = 3.0 * BASE_SIZE
const val HEIGHT = 0.2f
const val WIDTH = 0.7f
fun createArrowMesh(): Mesh {
    val vertices = listOf(
        Vec3(0f, -ELONGATED_SIZE, 0f), // Elongated vertex
        Vec3(-BASE_SIZE * WIDTH, BASE_SIZE, -BASE_SIZE * HEIGHT),
        Vec3(BASE_SIZE * WIDTH, BASE_SIZE, -BASE_SIZE * HEIGHT),
        Vec3(0f, BASE_SIZE, BASE_SIZE * HEIGHT)
    )
    val indices = listOf(
        0, 2, 1,
        0, 3, 2,
        0, 1, 3,
        1, 2, 3
    )

    val newIndices = processMesh(vertices, indices)
    return Mesh(vertices, newIndices)
}

fun createCubeMesh(): Mesh {
    val vertices = listOf(
        Vec3(-BASE_SIZE, -BASE_SIZE, -BASE_SIZE), // 0: left bottom back
        Vec3(BASE_SIZE, -BASE_SIZE, -BASE_SIZE),  // 1: right bottom back
        Vec3(BASE_SIZE, BASE_SIZE, -BASE_SIZE),   // 2: right top back
        Vec3(-BASE_SIZE, BASE_SIZE, -BASE_SIZE),  // 3: left top back
        Vec3(-BASE_SIZE, -BASE_SIZE, BASE_SIZE),  // 4: left bottom front
        Vec3(BASE_SIZE, -BASE_SIZE, BASE_SIZE),   // 5: right bottom front
        Vec3(BASE_SIZE, BASE_SIZE, BASE_SIZE),    // 6: right top front
        Vec3(-BASE_SIZE, BASE_SIZE, BASE_SIZE)    // 7: left top front
    )
    val indices = listOf(
        0, 2, 1, 0, 3, 2, // Back face
        4, 6, 5, 4, 7, 6, // Front face
        3, 6, 2, 3, 7, 6, // Top face
        0, 5, 1, 0, 4, 5, // Bottom face
        0, 7, 3, 0, 4, 7, // Left face
        1, 6, 5, 1, 2, 6  // Right face
    )

    val newIndices = processMesh(vertices, indices)

    return Mesh(vertices, newIndices)
}

val ARROW_MESH = createArrowMesh()
val CUBE_MESH = createCubeMesh()

fun computeMeshCenter(vertices: List<Vec3>): Vec3 {
    val sum = vertices.fold(Vec3(0f, 0f, 0f)) { acc, v -> acc + v }
    return sum / vertices.size.toFloat()
}

fun isTriangleFacingWrongDirection(
    vertices: List<Vec3>,
    center: Vec3,
    i1: Int,
    i2: Int,
    i3: Int
): Boolean {
    val triangleCenter = computeTriangleCenter(vertices, i1, i2, i3)
    val directionToCenter = center - triangleCenter

    val edge1 = vertices[i2] - vertices[i1]
    val edge2 = vertices[i3] - vertices[i1]
    val normal = edge1.cross(edge2)

    return normal.dot(directionToCenter) > 0
}

fun Vec3.dot(other: Vec3): Float = this.x * other.x + this.y * other.y + this.z * other.z

fun processMesh(vertices: List<Vec3>, indices: List<Int>): List<Int> {
    val newIndices = indices.toMutableList()
    val center = computeMeshCenter(vertices)
    for (i in indices.indices step 3) {
        if (isTriangleFacingWrongDirection(
                vertices,
                center,
                indices[i],
                indices[i + 1],
                indices[i + 2]
            )
        ) {
            flipTriangleIndices(newIndices, i)
        }
    }
    return newIndices
}

fun flipTriangleIndices(indices: MutableList<Int>, index: Int) {
    val tmp = indices[index]
    indices[index] = indices[index + 2]
    indices[index + 2] = tmp
}

fun computeTriangleCenter(vertices: List<Vec3>, i1: Int, i2: Int, i3: Int): Vec3 {
    return (vertices[i1] + vertices[i2] + vertices[i3]) / 3f
}