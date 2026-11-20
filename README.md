# lm3d

A small Three.js-inspired 3D library written in Java and compiled for the browser with TeaVM. It provides a scene graph, geometry, materials, lights, cameras, WebGL rendering, animation-friendly transforms, and pointer/wheel orbit controls without Three.js or another JavaScript 3D runtime.

- [Interactive demo](https://www.lmctl.com/tools/lm3d/demo.html)
- [LLM quick reference](https://www.lmctl.com/tools/lm3d/)
- [Authoring skill](https://www.lmctl.com/tools/lm3d/skill.html)

lm3d is a focused subset with a familiar API, not a drop-in Three.js port. The demo contains only TeaVM-generated application code and a one-line `boot.js` entry point.

## Capabilities

- `Object3D`, `Group`, `Scene`, and `Mesh` with inherited position, Euler rotation, scale, and visibility.
- Mutable `Vector3f` and column-major `Matrix4f` math, including composition, inversion, perspective, orthographic projection, and look-at views.
- Indexed `BufferGeometry` plus `BoxGeometry`, `SphereGeometry`, `PlaneGeometry`, and `TorusGeometry`.
- `MeshBasicMaterial` and Phong-style `MeshPhongMaterial` with specular and emissive colors.
- Ambient lights, one directional light, and one point light with optional distance attenuation.
- `PerspectiveCamera`, `OrthographicCamera`, and `OrbitControls`.
- `GltfLoader` for glTF 2.x JSON assets with embedded base64 or external buffers, node hierarchies, TRS/matrix transforms, indexed or sequential triangle geometry, interleaved accessors, generated normals, and common material factors.
- `Lm3dHost` typed LMCode functions for creating groups and meshes, parenting, transforms, and Phong material properties from annotated LMCode scene handlers.
- WebGL 1 renderer with cached GPU buffers, inverse-transpose normal matrices, alpha blending, depth testing, and draw statistics.

Current limits include no binary `.glb` container, textures, skins, glTF animation, shadows, post-processing, quaternion animation API, or transparent-object sorting. Sparse glTF accessors and non-triangle primitives are rejected. Meshes render double-sided. The first visible directional and point lights are used; all visible ambient lights are accumulated.

## Build

Requirements: Java 21, Maven, Node 24.15 or newer, `lmcode-java` commit `b88d3401ee10210a0a6583991d6a81427d5f112a` in the local Maven repository, and LMCode compiler `@lmctl-ai/lmctl` 0.2.482. The runtime commit and compiler version are pinned for reproducible bytecode and TeaVM compatibility. Node compiles LMCode during the Maven build and runs the browser tests.

```sh
git clone https://github.com/lmctl-ai/lm3d.git
git clone https://github.com/lmctl-ai/lmcode-java.git
(cd lmcode-java && git checkout b88d3401ee10210a0a6583991d6a81427d5f112a && mvn -DskipTests install)
cd lm3d
npm install --prefix compiler @lmctl-ai/lmctl@0.2.482
export LMCODE_COMPILER_MODULE="$PWD/compiler/node_modules/@lmctl-ai/lmctl/dist/dsl.js"
mvn clean package
```

The static demo is generated under `target/demo/`. The Maven artifact contains the reusable Java library and demo entry point. Use `mvn install` to make version `0.1.0` available in the local Maven repository.

To write the demo into another static site:

```sh
mvn -Ddemo.output=/absolute/site/path clean package
```

## Minimal scene

```java
import com.lmctl.lm3d.camera.PerspectiveCamera;
import com.lmctl.lm3d.core.Mesh;
import com.lmctl.lm3d.core.Scene;
import com.lmctl.lm3d.geometry.BoxGeometry;
import com.lmctl.lm3d.light.AmbientLight;
import com.lmctl.lm3d.light.DirectionalLight;
import com.lmctl.lm3d.material.MeshPhongMaterial;
import com.lmctl.lm3d.renderer.WebGLRenderer;
import org.teavm.jso.dom.html.HTMLCanvasElement;

HTMLCanvasElement canvas = (HTMLCanvasElement) document.getElementById("scene");
Scene scene = new Scene();
PerspectiveCamera camera = new PerspectiveCamera(48, 16f / 9, 0.1f, 100);
camera.position.set(5, 4, 7);
camera.lookAt(0, 1, 0);

Mesh cube = new Mesh(new BoxGeometry(2, 2, 2), new MeshPhongMaterial("#61d6c5"));
cube.position.y = 1.25f;
scene.add(cube);
scene.add(new AmbientLight("#ffffff", 0.4f));
DirectionalLight sun = new DirectionalLight("#ffffff", 1);
sun.position.set(4, 7, 5);
scene.add(sun);

WebGLRenderer renderer = new WebGLRenderer(canvas);
renderer.setSize(960, 540);
renderer.render(scene, camera);
```

`WebGLRenderer(canvas, true)` enables a preserved drawing buffer for screenshots and pixel readback. The default one-argument constructor leaves it disabled for normal rendering.

For animation, update Java object fields inside a `Window.requestAnimationFrame` callback and render again. `OrbitControls` attaches Java pointer and wheel listeners to a canvas.

## Load glTF

`GltfLoader` loads glTF 2.x JSON documents asynchronously. Add `model.root()` to a scene only after the success callback, and render again. External buffer URIs resolve relative to the `.gltf` document; normal HTTP CORS rules apply.

```java
import com.lmctl.lm3d.loader.GltfLoader;

GltfLoader loader = new GltfLoader();
loader.load("models/hierarchy.gltf", model -> {
    scene.add(model.root());
    renderer.render(scene, camera);
    System.out.println(model.meshCount() + " meshes, "
        + model.triangleCount() + " triangles");
}, error -> {
    status.setInnerText(error);
});
```

Supported geometry includes FLOAT VEC3 positions/normals, non-indexed triangles, and UNSIGNED_BYTE/UNSIGNED_SHORT/UNSIGNED_INT indices subject to the renderer's 16-bit index limit. Interleaved buffer views and accessor strides are handled. Missing normals are generated from triangle faces. Node translation/rotation/scale, quaternion rotation, and column-major node matrices are supported.

Materials map base color, emissive factor, alpha blend, `KHR_materials_unlit`, and a Phong approximation of metallic/roughness factors. Binary GLB containers, textures, sparse accessors, non-triangle primitives, skins, and glTF animation clips are not supported in this increment.

### Runnable examples

The demo selector contains three original MIT-licensed assets:

| Example | Files | Demonstrates |
| --- | --- | --- |
| Embedded cube | `models/cube-embedded.gltf` | Base64 buffer, quaternion rotation, indexed cube geometry |
| External hierarchy | `models/hierarchy.gltf`, `models/hierarchy.bin` | External buffer, parent/children, two materials, generated normals |
| Interleaved torus | `models/torus-interleaved.gltf`, `models/torus.bin` | Interleaved accessors, byte stride, node matrix, default scene and material |

Regenerate all three from standard-library Python:

```sh
python3 scripts/generate-gltf.py
```

## LMCode scene handlers

`Lm3dHost` exposes Java scene construction as typed LMCode host functions. LMCode sees only integer handles; Java owns the actual `Object3D` instances. The schema is `src/main/lmcode/scenes.schema.json`, and the three annotated handlers are in `src/main/lmcode/scenes.lms`.

| Function | Returns | Purpose |
| --- | --- | --- |
| `lm3d_group()` | `int` | Create a group handle |
| `lm3d_box(w, h, d, color)` | `int` | Create a Phong box mesh |
| `lm3d_sphere(radius, widthSegments, heightSegments, color)` | `int` | Create a Phong sphere mesh |
| `lm3d_torus(radius, tube, radialSegments, tubularSegments, color)` | `int` | Create a Phong torus mesh |
| `lm3d_add(parent, child)` | `void` | Attach a child node |
| `lm3d_position(node, x, y, z)` | `void` | Set node translation |
| `lm3d_rotation(node, x, y, z)` | `void` | Set Euler rotation in radians |
| `lm3d_scale(node, x, y, z)` | `void` | Set node scale |
| `lm3d_shininess(node, value)` | `void` | Set Phong shininess |
| `lm3d_specular(node, color)` | `void` | Set Phong specular color |
| `lm3d_emissive(node, color)` | `void` | Set Phong emissive color |
| `lm3d_use(group)` | `void` | Give the completed root to the Java application |

For example, the demo's `LMCode: cube` option runs this annotated handler:

```text
@cube
fn buildCube() -> void implements SceneHandler {
  let root = lm3d_group();
  let cube = lm3d_box(2.0, 2.0, 2.0, "#61d6c5");
  lm3d_position(cube, 0.0, 1.25, 0.0);
  lm3d_rotation(cube, 0.15, 0.4, 0.0);
  lm3d_shininess(cube, 72.0);
  lm3d_add(root, cube);
  lm3d_use(root);
}
```

Register the host with the exact schema, then load the compiled program and call the entry whose annotation matches `cube`, `hierarchy`, or `materials`:

```java
Registry registry = new Registry(false);
Lm3dHost host = new Lm3dHost(registry, scene::add);
Program program = Program.load(bytecode, schemaBytes, registry);
program.call(entryIndex, new Context(), registry, List.of());
```

The Maven build compiles `scenes.lms` before Java compilation and embeds its bytecode and exact schema bytes in a generated Java class. The browser demo resolves the selected annotation and invokes the handler. `@hierarchy` composes three transformed meshes; `@materials` exercises shininess, specular, and emissive properties. `@failure` calls `lm3d_use` and then fails, demonstrating that the previous scene is replaced only after successful handler completion. The host rejects direct and indirect scene cycles.

## Tests

```sh
mvn clean verify
npm ci
npx playwright install --with-deps chromium
npm test
```

The 21 JVM tests cover vector/matrix math, cameras, scene inheritance, colors, geometry counts, normal generation, defensive copies, invalid input, all LMCode scene handlers, failure after use, and indirect-cycle rejection. The Playwright suite starts a local static server and checks actual WebGL pixels, three procedural geometries, all three glTF examples and their external buffers, default scene/material behavior, propagation of success-callback exceptions, all three successful LMCode scenes, failure-atomic replacement, animation frames, pause/resume, keyboard input, orbit/zoom/reset, mobile overflow, runtime resources, and browser errors. Set `DEMO_URL` to run the same browser suite against a deployed demo or `PLAYWRIGHT_CHROMIUM_EXECUTABLE` to select an existing Chromium binary.

GitHub Actions builds the public LMCode runtime and compiler, then runs both suites on Ubuntu using public dependencies only.

## Source layout

- `src/main/java/com/lmctl/lm3d/` — reusable library packages, including `loader/GltfLoader.java` and `lmcode/Lm3dHost.java`.
- `src/main/java/com/lmctl/lm3d/demo/` — browser application entry point and LMCode annotation dispatch.
- `src/main/lmcode/` — typed host schema and four annotated scene handlers, also copied into the static demo.
- `src/main/webapp/` — static demo HTML, CSS, favicon, bootstrap, and glTF examples.
- `scripts/` — deterministic glTF generator and LMCode compiler bridge.
- `src/test/java/` — JVM unit tests.
- `tests/browser.mjs` — WebGL, glTF, and LMCode browser integration tests.

## License

MIT. TeaVM is Apache-2.0 licensed; the licenses are compatible. See `LICENSE` and `THIRD_PARTY_NOTICES.md`.
