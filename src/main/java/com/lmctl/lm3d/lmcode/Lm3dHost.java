package com.lmctl.lm3d.lmcode;

import com.lmctl.lm3d.core.Group;
import com.lmctl.lm3d.core.Mesh;
import com.lmctl.lm3d.core.Object3D;
import com.lmctl.lm3d.geometry.BoxGeometry;
import com.lmctl.lm3d.geometry.SphereGeometry;
import com.lmctl.lm3d.geometry.TorusGeometry;
import com.lmctl.lm3d.material.MeshPhongMaterial;
import io.lmctl.lmcode.Registry;
import io.lmctl.lmcode.Value;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Lm3dHost {
    private final Map<Integer, Object3D> nodes = new HashMap<>();
    private final Consumer<Group> rootConsumer;
    private int nextId = 1;
    private int meshCount;
    private int vertexCount;
    private boolean used;

    public Lm3dHost(Registry registry, Consumer<Group> rootConsumer) {
        if (registry == null || rootConsumer == null) {
            throw new IllegalArgumentException("LMCode registry and root consumer must not be null");
        }
        this.rootConsumer = rootConsumer;
        registry.function("lm3d_group", List.of(), "int", (context, self, args) -> Value.ofInt(add(new Group())));
        registry.function("lm3d_box", List.of("float", "float", "float", "string"), "int",
                (context, self, args) -> Value.ofInt(mesh(new BoxGeometry(number(args[0]), number(args[1]), number(args[2])),
                        string(args[3]))));
        registry.function("lm3d_sphere", List.of("float", "int", "int", "string"), "int",
                (context, self, args) -> Value.ofInt(mesh(new SphereGeometry(number(args[0]), integer(args[1]),
                        integer(args[2])), string(args[3]))));
        registry.function("lm3d_torus", List.of("float", "float", "int", "int", "string"), "int",
                (context, self, args) -> Value.ofInt(mesh(new TorusGeometry(number(args[0]), number(args[1]),
                        integer(args[2]), integer(args[3])), string(args[4]))));
        registry.function("lm3d_add", List.of("int", "int"), "void", (context, self, args) -> {
            Object3D parent = node(args[0]);
            Object3D child = node(args[1]);
            if (contains(child, parent)) {
                throw new IllegalArgumentException("lm3d_add would create a scene cycle");
            }
            parent.add(child);
            return Value.none();
        });
        registry.function("lm3d_position", List.of("int", "float", "float", "float"), "void",
                (context, self, args) -> {
                    node(args[0]).position.set(number(args[1]), number(args[2]), number(args[3]));
                    return Value.none();
                });
        registry.function("lm3d_rotation", List.of("int", "float", "float", "float"), "void",
                (context, self, args) -> {
                    node(args[0]).rotation.set(number(args[1]), number(args[2]), number(args[3]));
                    return Value.none();
                });
        registry.function("lm3d_scale", List.of("int", "float", "float", "float"), "void",
                (context, self, args) -> {
                    node(args[0]).scale.set(number(args[1]), number(args[2]), number(args[3]));
                    return Value.none();
                });
        registry.function("lm3d_shininess", List.of("int", "float"), "void", (context, self, args) -> {
            phong(args[0]).shininess = number(args[1]);
            return Value.none();
        });
        registry.function("lm3d_specular", List.of("int", "string"), "void", (context, self, args) -> {
            phong(args[0]).specular.set(string(args[1]));
            return Value.none();
        });
        registry.function("lm3d_emissive", List.of("int", "string"), "void", (context, self, args) -> {
            phong(args[0]).emissive.set(string(args[1]));
            return Value.none();
        });
        registry.function("lm3d_use", List.of("int"), "void", (context, self, args) -> {
            Object3D root = node(args[0]);
            if (!(root instanceof Group group)) {
                throw new IllegalArgumentException("lm3d_use expects a group handle");
            }
            rootConsumer.accept(group);
            used = true;
            return Value.none();
        });
    }

    public int meshCount() {
        return meshCount;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public boolean used() {
        return used;
    }

    private int add(Object3D node) {
        int id = nextId++;
        nodes.put(id, node);
        return id;
    }

    private int mesh(com.lmctl.lm3d.geometry.BufferGeometry geometry, String color) {
        meshCount++;
        vertexCount += geometry.vertexCount();
        return add(new Mesh(geometry, new MeshPhongMaterial(color)));
    }

    private Object3D node(Value value) {
        Object3D node = nodes.get(integer(value));
        if (node == null) {
            throw new IllegalArgumentException("Unknown lm3d node handle: " + value.asInt());
        }
        return node;
    }

    private static boolean contains(Object3D root, Object3D target) {
        if (root == target) {
            return true;
        }
        for (Object3D child : root.children()) {
            if (contains(child, target)) {
                return true;
            }
        }
        return false;
    }

    private MeshPhongMaterial phong(Value value) {
        Object3D node = node(value);
        if (!(node instanceof Mesh mesh) || !(mesh.material() instanceof MeshPhongMaterial material)) {
            throw new IllegalArgumentException("Material operation expects a Phong mesh handle");
        }
        return material;
    }

    private static int integer(Value value) {
        return Math.toIntExact(value.asInt());
    }

    private static float number(Value value) {
        return (float) value.asFloat();
    }

    private static String string(Value value) {
        return value.asString().toString();
    }
}
