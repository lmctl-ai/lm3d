package com.lmctl.lm3d.loader;

import com.lmctl.lm3d.core.Group;
import com.lmctl.lm3d.core.Mesh;
import com.lmctl.lm3d.geometry.BufferGeometry;
import com.lmctl.lm3d.material.Material;
import com.lmctl.lm3d.material.MeshBasicMaterial;
import com.lmctl.lm3d.material.MeshPhongMaterial;
import com.lmctl.lm3d.math.Vector3f;
import java.util.function.Consumer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Uint8Array;

public final class GltfLoader {
    public void load(String url, Consumer<GltfModel> onLoad, Consumer<String> onError) {
        if (url == null || onLoad == null || onError == null) {
            throw new IllegalArgumentException("glTF URL and callbacks must not be null");
        }
        String absoluteUrl = resolveUrl(null, url);
        XMLHttpRequest request = XMLHttpRequest.create();
        request.open("GET", absoluteUrl);
        request.onComplete(() -> {
            if (!successful(request)) {
                onError.accept("Could not load glTF (HTTP " + request.getStatus() + "): " + url);
                return;
            }
            GltfDocument document;
            try {
                document = parseDocument(request.getResponseText());
                if (document.getAsset() == null || document.getAsset().getVersion() == null
                        || !document.getAsset().getVersion().startsWith("2.")) {
                    throw new IllegalArgumentException("Only glTF 2.x assets are supported");
                }
            } catch (RuntimeException e) {
                onError.accept("Invalid glTF: " + e.getMessage());
                return;
            }
            loadBuffers(document, absoluteUrl, onLoad, onError);
        });
        request.onError(event -> onError.accept("Network error while loading glTF: " + url));
        request.send();
    }

    private void loadBuffers(GltfDocument document, String baseUrl, Consumer<GltfModel> onLoad,
            Consumer<String> onError) {
        LoadState state = new LoadState();
        JSArrayReader<GltfBuffer> sources = document.getBuffers();
        if (sources == null || sources.getLength() == 0) {
            build(document, onLoad, onError, state);
            return;
        }
        ArrayBuffer[] buffers = new ArrayBuffer[sources.getLength()];
        state.pending = sources.getLength();
        for (int i = 0; i < sources.getLength(); i++) {
            final int index = i;
            GltfBuffer source = sources.get(i);
            String uri = source.getUri();
            if (uri != null && uri.startsWith("data:")) {
                try {
                    int comma = uri.indexOf(',');
                    if (comma < 0 || !uri.substring(0, comma).endsWith(";base64")) {
                        throw new IllegalArgumentException("Only base64 data URIs are supported for glTF buffers");
                    }
                    buffers[index] = decodeBase64(uri.substring(comma + 1)).getBuffer();
                } catch (RuntimeException e) {
                    fail(state, onError, "Invalid embedded glTF buffer: " + e.getMessage());
                    continue;
                }
                bufferReady(document, buffers, source.getByteLength(), index, onLoad, onError, state);
            } else if (uri != null) {
                String bufferUrl = resolveUrl(baseUrl, uri);
                XMLHttpRequest request = XMLHttpRequest.create();
                request.open("GET", bufferUrl);
                request.setResponseType("arraybuffer");
                request.onComplete(() -> {
                    if (state.failed) {
                        return;
                    }
                    if (!successful(request) || request.getResponse() == null) {
                        fail(state, onError, "Could not load glTF buffer (HTTP " + request.getStatus() + "): " + uri);
                        return;
                    }
                    try {
                        buffers[index] = request.getResponse().cast();
                    } catch (RuntimeException e) {
                        fail(state, onError, "Invalid glTF buffer: " + e.getMessage());
                        return;
                    }
                    bufferReady(document, buffers, source.getByteLength(), index, onLoad, onError, state);
                });
                request.onError(event -> fail(state, onError, "Network error while loading glTF buffer: " + uri));
                request.send();
            } else {
                fail(state, onError, "External GLB buffer chunks are not supported by this .gltf loader");
            }
        }
    }

    private void bufferReady(GltfDocument document, ArrayBuffer[] buffers, int expectedLength, int index,
            Consumer<GltfModel> onLoad, Consumer<String> onError, LoadState state) {
        if (state.failed || state.completed) {
            return;
        }
        if (buffers[index] == null || buffers[index].getByteLength() < expectedLength) {
            fail(state, onError, "glTF buffer " + index + " is shorter than its declared byteLength");
            return;
        }
        document.getBuffers().get(index).setLoadedBuffer(buffers[index]);
        state.pending--;
        if (state.pending == 0) {
            build(document, onLoad, onError, state);
        }
    }

    private void build(GltfDocument document, Consumer<GltfModel> onLoad, Consumer<String> onError,
            LoadState state) {
        if (state.failed || state.completed) {
            return;
        }
        GltfModel model;
        try {
            model = new Builder(document).build();
        } catch (RuntimeException e) {
            fail(state, onError, "Invalid glTF: " + e.getMessage());
            return;
        }
        state.completed = true;
        onLoad.accept(model);
    }

    private static boolean successful(XMLHttpRequest request) {
        return request.getStatus() >= 200 && request.getStatus() < 300;
    }

    private static void fail(LoadState state, Consumer<String> onError, String message) {
        if (!state.failed && !state.completed) {
            state.failed = true;
            onError.accept(message);
        }
    }

    private static final class LoadState {
        int pending;
        boolean failed;
        boolean completed;
    }

    private static final class Builder {
        private final GltfDocument document;
        private final Material[] materials;
        private final boolean[] ancestors;
        private int meshCount;
        private int vertexCount;
        private int triangleCount;

        Builder(GltfDocument document) {
            this.document = document;
            materials = new Material[document.getMaterials() == null ? 0 : document.getMaterials().getLength()];
            ancestors = new boolean[document.getNodes() == null ? 0 : document.getNodes().getLength()];
        }

        GltfModel build() {
            if (document.getNodes() == null || document.getMeshes() == null) {
                throw new IllegalArgumentException("Asset must contain nodes and meshes");
            }
            Group root = new Group();
            root.name = "glTF scene";
            JSArrayReader<GltfScene> scenes = document.getScenes();
            if (scenes == null || scenes.getLength() == 0) {
                boolean[] hasParent = new boolean[document.getNodes().getLength()];
                for (int i = 0; i < document.getNodes().getLength(); i++) {
                    JSArrayReader<JSNumber> children = document.getNodes().get(i).getChildren();
                    if (children != null) {
                        for (int j = 0; j < children.getLength(); j++) {
                            hasParent[children.get(j).intValue()] = true;
                        }
                    }
                }
                for (int i = 0; i < document.getNodes().getLength(); i++) {
                    if (!hasParent[i]) {
                        root.add(buildNode(i));
                    }
                }
            } else {
                int sceneIndex = document.getScene() == null ? 0 : document.getScene().intValue();
                if (sceneIndex < 0 || sceneIndex >= scenes.getLength()) {
                    throw new IllegalArgumentException("Default scene index is out of range");
                }
                JSArrayReader<JSNumber> nodes = scenes.get(sceneIndex).getNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        root.add(buildNode(nodes.get(i).intValue()));
                    }
                }
            }
            return new GltfModel(root, meshCount, vertexCount, triangleCount);
        }

        private Group buildNode(int index) {
            if (index < 0 || index >= document.getNodes().getLength()) {
                throw new IllegalArgumentException("Node index is out of range: " + index);
            }
            if (ancestors[index]) {
                throw new IllegalArgumentException("Node hierarchy contains a cycle at " + index);
            }
            ancestors[index] = true;
            GltfNode source = document.getNodes().get(index);
            Group group = new Group();
            if (source.getName() != null) {
                group.name = source.getName();
            }
            applyTransform(group, source);
            if (source.getMesh() != null) {
                int meshIndex = source.getMesh().intValue();
                if (meshIndex < 0 || meshIndex >= document.getMeshes().getLength()) {
                    throw new IllegalArgumentException("Mesh index is out of range: " + meshIndex);
                }
                GltfMesh mesh = document.getMeshes().get(meshIndex);
                for (int i = 0; i < mesh.getPrimitives().getLength(); i++) {
                    group.add(buildPrimitive(mesh.getPrimitives().get(i)));
                }
            }
            if (source.getChildren() != null) {
                for (int i = 0; i < source.getChildren().getLength(); i++) {
                    group.add(buildNode(source.getChildren().get(i).intValue()));
                }
            }
            ancestors[index] = false;
            return group;
        }

        private Mesh buildPrimitive(GltfPrimitive primitive) {
            if (primitive.getMode() != null && primitive.getMode().intValue() != 4) {
                throw new IllegalArgumentException("Only triangle primitives are supported");
            }
            if (primitive.getAttributes() == null || primitive.getAttributes().getPosition() == null) {
                throw new IllegalArgumentException("Primitive POSITION accessor is required");
            }
            float[] positions = readVec3(primitive.getAttributes().getPosition().intValue());
            float[] normals = primitive.getAttributes().getNormal() == null ? null
                    : readVec3(primitive.getAttributes().getNormal().intValue());
            char[] indices = primitive.getIndices() == null ? sequentialIndices(positions.length / 3)
                    : readIndices(primitive.getIndices().intValue());
            if (normals == null) {
                normals = computeNormals(positions, indices);
            }
            BufferGeometry geometry = new BufferGeometry(positions, normals, indices);
            vertexCount += geometry.vertexCount();
            triangleCount += geometry.triangleCount();
            meshCount++;
            int materialIndex = primitive.getMaterial() == null ? -1 : primitive.getMaterial().intValue();
            return new Mesh(geometry, material(materialIndex));
        }

        private Material material(int index) {
            if (index < 0) {
                MeshPhongMaterial material = new MeshPhongMaterial("#ffffff");
                material.shininess = 4;
                return material;
            }
            if (index >= materials.length) {
                throw new IllegalArgumentException("Material index is out of range: " + index);
            }
            if (materials[index] != null) {
                return materials[index];
            }
            GltfMaterial source = document.getMaterials().get(index);
            GltfPbr pbr = source.getPbrMetallicRoughness();
            JSArrayReader<JSNumber> base = pbr == null ? null : pbr.getBaseColorFactor();
            float r = factor(base, 0, 1);
            float g = factor(base, 1, 1);
            float b = factor(base, 2, 1);
            float opacity = source.getAlphaMode() != null && source.getAlphaMode().equals("BLEND")
                    ? factor(base, 3, 1) : 1;
            boolean unlit = source.getExtensions() != null && source.getExtensions().getUnlit() != null;
            Material material = unlit ? new MeshBasicMaterial("#ffffff") : new MeshPhongMaterial("#ffffff");
            material.color.set(r, g, b);
            material.opacity = opacity;
            if (material instanceof MeshPhongMaterial phong) {
                float metallic = pbr == null || pbr.getMetallicFactor() == null ? 1 : pbr.getMetallicFactor().floatValue();
                float roughness = pbr == null || pbr.getRoughnessFactor() == null ? 1 : pbr.getRoughnessFactor().floatValue();
                phong.specular.set(0.04f * (1 - metallic) + r * metallic,
                        0.04f * (1 - metallic) + g * metallic, 0.04f * (1 - metallic) + b * metallic);
                phong.shininess = Math.max(4, (1 - roughness) * 128);
                JSArrayReader<JSNumber> emissive = source.getEmissiveFactor();
                phong.emissive.set(factor(emissive, 0, 0), factor(emissive, 1, 0), factor(emissive, 2, 0));
            }
            materials[index] = material;
            return material;
        }

        private float[] readVec3(int accessorIndex) {
            AccessorReader reader = new AccessorReader(document, accessorIndex, 3);
            float[] result = new float[reader.count * 3];
            for (int i = 0; i < reader.count; i++) {
                for (int component = 0; component < 3; component++) {
                    result[i * 3 + component] = reader.floatComponent(i, component);
                }
            }
            return result;
        }

        private char[] readIndices(int accessorIndex) {
            AccessorReader reader = new AccessorReader(document, accessorIndex, 1);
            char[] result = new char[reader.count];
            for (int i = 0; i < reader.count; i++) {
                long value = reader.integerComponent(i);
                if (value > 65535) {
                    throw new IllegalArgumentException("WebGL 1 index exceeds 65535");
                }
                result[i] = (char) value;
            }
            return result;
        }
    }

    private static final class AccessorReader {
        final int count;
        private final DataView view;
        private final int offset;
        private final int stride;
        private final int componentType;
        private final int componentSize;
        private final int components;

        AccessorReader(GltfDocument document, int index, int components) {
            if (document.getAccessors() == null || index < 0 || index >= document.getAccessors().getLength()) {
                throw new IllegalArgumentException("Accessor index is out of range: " + index);
            }
            GltfAccessor accessor = document.getAccessors().get(index);
            if (accessor.getBufferView() == null || document.getBufferViews() == null) {
                throw new IllegalArgumentException("Sparse accessors are not supported");
            }
            int viewIndex = accessor.getBufferView().intValue();
            if (viewIndex < 0 || viewIndex >= document.getBufferViews().getLength()) {
                throw new IllegalArgumentException("Buffer view index is out of range: " + viewIndex);
            }
            GltfBufferView source = document.getBufferViews().get(viewIndex);
            ArrayBuffer buffer = document.getBuffers().get(source.getBuffer()).getLoadedBuffer();
            if (buffer == null) {
                throw new IllegalArgumentException("Buffer is not loaded: " + source.getBuffer());
            }
            if (source.getByteOffset() < 0 || source.getByteLength() < 0
                    || source.getByteOffset() + source.getByteLength() > buffer.getByteLength()) {
                throw new IllegalArgumentException("Buffer view exceeds its buffer");
            }
            componentType = accessor.getComponentType();
            componentSize = componentSize(componentType);
            this.components = components;
            String expectedType = components == 3 ? "VEC3" : "SCALAR";
            if (!expectedType.equals(accessor.getType())) {
                throw new IllegalArgumentException("Accessor must be " + expectedType + ": " + accessor.getType());
            }
            count = accessor.getCount();
            stride = source.getByteStride() > 0 ? source.getByteStride() : componentSize * components;
            offset = accessor.getByteOffset();
            if (count < 0 || offset < 0 || (count > 0
                    && offset + (count - 1L) * stride + componentSize * components > source.getByteLength())) {
                throw new IllegalArgumentException("Accessor exceeds its buffer view");
            }
            view = new DataView(buffer, source.getByteOffset(), source.getByteLength());
        }

        float floatComponent(int element, int component) {
            if (componentType != 5126) {
                throw new IllegalArgumentException("VEC3 attributes must use FLOAT components");
            }
            return view.getFloat32(offset + element * stride + component * componentSize, true);
        }

        long integerComponent(int element) {
            int position = offset + element * stride;
            return switch (componentType) {
                case 5121 -> view.getUint8(position);
                case 5123 -> view.getUint16(position, true);
                case 5125 -> view.getUint32(position, true);
                default -> throw new IllegalArgumentException("Indices must be UNSIGNED_BYTE, UNSIGNED_SHORT, or UNSIGNED_INT");
            };
        }

        private static int componentSize(int componentType) {
            return switch (componentType) {
                case 5121 -> 1;
                case 5123 -> 2;
                case 5125, 5126 -> 4;
                default -> throw new IllegalArgumentException("Unsupported accessor componentType: " + componentType);
            };
        }
    }

    private static char[] sequentialIndices(int vertexCount) {
        if (vertexCount > 65536) {
            throw new IllegalArgumentException("WebGL 1 geometry is limited to 65536 vertices");
        }
        char[] indices = new char[vertexCount];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = (char) i;
        }
        return indices;
    }

    private static float[] computeNormals(float[] positions, char[] indices) {
        float[] normals = new float[positions.length];
        for (int i = 0; i < indices.length; i += 3) {
            int a = indices[i] * 3;
            int b = indices[i + 1] * 3;
            int c = indices[i + 2] * 3;
            Vector3f ab = new Vector3f(positions[b] - positions[a], positions[b + 1] - positions[a + 1],
                    positions[b + 2] - positions[a + 2]);
            Vector3f ac = new Vector3f(positions[c] - positions[a], positions[c + 1] - positions[a + 1],
                    positions[c + 2] - positions[a + 2]);
            Vector3f normal = ab.cross(ac);
            for (int vertex : new int[] {a, b, c}) {
                normals[vertex] += normal.x;
                normals[vertex + 1] += normal.y;
                normals[vertex + 2] += normal.z;
            }
        }
        for (int i = 0; i < normals.length; i += 3) {
            Vector3f normal = new Vector3f(normals[i], normals[i + 1], normals[i + 2]).normalize();
            normals[i] = normal.x;
            normals[i + 1] = normal.y;
            normals[i + 2] = normal.z;
        }
        return normals;
    }

    private static void applyTransform(Group group, GltfNode source) {
        if (source.getMatrix() != null) {
            float[] e = numbers(source.getMatrix(), 16);
            group.position.set(e[12], e[13], e[14]);
            float sx = (float) Math.sqrt(e[0] * e[0] + e[1] * e[1] + e[2] * e[2]);
            float sy = (float) Math.sqrt(e[4] * e[4] + e[5] * e[5] + e[6] * e[6]);
            float sz = (float) Math.sqrt(e[8] * e[8] + e[9] * e[9] + e[10] * e[10]);
            float determinant = e[0] * (e[5] * e[10] - e[6] * e[9])
                    - e[4] * (e[1] * e[10] - e[2] * e[9]) + e[8] * (e[1] * e[6] - e[2] * e[5]);
            if (determinant < 0) {
                sx = -sx;
            }
            group.scale.set(sx, sy, sz);
            setEuler(group.rotation, e[0] / sx, e[1] / sx, e[2] / sx,
                    e[4] / sy, e[5] / sy, e[6] / sy, e[8] / sz, e[9] / sz, e[10] / sz);
            return;
        }
        group.position.set(factor(source.getTranslation(), 0, 0), factor(source.getTranslation(), 1, 0),
                factor(source.getTranslation(), 2, 0));
        group.scale.set(factor(source.getScale(), 0, 1), factor(source.getScale(), 1, 1),
                factor(source.getScale(), 2, 1));
        float x = factor(source.getRotation(), 0, 0);
        float y = factor(source.getRotation(), 1, 0);
        float z = factor(source.getRotation(), 2, 0);
        float w = factor(source.getRotation(), 3, 1);
        float length = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (length == 0) {
            throw new IllegalArgumentException("Node quaternion must not be zero");
        }
        x /= length;
        y /= length;
        z /= length;
        w /= length;
        setEuler(group.rotation,
                1 - 2 * (y * y + z * z), 2 * (x * y + z * w), 2 * (x * z - y * w),
                2 * (x * y - z * w), 1 - 2 * (x * x + z * z), 2 * (y * z + x * w),
                2 * (x * z + y * w), 2 * (y * z - x * w), 1 - 2 * (x * x + y * y));
    }

    private static void setEuler(Vector3f rotation, float m11, float m21, float m31,
            float m12, float m22, float m32, float m13, float m23, float m33) {
        rotation.y = (float) Math.asin(Math.max(-1, Math.min(1, m13)));
        if (Math.abs(m13) < 0.9999999f) {
            rotation.x = (float) Math.atan2(-m23, m33);
            rotation.z = (float) Math.atan2(-m12, m11);
        } else {
            rotation.x = (float) Math.atan2(m32, m22);
            rotation.z = 0;
        }
    }

    private static float[] numbers(JSArrayReader<JSNumber> array, int expectedLength) {
        if (array.getLength() != expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength + " numeric values");
        }
        float[] result = new float[expectedLength];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).floatValue();
        }
        return result;
    }

    private static float factor(JSArrayReader<JSNumber> array, int index, float fallback) {
        return array == null ? fallback : array.get(index).floatValue();
    }

    @JSBody(params = {"text"}, script = "function wrap(value) {"
            + " if (value === null || typeof value !== 'object' || value instanceof ArrayBuffer) return value;"
            + " return new Proxy(value, { get: function(target, key) {"
            + " var item = target[key]; return item == null ? null : wrap(item); } });"
            + " } return wrap(JSON.parse(text));")
    private static native GltfDocument parseDocument(String text);

    @JSBody(params = {"base", "relative"}, script = "return new URL(relative, base || window.location.href).href;")
    private static native String resolveUrl(String base, String relative);

    @JSBody(params = {"value"}, script = "var text = atob(value); var bytes = new Uint8Array(text.length);"
            + "for (var i = 0; i < text.length; i++) { bytes[i] = text.charCodeAt(i); } return bytes;")
    private static native Uint8Array decodeBase64(String value);

    private interface GltfDocument extends JSObject {
        @JSProperty GltfAsset getAsset();
        @JSProperty JSNumber getScene();
        @JSProperty JSArrayReader<GltfScene> getScenes();
        @JSProperty JSArrayReader<GltfNode> getNodes();
        @JSProperty JSArrayReader<GltfMesh> getMeshes();
        @JSProperty JSArrayReader<GltfMaterial> getMaterials();
        @JSProperty JSArrayReader<GltfAccessor> getAccessors();
        @JSProperty JSArrayReader<GltfBufferView> getBufferViews();
        @JSProperty JSArrayReader<GltfBuffer> getBuffers();
    }

    private interface GltfAsset extends JSObject {
        @JSProperty String getVersion();
    }

    private interface GltfScene extends JSObject {
        @JSProperty JSArrayReader<JSNumber> getNodes();
    }

    private interface GltfNode extends JSObject {
        @JSProperty String getName();
        @JSProperty JSNumber getMesh();
        @JSProperty JSArrayReader<JSNumber> getChildren();
        @JSProperty JSArrayReader<JSNumber> getTranslation();
        @JSProperty JSArrayReader<JSNumber> getRotation();
        @JSProperty JSArrayReader<JSNumber> getScale();
        @JSProperty JSArrayReader<JSNumber> getMatrix();
    }

    private interface GltfMesh extends JSObject {
        @JSProperty JSArrayReader<GltfPrimitive> getPrimitives();
    }

    private interface GltfPrimitive extends JSObject {
        @JSProperty GltfAttributes getAttributes();
        @JSProperty JSNumber getIndices();
        @JSProperty JSNumber getMaterial();
        @JSProperty JSNumber getMode();
    }

    private interface GltfAttributes extends JSObject {
        @JSProperty("POSITION") JSNumber getPosition();
        @JSProperty("NORMAL") JSNumber getNormal();
    }

    private interface GltfMaterial extends JSObject {
        @JSProperty GltfPbr getPbrMetallicRoughness();
        @JSProperty JSArrayReader<JSNumber> getEmissiveFactor();
        @JSProperty String getAlphaMode();
        @JSProperty GltfExtensions getExtensions();
    }

    private interface GltfPbr extends JSObject {
        @JSProperty JSArrayReader<JSNumber> getBaseColorFactor();
        @JSProperty JSNumber getMetallicFactor();
        @JSProperty JSNumber getRoughnessFactor();
    }

    private interface GltfExtensions extends JSObject {
        @JSProperty("KHR_materials_unlit") JSObject getUnlit();
    }

    private interface GltfAccessor extends JSObject {
        @JSProperty JSNumber getBufferView();
        @JSProperty int getByteOffset();
        @JSProperty int getComponentType();
        @JSProperty int getCount();
        @JSProperty String getType();
    }

    private interface GltfBufferView extends JSObject {
        @JSProperty int getBuffer();
        @JSProperty int getByteOffset();
        @JSProperty int getByteLength();
        @JSProperty int getByteStride();
    }

    private interface GltfBuffer extends JSObject {
        @JSProperty String getUri();
        @JSProperty int getByteLength();
        @JSProperty ArrayBuffer getLoadedBuffer();
        @JSProperty void setLoadedBuffer(ArrayBuffer buffer);
    }
}
