package com.lmctl.lm3d.geometry;

public final class SphereGeometry extends BufferGeometry {
    public SphereGeometry(float radius, int widthSegments, int heightSegments) {
        this(radius, Math.max(3, widthSegments), Math.max(2, heightSegments), new Data(radius,
                Math.max(3, widthSegments), Math.max(2, heightSegments)));
        if (radius <= 0) {
            throw new IllegalArgumentException("Sphere radius must be positive");
        }
    }

    private SphereGeometry(float radius, int widthSegments, int heightSegments, Data data) {
        super(data.positions, data.normals, data.indices);
    }

    private static final class Data {
        final float[] positions;
        final float[] normals;
        char[] indices;

        Data(float radius, int widthSegments, int heightSegments) {
            int columns = widthSegments + 1;
            int rows = heightSegments + 1;
            positions = new float[columns * rows * 3];
            normals = new float[positions.length];
            indices = new char[widthSegments * heightSegments * 6];
            int vertex = 0;
            for (int y = 0; y <= heightSegments; y++) {
                float v = (float) y / heightSegments;
                float theta = v * (float) Math.PI;
                for (int x = 0; x <= widthSegments; x++) {
                    float u = (float) x / widthSegments;
                    float phi = u * (float) Math.PI * 2;
                    float nx = -(float) Math.cos(phi) * (float) Math.sin(theta);
                    float ny = (float) Math.cos(theta);
                    float nz = (float) Math.sin(phi) * (float) Math.sin(theta);
                    positions[vertex] = radius * nx;
                    normals[vertex++] = nx;
                    positions[vertex] = radius * ny;
                    normals[vertex++] = ny;
                    positions[vertex] = radius * nz;
                    normals[vertex++] = nz;
                }
            }
            int offset = 0;
            for (int y = 0; y < heightSegments; y++) {
                for (int x = 0; x < widthSegments; x++) {
                    char a = (char) (y * columns + x);
                    char b = (char) ((y + 1) * columns + x);
                    char c = (char) ((y + 1) * columns + x + 1);
                    char d = (char) (y * columns + x + 1);
                    if (y != 0) {
                        indices[offset++] = a;
                        indices[offset++] = b;
                        indices[offset++] = d;
                    }
                    if (y != heightSegments - 1) {
                        indices[offset++] = b;
                        indices[offset++] = c;
                        indices[offset++] = d;
                    }
                }
            }
            if (offset != indices.length) {
                char[] trimmed = new char[offset];
                System.arraycopy(indices, 0, trimmed, 0, offset);
                indices = trimmed;
            }
        }
    }
}
