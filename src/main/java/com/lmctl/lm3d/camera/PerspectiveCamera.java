package com.lmctl.lm3d.camera;

public final class PerspectiveCamera extends Camera {
    public float fov;
    public float aspect;
    public float near;
    public float far;

    public PerspectiveCamera(float fov, float aspect, float near, float far) {
        this.fov = fov;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
        updateProjectionMatrix();
    }

    @Override
    public void updateProjectionMatrix() {
        projectionMatrix.perspective(fov, aspect, near, far);
    }
}
