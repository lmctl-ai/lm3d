package com.lmctl.lm3d.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lmctl.lm3d.camera.OrthographicCamera;
import com.lmctl.lm3d.camera.PerspectiveCamera;
import org.junit.jupiter.api.Test;

class MathTest {
    private static final float DELTA = 0.0001f;

    @Test
    void vectorOperationsPreserveExpectedDirections() {
        Vector3f x = new Vector3f(1, 0, 0);
        Vector3f y = new Vector3f(0, 1, 0);
        Vector3f z = x.copy().cross(y);
        assertVector(z, 0, 0, 1);
        assertEquals(1, z.length(), DELTA);
        assertEquals(0, x.dot(y), DELTA);
        assertVector(new Vector3f(3, 0, 4).normalize(), 0.6f, 0, 0.8f);
        assertEquals(5, new Vector3f(3, 0, 4).length(), DELTA);
    }

    @Test
    void composedTransformAppliesScaleRotationAndTranslation() {
        Matrix4f transform = new Matrix4f().compose(new Vector3f(2, 3, 4),
                new Vector3f(0, 0, (float) Math.PI / 2), new Vector3f(2, 2, 2));
        assertVector(new Vector3f(1, 0, 0).applyMatrix4(transform), 2, 5, 4);
    }

    @Test
    void inverseRestoresAWorldPoint() {
        Matrix4f transform = new Matrix4f().compose(new Vector3f(-3, 2, 8),
                new Vector3f(0.3f, -0.7f, 0.2f), new Vector3f(2, 3, 0.5f));
        Vector3f original = new Vector3f(1, 2, 3);
        Vector3f transformed = original.copy().applyMatrix4(transform);
        assertVector(transformed.applyMatrix4(transform.copy().invert()), 1, 2, 3);
    }

    @Test
    void viewMatrixLooksDownNegativeZ() {
        Matrix4f view = new Matrix4f().lookAt(new Vector3f(0, 0, 5), new Vector3f(), new Vector3f(0, 1, 0));
        assertVector(new Vector3f().applyMatrix4(view), 0, 0, -5);
    }

    @Test
    void perspectiveAndOrthographicCamerasUpdateTheirProjections() {
        PerspectiveCamera perspective = new PerspectiveCamera(60, 16f / 9, 0.1f, 100);
        assertEquals(0, perspective.projectionMatrix.elements()[15], DELTA);
        assertEquals(-1, perspective.projectionMatrix.elements()[11], DELTA);
        perspective.aspect = 1;
        perspective.updateProjectionMatrix();
        assertEquals(perspective.projectionMatrix.elements()[0], perspective.projectionMatrix.elements()[5], DELTA);

        OrthographicCamera orthographic = new OrthographicCamera(-2, 2, -1, 1, 0.1f, 20);
        assertEquals(0.5f, orthographic.projectionMatrix.elements()[0], DELTA);
        assertEquals(1, orthographic.projectionMatrix.elements()[5], DELTA);
    }

    @Test
    void singularMatricesAndInvalidProjectionBoundsAreRejected() {
        Matrix4f singular = new Matrix4f();
        singular.elements()[15] = 0;
        assertThrows(IllegalStateException.class, singular::invert);
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(60, 0, 0.1f, 100));
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(0, 1, 0.1f, 100));
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(180, 1, 0.1f, 100));
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(-10, 1, 0.1f, 100));
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(Float.NaN, 1, 0.1f, 100));
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(60, Float.NaN, 0.1f, 100));
        assertThrows(IllegalArgumentException.class, () -> new PerspectiveCamera(60, 1, 0.1f, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new OrthographicCamera(0, 0, -1, 1, 0.1f, 20));
    }

    @Test
    void columnMajorMatrixArrayHasExpectedTranslation() {
        Matrix4f matrix = new Matrix4f().compose(new Vector3f(4, 5, 6), new Vector3f(), new Vector3f(1, 1, 1));
        assertArrayEquals(new float[] {4, 5, 6}, new float[] {matrix.elements()[12], matrix.elements()[13], matrix.elements()[14]});
    }

    private static void assertVector(Vector3f value, float x, float y, float z) {
        assertEquals(x, value.x, DELTA);
        assertEquals(y, value.y, DELTA);
        assertEquals(z, value.z, DELTA);
    }
}
