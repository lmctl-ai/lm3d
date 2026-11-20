package com.lmctl.lm3d.material;

import com.lmctl.lm3d.core.Color;

public final class MeshPhongMaterial extends Material {
    public final Color specular = new Color("#ffffff");
    public final Color emissive = new Color(0, 0, 0);
    public float shininess = 48;

    public MeshPhongMaterial(String color) {
        super(color);
    }

    @Override
    public boolean unlit() {
        return false;
    }
}
