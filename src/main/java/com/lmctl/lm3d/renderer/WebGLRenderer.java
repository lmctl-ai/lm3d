package com.lmctl.lm3d.renderer;

import com.lmctl.lm3d.camera.Camera;
import com.lmctl.lm3d.core.Color;
import com.lmctl.lm3d.core.Mesh;
import com.lmctl.lm3d.core.Object3D;
import com.lmctl.lm3d.core.Scene;
import com.lmctl.lm3d.geometry.BufferGeometry;
import com.lmctl.lm3d.light.AmbientLight;
import com.lmctl.lm3d.light.DirectionalLight;
import com.lmctl.lm3d.light.PointLight;
import com.lmctl.lm3d.material.Material;
import com.lmctl.lm3d.material.MeshPhongMaterial;
import com.lmctl.lm3d.math.Matrix4f;
import com.lmctl.lm3d.math.Vector3f;
import java.util.IdentityHashMap;
import java.util.Map;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint16Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLContextAttributes;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLUniformLocation;

public final class WebGLRenderer {
    private static final String VERTEX_SHADER =
            "attribute vec3 aPosition;"
            + "attribute vec3 aNormal;"
            + "uniform mat4 uModel;"
            + "uniform mat4 uView;"
            + "uniform mat4 uProjection;"
            + "uniform mat3 uNormal;"
            + "varying vec3 vNormal;"
            + "varying vec3 vWorldPosition;"
            + "void main() {"
            + " vec4 world = uModel * vec4(aPosition, 1.0);"
            + " vWorldPosition = world.xyz;"
            + " vNormal = normalize(uNormal * aNormal);"
            + " gl_Position = uProjection * uView * world;"
            + "}";
    private static final String FRAGMENT_SHADER =
            "precision highp float;"
            + "varying vec3 vNormal;"
            + "varying vec3 vWorldPosition;"
            + "uniform vec3 uColor;"
            + "uniform vec3 uEmissive;"
            + "uniform vec3 uSpecular;"
            + "uniform float uOpacity;"
            + "uniform float uShininess;"
            + "uniform bool uUnlit;"
            + "uniform vec3 uAmbient;"
            + "uniform vec3 uDirectionalColor;"
            + "uniform vec3 uDirectionalDirection;"
            + "uniform vec3 uPointColor;"
            + "uniform vec3 uPointPosition;"
            + "uniform float uPointDistance;"
            + "uniform vec3 uCameraPosition;"
            + "void main() {"
            + " if (uUnlit) { gl_FragColor = vec4(uColor, uOpacity); return; }"
            + " vec3 normal = normalize(vNormal);"
            + " vec3 viewDirection = normalize(uCameraPosition - vWorldPosition);"
            + " float directionalDiffuse = max(dot(normal, uDirectionalDirection), 0.0);"
            + " vec3 directionalHalf = normalize(uDirectionalDirection + viewDirection);"
            + " float directionalSpec = pow(max(dot(normal, directionalHalf), 0.0), uShininess);"
            + " vec3 pointVector = uPointPosition - vWorldPosition;"
            + " float pointLength = length(pointVector);"
            + " vec3 pointDirection = pointLength > 0.0 ? pointVector / pointLength : vec3(0.0);"
            + " float attenuation = uPointDistance > 0.0"
            + " ? max(1.0 - pointLength / uPointDistance, 0.0) : 1.0;"
            + " float pointDiffuse = max(dot(normal, pointDirection), 0.0) * attenuation;"
            + " vec3 pointHalf = normalize(pointDirection + viewDirection);"
            + " float pointSpec = pow(max(dot(normal, pointHalf), 0.0), uShininess) * attenuation;"
            + " vec3 diffuse = uDirectionalColor * directionalDiffuse + uPointColor * pointDiffuse;"
            + " vec3 specular = uSpecular * (uDirectionalColor * directionalSpec + uPointColor * pointSpec);"
            + " vec3 lit = uEmissive + uColor * (uAmbient + diffuse) + specular;"
            + " gl_FragColor = vec4(lit, uOpacity);"
            + "}";

    private final HTMLCanvasElement canvas;
    private final WebGLRenderingContext gl;
    private final WebGLProgram program;
    private final Locations locations;
    private final Map<BufferGeometry, GpuGeometry> geometries = new IdentityHashMap<>();
    private int drawCalls;
    private int triangles;

    public WebGLRenderer(HTMLCanvasElement canvas) {
        this(canvas, false);
    }

    public WebGLRenderer(HTMLCanvasElement canvas, boolean preserveDrawingBuffer) {
        if (canvas == null) {
            throw new IllegalArgumentException("Renderer canvas must not be null");
        }
        this.canvas = canvas;
        WebGLContextAttributes attributes = WebGLContextAttributes.create();
        attributes.setAntialias(true);
        attributes.setPreserveDrawingBuffer(preserveDrawingBuffer);
        gl = canvas.getContext("webgl", attributes).cast();
        if (gl == null) {
            throw new IllegalStateException("WebGL is not available");
        }
        program = createProgram(gl);
        locations = new Locations(gl, program);
        gl.useProgram(program);
        gl.enable(WebGLRenderingContext.DEPTH_TEST);
        gl.depthFunc(WebGLRenderingContext.LEQUAL);
        gl.enable(WebGLRenderingContext.BLEND);
        gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
        gl.disable(WebGLRenderingContext.CULL_FACE);
    }

    public void setSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Renderer dimensions must be positive");
        }
        canvas.setWidth(width);
        canvas.setHeight(height);
        gl.viewport(0, 0, width, height);
    }

    public void render(Scene scene, Camera camera) {
        if (scene == null || camera == null) {
            throw new IllegalArgumentException("Render requires a scene and camera");
        }
        scene.updateWorldMatrix();
        camera.updateViewMatrix();
        LightState lights = new LightState();
        collectLights(scene, lights);
        uploadLights(lights, camera.worldPosition());

        gl.viewport(0, 0, canvas.getWidth(), canvas.getHeight());
        gl.clearColor(scene.background.r, scene.background.g, scene.background.b, 1);
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT | WebGLRenderingContext.DEPTH_BUFFER_BIT);
        gl.useProgram(program);
        gl.uniformMatrix4fv(locations.view, false, Float32Array.copyFromJavaArray(camera.viewMatrix.elements()));
        gl.uniformMatrix4fv(locations.projection, false,
                Float32Array.copyFromJavaArray(camera.projectionMatrix.elements()));
        drawCalls = 0;
        triangles = 0;
        if (scene.visible) {
            renderNode(scene);
        }
        gl.flush();
    }

    public int drawCalls() {
        return drawCalls;
    }

    public int triangles() {
        return triangles;
    }

    public WebGLRenderingContext context() {
        return gl;
    }

    public void dispose() {
        for (GpuGeometry geometry : geometries.values()) {
            gl.deleteBuffer(geometry.positions);
            gl.deleteBuffer(geometry.normals);
            gl.deleteBuffer(geometry.indices);
        }
        geometries.clear();
        gl.deleteProgram(program);
    }

    private void renderNode(Object3D node) {
        if (!node.visible) {
            return;
        }
        if (node instanceof Mesh mesh) {
            renderMesh(mesh);
        }
        for (Object3D child : node.children()) {
            renderNode(child);
        }
    }

    private void renderMesh(Mesh mesh) {
        BufferGeometry source = mesh.geometry();
        GpuGeometry geometry = geometries.computeIfAbsent(source, key -> new GpuGeometry(gl, key));
        Material material = mesh.material();
        gl.uniformMatrix4fv(locations.model, false, Float32Array.copyFromJavaArray(mesh.matrixWorld.elements()));
        gl.uniformMatrix3fv(locations.normal, false, Float32Array.copyFromJavaArray(normalMatrix(mesh.matrixWorld)));
        gl.uniform3f(locations.color, material.color.r, material.color.g, material.color.b);
        gl.uniform1f(locations.opacity, material.opacity);
        gl.uniform1i(locations.unlit, material.unlit() ? 1 : 0);
        if (material instanceof MeshPhongMaterial phong) {
            gl.uniform3f(locations.emissive, phong.emissive.r, phong.emissive.g, phong.emissive.b);
            gl.uniform3f(locations.specular, phong.specular.r, phong.specular.g, phong.specular.b);
            gl.uniform1f(locations.shininess, Math.max(1, phong.shininess));
        } else {
            gl.uniform3f(locations.emissive, 0, 0, 0);
            gl.uniform3f(locations.specular, 0, 0, 0);
            gl.uniform1f(locations.shininess, 1);
        }
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, geometry.positions);
        gl.enableVertexAttribArray(locations.position);
        gl.vertexAttribPointer(locations.position, 3, WebGLRenderingContext.FLOAT, false, 0, 0);
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, geometry.normals);
        gl.enableVertexAttribArray(locations.normalAttribute);
        gl.vertexAttribPointer(locations.normalAttribute, 3, WebGLRenderingContext.FLOAT, false, 0, 0);
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, geometry.indices);
        gl.drawElements(WebGLRenderingContext.TRIANGLES, source.triangleCount() * 3,
                WebGLRenderingContext.UNSIGNED_SHORT, 0);
        drawCalls++;
        triangles += source.triangleCount();
    }

    private static float[] normalMatrix(Matrix4f model) {
        float[] inverse = model.copy().invert().elements();
        return new float[] {
            inverse[0], inverse[4], inverse[8],
            inverse[1], inverse[5], inverse[9],
            inverse[2], inverse[6], inverse[10]
        };
    }

    private static void collectLights(Object3D node, LightState state) {
        if (!node.visible) {
            return;
        }
        if (node instanceof AmbientLight light) {
            state.ambient.r += light.color.r * light.intensity;
            state.ambient.g += light.color.g * light.intensity;
            state.ambient.b += light.color.b * light.intensity;
        } else if (node instanceof DirectionalLight light && state.directional == null) {
            state.directional = light;
        } else if (node instanceof PointLight light && state.point == null) {
            state.point = light;
        }
        for (Object3D child : node.children()) {
            collectLights(child, state);
        }
    }

    private void uploadLights(LightState state, Vector3f cameraPosition) {
        gl.uniform3f(locations.ambient, state.ambient.r, state.ambient.g, state.ambient.b);
        gl.uniform3f(locations.cameraPosition, cameraPosition.x, cameraPosition.y, cameraPosition.z);
        if (state.directional == null) {
            gl.uniform3f(locations.directionalColor, 0, 0, 0);
            gl.uniform3f(locations.directionalDirection, 0, 1, 0);
        } else {
            DirectionalLight light = state.directional;
            light.target.updateWorldMatrix();
            Vector3f direction = light.worldPosition().subtract(light.target.worldPosition()).normalize();
            gl.uniform3f(locations.directionalColor, light.color.r * light.intensity,
                    light.color.g * light.intensity, light.color.b * light.intensity);
            gl.uniform3f(locations.directionalDirection, direction.x, direction.y, direction.z);
        }
        if (state.point == null) {
            gl.uniform3f(locations.pointColor, 0, 0, 0);
            gl.uniform3f(locations.pointPosition, 0, 0, 0);
            gl.uniform1f(locations.pointDistance, 0);
        } else {
            PointLight light = state.point;
            Vector3f position = light.worldPosition();
            gl.uniform3f(locations.pointColor, light.color.r * light.intensity,
                    light.color.g * light.intensity, light.color.b * light.intensity);
            gl.uniform3f(locations.pointPosition, position.x, position.y, position.z);
            gl.uniform1f(locations.pointDistance, Math.max(0, light.distance));
        }
    }

    private static WebGLProgram createProgram(WebGLRenderingContext gl) {
        WebGLShader vertex = compileShader(gl, WebGLRenderingContext.VERTEX_SHADER, VERTEX_SHADER);
        WebGLShader fragment = compileShader(gl, WebGLRenderingContext.FRAGMENT_SHADER, FRAGMENT_SHADER);
        WebGLProgram program = gl.createProgram();
        gl.attachShader(program, vertex);
        gl.attachShader(program, fragment);
        gl.linkProgram(program);
        gl.deleteShader(vertex);
        gl.deleteShader(fragment);
        if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
            String message = gl.getProgramInfoLog(program);
            gl.deleteProgram(program);
            throw new IllegalStateException("WebGL link failed: " + message);
        }
        return program;
    }

    private static WebGLShader compileShader(WebGLRenderingContext gl, int type, String source) {
        WebGLShader shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
            String message = gl.getShaderInfoLog(shader);
            gl.deleteShader(shader);
            throw new IllegalStateException("WebGL shader failed: " + message);
        }
        return shader;
    }

    private static final class GpuGeometry {
        final WebGLBuffer positions;
        final WebGLBuffer normals;
        final WebGLBuffer indices;

        GpuGeometry(WebGLRenderingContext gl, BufferGeometry geometry) {
            positions = gl.createBuffer();
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, positions);
            gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
                    Float32Array.copyFromJavaArray(geometry.positions()), WebGLRenderingContext.STATIC_DRAW);
            normals = gl.createBuffer();
            gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, normals);
            gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
                    Float32Array.copyFromJavaArray(geometry.normals()), WebGLRenderingContext.STATIC_DRAW);
            indices = gl.createBuffer();
            gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indices);
            gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER,
                    Uint16Array.copyFromJavaArray(geometry.indices()), WebGLRenderingContext.STATIC_DRAW);
        }
    }

    private static final class LightState {
        final Color ambient = new Color(0, 0, 0);
        DirectionalLight directional;
        PointLight point;
    }

    private static final class Locations {
        final int position;
        final int normalAttribute;
        final WebGLUniformLocation model;
        final WebGLUniformLocation view;
        final WebGLUniformLocation projection;
        final WebGLUniformLocation normal;
        final WebGLUniformLocation color;
        final WebGLUniformLocation emissive;
        final WebGLUniformLocation specular;
        final WebGLUniformLocation opacity;
        final WebGLUniformLocation shininess;
        final WebGLUniformLocation unlit;
        final WebGLUniformLocation ambient;
        final WebGLUniformLocation directionalColor;
        final WebGLUniformLocation directionalDirection;
        final WebGLUniformLocation pointColor;
        final WebGLUniformLocation pointPosition;
        final WebGLUniformLocation pointDistance;
        final WebGLUniformLocation cameraPosition;

        Locations(WebGLRenderingContext gl, WebGLProgram program) {
            position = gl.getAttribLocation(program, "aPosition");
            normalAttribute = gl.getAttribLocation(program, "aNormal");
            model = gl.getUniformLocation(program, "uModel");
            view = gl.getUniformLocation(program, "uView");
            projection = gl.getUniformLocation(program, "uProjection");
            normal = gl.getUniformLocation(program, "uNormal");
            color = gl.getUniformLocation(program, "uColor");
            emissive = gl.getUniformLocation(program, "uEmissive");
            specular = gl.getUniformLocation(program, "uSpecular");
            opacity = gl.getUniformLocation(program, "uOpacity");
            shininess = gl.getUniformLocation(program, "uShininess");
            unlit = gl.getUniformLocation(program, "uUnlit");
            ambient = gl.getUniformLocation(program, "uAmbient");
            directionalColor = gl.getUniformLocation(program, "uDirectionalColor");
            directionalDirection = gl.getUniformLocation(program, "uDirectionalDirection");
            pointColor = gl.getUniformLocation(program, "uPointColor");
            pointPosition = gl.getUniformLocation(program, "uPointPosition");
            pointDistance = gl.getUniformLocation(program, "uPointDistance");
            cameraPosition = gl.getUniformLocation(program, "uCameraPosition");
        }
    }
}
