package com.lmctl.lm3d.camera;

public final class OrthographicCamera extends Camera {
    public float left;
    public float right;
    public float bottom;
    public float top;
    public float near;
    public float far;

    public OrthographicCamera(float left, float right, float bottom, float top, float near, float far) {
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.top = top;
        this.near = near;
        this.far = far;
        updateProjectionMatrix();
    }

    @Override
    public void updateProjectionMatrix() {
        projectionMatrix.orthographic(left, right, bottom, top, near, far);
    }
}
