package com.lmctl.lm3d.controls;

import com.lmctl.lm3d.camera.Camera;
import com.lmctl.lm3d.math.Vector3f;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public final class OrbitControls {
    private final Camera camera;
    private final HTMLCanvasElement canvas;
    private final Vector3f initialPosition;
    private final Vector3f initialTarget;
    private float radius;
    private float theta;
    private float phi;
    private boolean dragging;
    private int previousX;
    private int previousY;
    public float minRadius = 1;
    public float maxRadius = 40;
    public float rotateSpeed = 0.008f;
    public float zoomSpeed = 0.001f;
    public boolean enabled = true;

    public OrbitControls(Camera camera, HTMLCanvasElement canvas) {
        this.camera = camera;
        this.canvas = canvas;
        initialPosition = camera.position.copy();
        initialTarget = camera.target.copy();
        readCamera();
        canvas.onEvent("pointerdown", event -> {
            if (!enabled) {
                return;
            }
            MouseEvent mouse = (MouseEvent) event;
            dragging = true;
            previousX = mouse.getClientX();
            previousY = mouse.getClientY();
            mouse.preventDefault();
        });
        canvas.onEvent("pointermove", event -> {
            if (!enabled || !dragging) {
                return;
            }
            MouseEvent mouse = (MouseEvent) event;
            int x = mouse.getClientX();
            int y = mouse.getClientY();
            theta -= (x - previousX) * rotateSpeed;
            phi = clamp(phi - (y - previousY) * rotateSpeed, 0.05f, (float) Math.PI - 0.05f);
            previousX = x;
            previousY = y;
            update();
            mouse.preventDefault();
        });
        canvas.onEvent("pointerup", event -> dragging = false);
        canvas.onEvent("pointercancel", event -> dragging = false);
        canvas.onEvent("pointerleave", event -> dragging = false);
        canvas.onEvent("wheel", event -> {
            if (!enabled) {
                return;
            }
            WheelEvent wheel = (WheelEvent) event;
            radius = clamp(radius * (float) Math.exp(wheel.getDeltaY() * zoomSpeed), minRadius, maxRadius);
            update();
            wheel.preventDefault();
        });
        update();
    }

    public void update() {
        float sinPhi = (float) Math.sin(phi);
        camera.position.set(
                camera.target.x + radius * sinPhi * (float) Math.sin(theta),
                camera.target.y + radius * (float) Math.cos(phi),
                camera.target.z + radius * sinPhi * (float) Math.cos(theta));
        camera.lookAt(camera.target);
    }

    public void reset() {
        camera.position.set(initialPosition);
        camera.target.set(initialTarget);
        dragging = false;
        readCamera();
        update();
    }

    private void readCamera() {
        Vector3f offset = camera.position.copy().subtract(camera.target);
        radius = Math.max(0.0001f, offset.length());
        theta = (float) Math.atan2(offset.x, offset.z);
        phi = (float) Math.acos(Math.max(-1, Math.min(1, offset.y / radius)));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
