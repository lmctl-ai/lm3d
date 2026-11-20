package com.lmctl.lm3d.loader;

import com.lmctl.lm3d.core.Group;

public final class GltfModel {
    private final Group root;
    private final int meshCount;
    private final int vertexCount;
    private final int triangleCount;

    GltfModel(Group root, int meshCount, int vertexCount, int triangleCount) {
        this.root = root;
        this.meshCount = meshCount;
        this.vertexCount = vertexCount;
        this.triangleCount = triangleCount;
    }

    public Group root() {
        return root;
    }

    public int meshCount() {
        return meshCount;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int triangleCount() {
        return triangleCount;
    }
}
