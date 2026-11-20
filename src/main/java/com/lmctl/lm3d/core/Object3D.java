package com.lmctl.lm3d.core;

import com.lmctl.lm3d.math.Matrix4f;
import com.lmctl.lm3d.math.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Object3D {
    private final List<Object3D> children = new ArrayList<>();
    public final Vector3f position = new Vector3f();
    public final Vector3f rotation = new Vector3f();
    public final Vector3f scale = new Vector3f(1, 1, 1);
    public final Matrix4f matrix = new Matrix4f();
    public final Matrix4f matrixWorld = new Matrix4f();
    public String name = "";
    public boolean visible = true;

    public Object3D add(Object3D child) {
        if (child == null || child == this) {
            throw new IllegalArgumentException("A scene node cannot add itself or null");
        }
        children.add(child);
        return this;
    }

    public Object3D remove(Object3D child) {
        children.remove(child);
        return this;
    }

    public List<Object3D> children() {
        return Collections.unmodifiableList(children);
    }

    public Object3D updateWorldMatrix() {
        return updateWorldMatrix(null);
    }

    public Object3D updateWorldMatrix(Matrix4f parentMatrix) {
        matrix.compose(position, rotation, scale);
        if (parentMatrix == null) {
            matrixWorld.set(matrix);
        } else {
            matrixWorld.set(parentMatrix).multiply(matrix);
        }
        for (Object3D child : children) {
            child.updateWorldMatrix(matrixWorld);
        }
        return this;
    }

    public Vector3f worldPosition() {
        return new Vector3f(matrixWorld.elements()[12], matrixWorld.elements()[13], matrixWorld.elements()[14]);
    }
}
