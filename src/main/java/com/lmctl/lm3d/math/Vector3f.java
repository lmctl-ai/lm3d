package com.lmctl.lm3d.math;

public final class Vector3f {
    public float x;
    public float y;
    public float z;

    public Vector3f() {
    }

    public Vector3f(float x, float y, float z) {
        set(x, y, z);
    }

    public Vector3f(Vector3f source) {
        set(source);
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3f set(Vector3f source) {
        return set(source.x, source.y, source.z);
    }

    public Vector3f copy() {
        return new Vector3f(this);
    }

    public Vector3f add(Vector3f value) {
        return set(x + value.x, y + value.y, z + value.z);
    }

    public Vector3f subtract(Vector3f value) {
        return set(x - value.x, y - value.y, z - value.z);
    }

    public Vector3f scale(float scalar) {
        return set(x * scalar, y * scalar, z * scalar);
    }

    public Vector3f addScaled(Vector3f value, float scalar) {
        return set(x + value.x * scalar, y + value.y * scalar, z + value.z * scalar);
    }

    public float dot(Vector3f value) {
        return x * value.x + y * value.y + z * value.z;
    }

    public Vector3f cross(Vector3f value) {
        return set(y * value.z - z * value.y, z * value.x - x * value.z,
                x * value.y - y * value.x);
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public float distanceTo(Vector3f value) {
        return copy().subtract(value).length();
    }

    public Vector3f normalize() {
        float length = length();
        return length > 0 ? scale(1 / length) : set(0, 0, 0);
    }

    public Vector3f applyMatrix4(Matrix4f matrix) {
        float[] e = matrix.elements();
        float w = e[3] * x + e[7] * y + e[11] * z + e[15];
        float newX = (e[0] * x + e[4] * y + e[8] * z + e[12]) / w;
        float newY = (e[1] * x + e[5] * y + e[9] * z + e[13]) / w;
        float newZ = (e[2] * x + e[6] * y + e[10] * z + e[14]) / w;
        return set(newX, newY, newZ);
    }
}
