package com.lmctl.lm3d.geometry;

public final class TorusGeometry extends BufferGeometry {
    public TorusGeometry(float radius, float tube, int radialSegments, int tubularSegments) {
        this(Math.max(3, radialSegments), Math.max(3, tubularSegments),
                new Data(radius, tube, Math.max(3, radialSegments), Math.max(3, tubularSegments)));
        if (radius <= 0 || tube <= 0 || tube >= radius) {
            throw new IllegalArgumentException("Torus requires radius > tube > 0");
        }
    }

    private TorusGeometry(int radialSegments, int tubularSegments, Data data) {
        super(data.positions, data.normals, data.indices);
    }

    private static final class Data {
        final float[] positions;
        final float[] normals;
        final char[] indices;

        Data(float radius, float tube, int radialSegments, int tubularSegments) {
            int columns = radialSegments + 1;
            int rows = tubularSegments + 1;
            positions = new float[columns * rows * 3];
            normals = new float[positions.length];
            indices = new char[radialSegments * tubularSegments * 6];
            int offset = 0;
            for (int j = 0; j <= tubularSegments; j++) {
                float v = (float) j / tubularSegments * (float) Math.PI * 2;
                float cosV = (float) Math.cos(v);
                float sinV = (float) Math.sin(v);
                for (int i = 0; i <= radialSegments; i++) {
                    float u = (float) i / radialSegments * (float) Math.PI * 2;
                    float cosU = (float) Math.cos(u);
                    float sinU = (float) Math.sin(u);
                    positions[offset] = (radius + tube * cosV) * cosU;
                    normals[offset++] = cosV * cosU;
                    positions[offset] = tube * sinV;
                    normals[offset++] = sinV;
                    positions[offset] = (radius + tube * cosV) * sinU;
                    normals[offset++] = cosV * sinU;
                }
            }
            offset = 0;
            for (int j = 0; j < tubularSegments; j++) {
                for (int i = 0; i < radialSegments; i++) {
                    char a = (char) (j * columns + i);
                    char b = (char) (j * columns + i + 1);
                    char c = (char) ((j + 1) * columns + i + 1);
                    char d = (char) ((j + 1) * columns + i);
                    indices[offset++] = a;
                    indices[offset++] = d;
                    indices[offset++] = b;
                    indices[offset++] = b;
                    indices[offset++] = d;
                    indices[offset++] = c;
                }
            }
        }
    }
}
