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
- WebGL 1 renderer with cached GPU buffers, inverse-transpose normal matrices, alpha blending, depth testing, and draw statistics.

Current limits include no textures, model loaders, shadows, post-processing, quaternion animation, or transparent-object sorting. Meshes render double-sided. The first visible directional and point lights are used; all visible ambient lights are accumulated.

## Build

Requirements: Java 21 and Maven. Node is needed only for browser integration tests.

```sh
git clone https://github.com/lmctl-ai/lm3d.git
cd lm3d
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

## Tests

```sh
mvn clean verify
npm ci
npx playwright install --with-deps chromium
npm test
```

The JVM suite covers vector/matrix math, cameras, scene inheritance, colors, geometry counts, normal generation, defensive copies, and invalid input. The Playwright suite starts a local static server and checks actual WebGL pixels, all three hero geometries, animation frames, pause/resume, keyboard input, orbit/zoom/reset, mobile overflow, runtime resources, and browser errors. Set `DEMO_URL` to run the same browser suite against a deployed demo or `PLAYWRIGHT_CHROMIUM_EXECUTABLE` to select an existing Chromium binary.

GitHub Actions runs both suites on Ubuntu using public dependencies only.

## Source layout

- `src/main/java/com/lmctl/lm3d/` — reusable library packages.
- `src/main/java/com/lmctl/lm3d/demo/` — browser application entry point.
- `src/main/webapp/` — static demo HTML, CSS, favicon, and bootstrap.
- `src/test/java/` — JVM unit tests.
- `tests/browser.mjs` — WebGL browser integration tests.

## License

MIT. TeaVM is Apache-2.0 licensed; the licenses are compatible. See `LICENSE` and `THIRD_PARTY_NOTICES.md`.
