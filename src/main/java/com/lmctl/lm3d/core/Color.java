package com.lmctl.lm3d.core;

public final class Color {
    public float r;
    public float g;
    public float b;

    public Color(String hex) {
        set(hex);
    }

    public Color(float r, float g, float b) {
        set(r, g, b);
    }

    public Color set(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    public Color set(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Color must not be null");
        }
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        if (value.length() == 3) {
            value = "" + value.charAt(0) + value.charAt(0) + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2);
        }
        if (value.length() != 6) {
            throw new IllegalArgumentException("Color must use #rgb or #rrggbb: " + hex);
        }
        try {
            return set(Integer.parseInt(value.substring(0, 2), 16) / 255f,
                    Integer.parseInt(value.substring(2, 4), 16) / 255f,
                    Integer.parseInt(value.substring(4, 6), 16) / 255f);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Color contains non-hexadecimal digits: " + hex, e);
        }
    }
}
