package com.lmctl.lm3d.geometry;

public class BufferGeometry {
    private final float[] positions;
    private final float[] normals;
    private final char[] indices;

    public BufferGeometry(float[] positions, float[] normals, char[] indices) {
        if (positions == null || normals == null || indices == null
                || positions.length == 0 || positions.length % 3 != 0
                || normals.length != positions.length || indices.length % 3 != 0) {
            throw new IllegalArgumentException("Geometry needs positions, matching normals, and triangle indices");
        }
        int vertexCount = positions.length / 3;
        if (vertexCount > 65536) {
            throw new IllegalArgumentException("WebGL 1 indexed geometry is limited to 65536 vertices");
        }
        for (char index : indices) {
            if (index >= vertexCount) {
                throw new IllegalArgumentException("Geometry index is outside the vertex array");
            }
        }
        this.positions = positions.clone();
        this.normals = normals.clone();
        this.indices = indices.clone();
    }

    public float[] positions() {
        return positions.clone();
    }

    public float[] normals() {
        return normals.clone();
    }

    public char[] indices() {
        return indices.clone();
    }

    public int vertexCount() {
        return positions.length / 3;
    }

    public int triangleCount() {
        return indices.length / 3;
    }
}
