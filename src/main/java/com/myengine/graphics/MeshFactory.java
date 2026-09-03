package com.myengine.graphics;

/** Generates simple primitive meshes so you're not hand-typing vertex arrays for every demo. */
public class MeshFactory {

    /** A 1x1x1 cube centered on the origin, with correct per-face normals for lighting. */
    public static Mesh createCube() {
        float[] vertices = {
                // positions            // normals
                // Front (+Z)
                -0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, -0.5f, 0.5f, 0, 0, 1,
                0.5f, 0.5f, 0.5f, 0, 0, 1,
                -0.5f, 0.5f, 0.5f, 0, 0, 1,
                // Back (-Z)
                -0.5f, -0.5f, -0.5f, 0, 0, -1,
                0.5f, -0.5f, -0.5f, 0, 0, -1,
                0.5f, 0.5f, -0.5f, 0, 0, -1,
                -0.5f, 0.5f, -0.5f, 0, 0, -1,
                // Left (-X)
                -0.5f, -0.5f, -0.5f, -1, 0, 0,
                -0.5f, -0.5f, 0.5f, -1, 0, 0,
                -0.5f, 0.5f, 0.5f, -1, 0, 0,
                -0.5f, 0.5f, -0.5f, -1, 0, 0,
                // Right (+X)
                0.5f, -0.5f, -0.5f, 1, 0, 0,
                0.5f, -0.5f, 0.5f, 1, 0, 0,
                0.5f, 0.5f, 0.5f, 1, 0, 0,
                0.5f, 0.5f, -0.5f, 1, 0, 0,
                // Top (+Y)
                -0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, 0.5f, 0, 1, 0,
                0.5f, 0.5f, -0.5f, 0, 1, 0,
                -0.5f, 0.5f, -0.5f, 0, 1, 0,
                // Bottom (-Y)
                -0.5f, -0.5f, 0.5f, 0, -1, 0,
                0.5f, -0.5f, 0.5f, 0, -1, 0,
                0.5f, -0.5f, -0.5f, 0, -1, 0,
                -0.5f, -0.5f, -0.5f, 0, -1, 0,
        };

        int[] indices = {
                0, 1, 2, 2, 3, 0,        // front
                5, 4, 7, 7, 6, 5,        // back
                8, 9, 10, 10, 11, 8,     // left
                13, 12, 15, 15, 14, 13,  // right
                16, 17, 18, 18, 19, 16,  // top
                21, 20, 23, 23, 22, 21,  // bottom
        };

        return new Mesh(vertices, indices);
    }
}
