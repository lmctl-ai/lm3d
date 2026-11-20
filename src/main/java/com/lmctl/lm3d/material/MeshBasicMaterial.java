package com.lmctl.lm3d.material;

public final class MeshBasicMaterial extends Material {
    public MeshBasicMaterial(String color) {
        super(color);
    }

    @Override
    public boolean unlit() {
        return true;
    }
}
