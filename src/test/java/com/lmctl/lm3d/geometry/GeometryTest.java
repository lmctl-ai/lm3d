package com.lmctl.lm3d.geometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GeometryTest {
    private static final float DELTA = 0.0001f;

    @Test
    void boxHasIndependentFaceVerticesAndTwelveTriangles() {
        BufferGeometry geometry = new BoxGeometry(2, 4, 6);
        assertEquals(24, geometry.vertexCount());
        assertEquals(12, geometry.triangleCount());
        assertEquals(72, geometry.normals().length);
        assertEquals(36, geometry.indices().length);
    }

    @Test
    void sphereAndTorusGenerateUnitNormalsAndExpectedCounts() {
        BufferGeometry sphere = new SphereGeometry(1.35f, 32, 24);
        assertEquals(825, sphere.vertexCount());
        assertEquals(1472, sphere.triangleCount());
        assertUnitNormals(sphere);

        BufferGeometry torus = new TorusGeometry(1.15f, 0.42f, 48, 24);
        assertEquals(1225, torus.vertexCount());
        assertEquals(2304, torus.triangleCount());
        assertUnitNormals(torus);
    }

    @Test
    void planeHasFourVerticesAndTwoTriangles() {
        BufferGeometry plane = new PlaneGeometry(18, 18);
        assertEquals(4, plane.vertexCount());
        assertEquals(2, plane.triangleCount());
        assertArrayEquals(new char[] {0, 1, 2, 0, 2, 3}, plane.indices());
    }

    @Test
    void geometryArraysAreDefensivelyCopied() {
        float[] positions = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] normals = {0, 0, 1, 0, 0, 1, 0, 0, 1};
        char[] indices = {0, 1, 2};
        BufferGeometry geometry = new BufferGeometry(positions, normals, indices);
        positions[0] = 99;
        assertEquals(0, geometry.positions()[0], DELTA);
        float[] result = geometry.positions();
        result[0] = 88;
        assertEquals(0, geometry.positions()[0], DELTA);
    }

    @Test
    void malformedGeometryAndDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new BufferGeometry(new float[3], new float[3], new char[] {0, 1, 2}));
        assertThrows(IllegalArgumentException.class, () -> new BoxGeometry(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new SphereGeometry(-1, 16, 8));
        assertThrows(IllegalArgumentException.class, () -> new TorusGeometry(1, 1, 16, 8));
        assertThrows(IllegalArgumentException.class, () -> new PlaneGeometry(0, 1));
    }

    private static void assertUnitNormals(BufferGeometry geometry) {
        float[] normals = geometry.normals();
        for (int i = 0; i < normals.length; i += 3) {
            float length = (float) Math.sqrt(normals[i] * normals[i] + normals[i + 1] * normals[i + 1]
                    + normals[i + 2] * normals[i + 2]);
            assertEquals(1, length, DELTA);
        }
    }
}
