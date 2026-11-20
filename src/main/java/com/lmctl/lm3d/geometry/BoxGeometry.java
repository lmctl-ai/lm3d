package com.lmctl.lm3d.geometry;

public final class BoxGeometry extends BufferGeometry {
    public BoxGeometry(float width, float height, float depth) {
        super(positions(width, height, depth), createNormals(), indices(width, height, depth));
        if (width <= 0 || height <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Box dimensions must be positive");
        }
    }

    private static float[] positions(float width, float height, float depth) {
        float x = width / 2;
        float y = height / 2;
        float z = depth / 2;
        float[][][] faces = {
            {{-x,-y,z},{x,-y,z},{x,y,z},{-x,y,z}},
            {{x,-y,-z},{-x,-y,-z},{-x,y,-z},{x,y,-z}},
            {{x,-y,z},{x,-y,-z},{x,y,-z},{x,y,z}},
            {{-x,-y,-z},{-x,-y,z},{-x,y,z},{-x,y,-z}},
            {{-x,y,z},{x,y,z},{x,y,-z},{-x,y,-z}},
            {{-x,-y,-z},{x,-y,-z},{x,-y,z},{-x,-y,z}}
        };
        float[] result = new float[72];
        int offset = 0;
        for (float[][] face : faces) {
            for (float[] vertex : face) {
                System.arraycopy(vertex, 0, result, offset, 3);
                offset += 3;
            }
        }
        return result;
    }

    private static float[] createNormals() {
        float[][] faceNormals = {{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};
        float[] result = new float[72];
        int offset = 0;
        for (float[] normal : faceNormals) {
            for (int vertex = 0; vertex < 4; vertex++) {
                System.arraycopy(normal, 0, result, offset, 3);
                offset += 3;
            }
        }
        return result;
    }

    private static char[] indices(float width, float height, float depth) {
        char[] result = new char[36];
        for (int face = 0; face < 6; face++) {
            char start = (char) (face * 4);
            int offset = face * 6;
            result[offset] = start;
            result[offset + 1] = (char) (start + 1);
            result[offset + 2] = (char) (start + 2);
            result[offset + 3] = start;
            result[offset + 4] = (char) (start + 2);
            result[offset + 5] = (char) (start + 3);
        }
        return result;
    }
}
