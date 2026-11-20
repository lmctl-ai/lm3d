package com.lmctl.lm3d.light;

public final class PointLight extends Light {
    public float distance;

    public PointLight(String color, float intensity, float distance) {
        super(color, intensity);
        this.distance = distance;
    }
}
