package com.lmctl.lm3d.material;

import com.lmctl.lm3d.core.Color;

public abstract class Material {
    public final Color color;
    public float opacity = 1;

    protected Material(String color) {
        this.color = new Color(color);
    }

    public abstract boolean unlit();
}
