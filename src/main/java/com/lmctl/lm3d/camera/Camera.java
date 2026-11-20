package com.lmctl.lm3d.camera;

import com.lmctl.lm3d.core.Object3D;
import com.lmctl.lm3d.math.Matrix4f;
import com.lmctl.lm3d.math.Vector3f;

public abstract class Camera extends Object3D {
    public final Matrix4f projectionMatrix = new Matrix4f();
    public final Matrix4f viewMatrix = new Matrix4f();
    public final Vector3f target = new Vector3f();
    public final Vector3f up = new Vector3f(0, 1, 0);

    public Camera lookAt(Vector3f value) {
        target.set(value);
        return this;
    }

    public Camera lookAt(float x, float y, float z) {
        target.set(x, y, z);
        return this;
    }

    public void updateViewMatrix() {
        updateWorldMatrix();
        viewMatrix.lookAt(worldPosition(), target, up);
    }

    public abstract void updateProjectionMatrix();
}
