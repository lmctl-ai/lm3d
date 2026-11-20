package com.lmctl.lm3d.core;

import com.lmctl.lm3d.geometry.BufferGeometry;
import com.lmctl.lm3d.material.Material;

public final class Mesh extends Object3D {
    private BufferGeometry geometry;
    private Material material;

    public Mesh(BufferGeometry geometry, Material material) {
        setGeometry(geometry);
        setMaterial(material);
    }

    public BufferGeometry geometry() {
        return geometry;
    }

    public Mesh setGeometry(BufferGeometry geometry) {
        if (geometry == null) {
            throw new IllegalArgumentException("Mesh geometry must not be null");
        }
        this.geometry = geometry;
        return this;
    }

    public Material material() {
        return material;
    }

    public Mesh setMaterial(Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Mesh material must not be null");
        }
        this.material = material;
        return this;
    }
}
