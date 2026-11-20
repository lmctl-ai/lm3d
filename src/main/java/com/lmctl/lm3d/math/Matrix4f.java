package com.lmctl.lm3d.math;

public final class Matrix4f {
    private final float[] elements = new float[16];

    public Matrix4f() {
        identity();
    }

    public Matrix4f identity() {
        for (int i = 0; i < elements.length; i++) {
            elements[i] = i % 5 == 0 ? 1 : 0;
        }
        return this;
    }

    public Matrix4f copy() {
        Matrix4f result = new Matrix4f();
        result.set(this);
        return result;
    }

    public Matrix4f set(Matrix4f source) {
        System.arraycopy(source.elements, 0, elements, 0, elements.length);
        return this;
    }

    public float[] elements() {
        return elements;
    }

    public Matrix4f multiply(Matrix4f other) {
        float[] a = elements.clone();
        float[] b = other.elements;
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                float value = 0;
                for (int i = 0; i < 4; i++) {
                    value += a[i * 4 + row] * b[column * 4 + i];
                }
                elements[column * 4 + row] = value;
            }
        }
        return this;
    }

    public Matrix4f compose(Vector3f position, Vector3f rotation, Vector3f scale) {
        float sx = (float) Math.sin(rotation.x);
        float cx = (float) Math.cos(rotation.x);
        float sy = (float) Math.sin(rotation.y);
        float cy = (float) Math.cos(rotation.y);
        float sz = (float) Math.sin(rotation.z);
        float cz = (float) Math.cos(rotation.z);

        elements[0] = cy * cz * scale.x;
        elements[1] = (sx * sy * cz + cx * sz) * scale.x;
        elements[2] = (-cx * sy * cz + sx * sz) * scale.x;
        elements[3] = 0;
        elements[4] = -cy * sz * scale.y;
        elements[5] = (-sx * sy * sz + cx * cz) * scale.y;
        elements[6] = (cx * sy * sz + sx * cz) * scale.y;
        elements[7] = 0;
        elements[8] = sy * scale.z;
        elements[9] = -sx * cy * scale.z;
        elements[10] = cx * cy * scale.z;
        elements[11] = 0;
        elements[12] = position.x;
        elements[13] = position.y;
        elements[14] = position.z;
        elements[15] = 1;
        return this;
    }

    public Matrix4f perspective(float fovYDegrees, float aspect, float near, float far) {
        if (aspect == 0 || near <= 0 || far <= near) {
            throw new IllegalArgumentException("Perspective requires nonzero aspect and 0 < near < far");
        }
        float f = 1 / (float) Math.tan(Math.toRadians(fovYDegrees) / 2);
        identity();
        elements[0] = f / aspect;
        elements[5] = f;
        elements[10] = (far + near) / (near - far);
        elements[11] = -1;
        elements[14] = 2 * far * near / (near - far);
        elements[15] = 0;
        return this;
    }

    public Matrix4f orthographic(float left, float right, float bottom, float top, float near, float far) {
        if (left == right || bottom == top || near == far) {
            throw new IllegalArgumentException("Orthographic bounds must have nonzero size");
        }
        identity();
        elements[0] = 2 / (right - left);
        elements[5] = 2 / (top - bottom);
        elements[10] = -2 / (far - near);
        elements[12] = -(right + left) / (right - left);
        elements[13] = -(top + bottom) / (top - bottom);
        elements[14] = -(far + near) / (far - near);
        return this;
    }

    public Matrix4f lookAt(Vector3f eye, Vector3f target, Vector3f up) {
        Vector3f z = eye.copy().subtract(target).normalize();
        if (z.lengthSquared() == 0) {
            z.z = 1;
        }
        Vector3f x = up.copy().cross(z).normalize();
        if (x.lengthSquared() == 0) {
            z.add(new Vector3f(0.0001f, 0, 0)).normalize();
            x = up.copy().cross(z).normalize();
        }
        Vector3f y = z.copy().cross(x).normalize();

        elements[0] = x.x;
        elements[1] = y.x;
        elements[2] = z.x;
        elements[3] = 0;
        elements[4] = x.y;
        elements[5] = y.y;
        elements[6] = z.y;
        elements[7] = 0;
        elements[8] = x.z;
        elements[9] = y.z;
        elements[10] = z.z;
        elements[11] = 0;
        elements[12] = -x.dot(eye);
        elements[13] = -y.dot(eye);
        elements[14] = -z.dot(eye);
        elements[15] = 1;
        return this;
    }

    public Matrix4f invert() {
        float[] m = elements.clone();
        float[] inv = new float[16];
        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15]
                + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15]
                - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15]
                + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14]
                - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15]
                - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15]
                + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15]
                - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14]
                + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15]
                + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15]
                - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15]
                + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14]
                - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11]
                - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11]
                + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11]
                - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10]
                + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
        float determinant = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12];
        if (determinant == 0) {
            throw new IllegalStateException("Matrix is not invertible");
        }
        for (int i = 0; i < elements.length; i++) {
            elements[i] = inv[i] / determinant;
        }
        return this;
    }
}
