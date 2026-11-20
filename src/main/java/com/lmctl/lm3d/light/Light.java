package com.lmctl.lm3d.light;

import com.lmctl.lm3d.core.Color;
import com.lmctl.lm3d.core.Object3D;

public abstract class Light extends Object3D {
    public final Color color;
    public float intensity;

    protected Light(String color, float intensity) {
        this.color = new Color(color);
        this.intensity = intensity;
    }
}
