package com.lmctl.lm3d.geometry;

public final class PlaneGeometry extends BufferGeometry {
    public PlaneGeometry(float width, float height) {
        super(new float[] {
                -width / 2, -height / 2, 0, width / 2, -height / 2, 0,
                width / 2, height / 2, 0, -width / 2, height / 2, 0
        }, new float[] {
                0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1
        }, new char[] {0, 1, 2, 0, 2, 3});
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Plane dimensions must be positive");
        }
    }
}
