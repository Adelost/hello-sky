
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import glm_.mat4x4.Mat4
import glm_.quat.Quat
import glm_.vec3.Vec3
import glm_.vec4.Vec4

data class Mesh(val vertices: List<Vec3>, val indices: List<Int>)

@Composable
fun MeshView(mesh: Mesh, rotation: Quat, drawEdges: Boolean = false) {
    Canvas(modifier = Modifier.size(110.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        val rotationMatrix = calculateRotationMatrix(rotation.eulerAngles())
        val projectedVertices = mesh.vertices.map { vertex ->
            projectIsometric3D(vertex, rotationMatrix, centerX, centerY)
        }

        val faces = mesh.indices.chunked(3) {
            Face(projectedVertices[it[0]], projectedVertices[it[1]], projectedVertices[it[2]])
        }.map { face ->
            FaceCentered(face, computeCenter(face))
        }.sortedBy { it.center.z }
            .map { it.face }

        faces.forEach { face -> drawFace(face, drawEdges) }
    }
}

private fun DrawScope.drawFace(face: Face, drawEdges: Boolean) {
    val faceVertices = listOf(face.a, face.b, face.c).map { Offset(it.x, it.y) }
    val normal = calculateNormal(face.a, face.b, face.c)

    if (normal.z <= 0) return

    val baseColor = Color.Red
    val color = shadeColor(baseColor, normal)

    drawPath(
        path = Path().apply {
            moveTo(faceVertices[0].x, faceVertices[0].y)
            faceVertices.forEach { vertex -> lineTo(vertex.x, vertex.y) }
            close()
        },
        color = color
    )

    if (drawEdges) {
        drawEdges(faceVertices)
    }
}

private fun DrawScope.drawEdges(faceVertices: List<Offset>) {
    val color = Color(0x22000000)
    faceVertices.zipWithNext { a, b ->
        drawLine(color, a, b, strokeWidth = 1f)
    }
    drawLine(
        color,
        faceVertices.last(),
        faceVertices.first(),
        strokeWidth = 1f
    )
}

fun projectIsometric3D(vertex: Vec3, rotationMatrix: Mat4, centerX: Float, centerY: Float): Vec3 {

    val rotatedVertex = Vec3(rotationMatrix * Vec4(vertex, 1f))
    return Vec3(centerX + rotatedVertex.x, centerY - rotatedVertex.y, rotatedVertex.z)
}

fun calculateRotationMatrix(rotation: Vec3): Mat4 {
    val rotX = Mat4().rotate(-rotation.z, 1f, 0f, 0f)
    val rotY = Mat4().rotate(rotation.y, 0f, 1f, 0f)
    val rotZ = Mat4().rotate(rotation.x, 0f, 0f, 1f)
    return rotX * rotY * rotZ
}

private fun shadeColorTransparent(color: Color, normal: Vec3): Color {
    val lightDirection = Vec3(1f, 1f, 1f).normalize()
    val dotProduct = normal.dot(lightDirection).coerceIn(0f, 1f)
    return color.copy(alpha = 0.3f + 0.7f * dotProduct)
}

private fun shadeColor(color: Color, normal: Vec3): Color {
    val lightDirection = Vec3(0f, 0f, 1f).normalize()
    val dotProduct = normal.dot(lightDirection).coerceIn(0.7f, 1f)
    return color.copy(
        red = color.red * dotProduct,
        green = color.green * dotProduct,
        blue = color.blue * dotProduct,
        alpha = 1f // Set to 1 for full opacity
    )
}

private fun computeCenter(face: Face): Vec3 {
    val centerX = (face.a.x + face.b.x + face.c.x) / 3
    val centerY = (face.a.y + face.b.y + face.c.y) / 3
    val centerZ = (face.a.z + face.b.z + face.c.z) / 3
    return Vec3(centerX, centerY, centerZ)
}

private fun calculateNormal(v0: Vec3, v1: Vec3, v2: Vec3): Vec3 {
    val dir1 = v1 - v0
    val dir2 = v2 - v0
    return dir1.cross(dir2).normalize()
}

data class Face(val a: Vec3, val b: Vec3, val c: Vec3)
data class FaceCentered(val face: Face, val center: Vec3)