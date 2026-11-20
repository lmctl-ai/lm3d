package com.lmctl.lm3d.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lmctl.lm3d.geometry.BoxGeometry;
import com.lmctl.lm3d.material.MeshBasicMaterial;
import org.junit.jupiter.api.Test;

class SceneGraphTest {
    private static final float DELTA = 0.0001f;

    @Test
    void childrenInheritParentTransforms() {
        Scene scene = new Scene();
        Group group = new Group();
        group.position.set(2, 1, -1);
        group.rotation.y = 0.4f;
        Mesh mesh = new Mesh(new BoxGeometry(1, 1, 1), new MeshBasicMaterial("#ffffff"));
        mesh.position.set(1, 2, 3);
        scene.add(group);
        group.add(mesh);
        scene.updateWorldMatrix();
        com.lmctl.lm3d.math.Vector3f expected = new com.lmctl.lm3d.math.Vector3f(1, 2, 3)
                .applyMatrix4(group.matrixWorld);
        assertEquals(expected.x, mesh.worldPosition().x, DELTA);
        assertEquals(expected.y, mesh.worldPosition().y, DELTA);
        assertEquals(expected.z, mesh.worldPosition().z, DELTA);
        assertEquals(1, group.children().size());
    }

    @Test
    void colorsAcceptShortAndLongHexForms() {
        Color shortColor = new Color("#0f8");
        assertEquals(0, shortColor.r, DELTA);
        assertEquals(1, shortColor.g, DELTA);
        assertEquals(136 / 255f, shortColor.b, DELTA);
        Color longColor = new Color("61d6c5");
        assertEquals(97 / 255f, longColor.r, DELTA);
        assertEquals(214 / 255f, longColor.g, DELTA);
        assertEquals(197 / 255f, longColor.b, DELTA);
        assertThrows(IllegalArgumentException.class, () -> new Color("red"));
        assertThrows(IllegalArgumentException.class, () -> new Color("#xyzxyz"));
    }

    @Test
    void invalidChildrenAndMeshesAreRejected() {
        Object3D node = new Object3D();
        assertThrows(IllegalArgumentException.class, () -> node.add(node));
        assertThrows(IllegalArgumentException.class, () -> node.add(null));
        assertThrows(IllegalArgumentException.class, () -> new Mesh(null, new MeshBasicMaterial("#fff")));
        assertThrows(IllegalArgumentException.class, () -> new Mesh(new BoxGeometry(1, 1, 1), null));
    }
}
