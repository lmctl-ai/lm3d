package com.lmctl.lm3d.demo;

import com.lmctl.lm3d.camera.PerspectiveCamera;
import com.lmctl.lm3d.controls.OrbitControls;
import com.lmctl.lm3d.core.Mesh;
import com.lmctl.lm3d.core.Scene;
import com.lmctl.lm3d.geometry.BoxGeometry;
import com.lmctl.lm3d.geometry.PlaneGeometry;
import com.lmctl.lm3d.geometry.SphereGeometry;
import com.lmctl.lm3d.geometry.TorusGeometry;
import com.lmctl.lm3d.light.AmbientLight;
import com.lmctl.lm3d.light.DirectionalLight;
import com.lmctl.lm3d.light.PointLight;
import com.lmctl.lm3d.material.MeshBasicMaterial;
import com.lmctl.lm3d.material.MeshPhongMaterial;
import com.lmctl.lm3d.renderer.WebGLRenderer;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

public final class Lm3dDemo {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static HTMLDocument document;
    private static HTMLCanvasElement canvas;
    private static HTMLSelectElement geometrySelect;
    private static HTMLButtonElement toggleButton;
    private static HTMLElement status;
    private static HTMLElement stats;
    private static Scene scene;
    private static PerspectiveCamera camera;
    private static WebGLRenderer renderer;
    private static OrbitControls controls;
    private static Mesh hero;
    private static Mesh satellite;
    private static PointLight satelliteLight;
    private static boolean paused;
    private static double previousTimestamp;
    private static double elapsed;

    private Lm3dDemo() {
    }

    public static void main(String[] args) {
        document = Window.current().getDocument();
        status = document.getElementById("status");
        try {
            initialize();
        } catch (RuntimeException e) {
            status.setInnerText("Unable to start the WebGL demo: " + e.getMessage());
            throw e;
        }
    }

    private static void initialize() {
        canvas = (HTMLCanvasElement) document.getElementById("scene-canvas");
        geometrySelect = (HTMLSelectElement) document.getElementById("geometry");
        toggleButton = (HTMLButtonElement) document.getElementById("toggle-animation");
        HTMLButtonElement resetButton = (HTMLButtonElement) document.getElementById("reset-scene");
        stats = document.getElementById("render-stats");

        renderer = new WebGLRenderer(canvas, true);
        renderer.setSize(WIDTH, HEIGHT);
        camera = new PerspectiveCamera(48, (float) WIDTH / HEIGHT, 0.1f, 100);
        camera.position.set(5.5f, 4.2f, 7);
        camera.lookAt(0, 1, 0);
        controls = new OrbitControls(camera, canvas);

        scene = new Scene();
        MeshPhongMaterial heroMaterial = new MeshPhongMaterial("#61d6c5");
        heroMaterial.specular.set("#d8fff7");
        heroMaterial.shininess = 72;
        hero = new Mesh(new BoxGeometry(2.1f, 2.1f, 2.1f), heroMaterial);
        hero.name = "Hero mesh";
        hero.position.set(0, 1.25f, 0);
        scene.add(hero);

        MeshPhongMaterial groundMaterial = new MeshPhongMaterial("#1b304a");
        groundMaterial.specular.set("#24344a");
        groundMaterial.shininess = 12;
        Mesh ground = new Mesh(new PlaneGeometry(18, 18), groundMaterial);
        ground.name = "Ground";
        ground.rotation.x = -(float) Math.PI / 2;
        scene.add(ground);

        satellite = new Mesh(new SphereGeometry(0.3f, 20, 12), new MeshBasicMaterial("#ffcb6b"));
        satellite.name = "Orbiting satellite";
        scene.add(satellite);
        satelliteLight = new PointLight("#8fa8ff", 1.8f, 12);
        scene.add(satelliteLight);

        AmbientLight ambient = new AmbientLight("#b8c9ef", 0.42f);
        scene.add(ambient);
        DirectionalLight sun = new DirectionalLight("#fff1d6", 1.05f);
        sun.position.set(4, 7, 5);
        scene.add(sun);

        geometrySelect.onEvent("change", event -> changeGeometry());
        toggleButton.onClick(event -> toggleAnimation());
        resetButton.onClick(event -> resetScene());
        updateSatellite();
        renderer.render(scene, camera);
        updateStats();
        status.setInnerText("Ready: Box geometry. Drag to orbit, scroll to zoom, or pause the Java animation.");
        Window.requestAnimationFrame(Lm3dDemo::animate);
    }

    private static void changeGeometry() {
        String geometry = geometrySelect.getValue();
        String label = switch (geometry) {
            case "sphere" -> {
                hero.setGeometry(new SphereGeometry(1.35f, 32, 24));
                yield "Sphere";
            }
            case "torus" -> {
                hero.setGeometry(new TorusGeometry(1.15f, 0.42f, 48, 24));
                yield "Torus";
            }
            default -> {
                hero.setGeometry(new BoxGeometry(2.1f, 2.1f, 2.1f));
                yield "Box";
            }
        };
        renderer.render(scene, camera);
        updateStats();
        status.setInnerText("Ready: " + label
                + " geometry. The mesh was replaced through the Java scene graph.");
    }

    private static void toggleAnimation() {
        paused = !paused;
        previousTimestamp = 0;
        toggleButton.setInnerText(paused ? "Resume animation" : "Pause animation");
        toggleButton.setAttribute("aria-pressed", Boolean.toString(paused));
        canvas.setAttribute("aria-label", paused ? "3D scene, animation paused" : "Interactive animated 3D scene");
        status.setInnerText(paused ? "Animation paused. Orbit controls remain available."
                : "Animation resumed through requestAnimationFrame.");
    }

    private static void resetScene() {
        controls.reset();
        hero.rotation.set(0, 0, 0);
        elapsed = 0;
        previousTimestamp = 0;
        updateSatellite();
        renderer.render(scene, camera);
        updateStats();
        status.setInnerText("Scene reset to its initial camera and object transforms.");
    }

    private static void animate(double timestamp) {
        if (!paused) {
            if (previousTimestamp > 0) {
                double delta = Math.min(0.05, (timestamp - previousTimestamp) / 1000);
                elapsed += delta;
                hero.rotation.y += (float) delta * 0.55f;
                hero.rotation.x = (float) Math.sin(elapsed * 0.45) * 0.18f;
                updateSatellite();
            }
            previousTimestamp = timestamp;
        }
        renderer.render(scene, camera);
        Window.requestAnimationFrame(Lm3dDemo::animate);
    }

    private static void updateSatellite() {
        float x = (float) Math.cos(elapsed * 0.9) * 2.8f;
        float y = 1.4f + (float) Math.sin(elapsed * 1.4) * 0.45f;
        float z = (float) Math.sin(elapsed * 0.9) * 2.8f;
        satellite.position.set(x, y, z);
        satelliteLight.position.set(x, y, z);
    }

    private static void updateStats() {
        stats.setInnerText(hero.geometry().vertexCount() + " hero vertices · "
                + renderer.triangles() + " triangles · " + renderer.drawCalls() + " draw calls");
    }
}
