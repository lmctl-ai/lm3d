package com.lmctl.lm3d.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lmctl.lm3d.core.Group;
import com.lmctl.lm3d.core.Mesh;
import com.lmctl.lm3d.lmcode.Lm3dHost;
import com.lmctl.lm3d.material.MeshPhongMaterial;
import io.lmctl.lmcode.Context;
import io.lmctl.lmcode.Registry;
import io.lmctl.lmcode.Value;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LmcodeScenesTest {
    private static final float DELTA = 0.0001f;

    @Test
    void cubeHandlerBuildsExpectedMeshThroughJavaHosts() {
        AtomicReference<Group> root = new AtomicReference<>();
        Lm3dHost host = LmcodeScenes.run("cube", root::set);
        assertTrue(host.used());
        assertEquals(1, host.meshCount());
        assertEquals(24, host.vertexCount());
        assertEquals(1, root.get().children().size());
        Mesh cube = (Mesh) root.get().children().get(0);
        assertEquals(1.25f, cube.position.y, DELTA);
        assertEquals(0.4f, cube.rotation.y, DELTA);
        assertEquals(72, ((MeshPhongMaterial) cube.material()).shininess, DELTA);
    }

    @Test
    void hierarchyHandlerBuildsThreeChildren() {
        AtomicReference<Group> root = new AtomicReference<>();
        Lm3dHost host = LmcodeScenes.run("hierarchy", root::set);
        assertEquals(3, host.meshCount());
        assertEquals(1010, host.vertexCount());
        assertEquals(3, root.get().children().size());
        assertEquals(-1.35f, root.get().children().get(0).position.x, DELTA);
        assertEquals(1.35f, root.get().children().get(1).position.x, DELTA);
        assertEquals(-1.5f, root.get().children().get(2).position.z, DELTA);
    }

    @Test
    void materialHandlerAppliesPhongProperties() {
        AtomicReference<Group> root = new AtomicReference<>();
        Lm3dHost host = LmcodeScenes.run("materials", root::set);
        assertEquals(2, host.meshCount());
        assertEquals(2050, host.vertexCount());
        MeshPhongMaterial sphere = (MeshPhongMaterial) ((Mesh) root.get().children().get(0)).material();
        MeshPhongMaterial ring = (MeshPhongMaterial) ((Mesh) root.get().children().get(1)).material();
        assertEquals(96, sphere.shininess, DELTA);
        assertEquals(1, sphere.specular.r, DELTA);
        assertEquals(16 / 255f, ring.emissive.r, DELTA);
        assertEquals(44 / 255f, ring.emissive.g, DELTA);
        assertEquals(40 / 255f, ring.emissive.b, DELTA);
    }

    @Test
    void missingAnnotationIsRejected() {
        assertThrows(IllegalStateException.class, () -> LmcodeScenes.run("missing", root -> { }));
    }

    @Test
    void failureAfterUseIsReported() {
        AtomicReference<Group> staged = new AtomicReference<>();
        assertThrows(RuntimeException.class, () -> LmcodeScenes.run("failure", staged::set));
        assertTrue(staged.get() != null);
    }

    @Test
    void indirectSceneCyclesAreRejected() {
        Registry registry = new Registry(false);
        new Lm3dHost(registry, root -> { });
        Context context = new Context();
        Value first = registry.resolve("fn lm3d_group() -> int").callback.call(context, Value.none(), new Value[0]);
        Value second = registry.resolve("fn lm3d_group() -> int").callback.call(context, Value.none(), new Value[0]);
        var add = registry.resolve("fn lm3d_add(int, int) -> void").callback;
        add.call(context, Value.none(), new Value[] {first, second});
        assertThrows(IllegalArgumentException.class,
                () -> add.call(context, Value.none(), new Value[] {second, first}));
    }
}
