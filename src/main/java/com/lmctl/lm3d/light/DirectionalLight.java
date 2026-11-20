package com.lmctl.lm3d.light;

import com.lmctl.lm3d.core.Object3D;

public final class DirectionalLight extends Light {
    public final Object3D target = new Object3D();

    public DirectionalLight(String color, float intensity) {
        super(color, intensity);
    }
}
