#!/usr/bin/env python3
import base64
import json
import math
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/webapp/models"
OUTPUT.mkdir(parents=True, exist_ok=True)

FACES = [
    ([(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)], (0,0,1)),
    ([(1,-1,-1),(-1,-1,-1),(-1,1,-1),(1,1,-1)], (0,0,-1)),
    ([(1,-1,1),(1,-1,-1),(1,1,-1),(1,1,1)], (1,0,0)),
    ([(-1,-1,-1),(-1,-1,1),(-1,1,1),(-1,1,-1)], (-1,0,0)),
    ([(-1,1,1),(1,1,1),(1,1,-1),(-1,1,-1)], (0,1,0)),
    ([(-1,-1,-1),(1,-1,-1),(1,-1,1),(-1,-1,1)], (0,-1,0)),
]

positions = []
normals = []
indices = []
for face, normal in FACES:
    start = len(positions) // 3
    for vertex in face:
        positions.extend(vertex)
        normals.extend(normal)
    indices.extend((start, start + 1, start + 2, start, start + 2, start + 3))

position_bytes = struct.pack("<" + "f" * len(positions), *positions)
normal_bytes = struct.pack("<" + "f" * len(normals), *normals)
index_bytes = struct.pack("<" + "H" * len(indices), *indices)
position_accessor = {"bufferView": 0, "componentType": 5126, "count": 24, "type": "VEC3", "min": [-1,-1,-1], "max": [1,1,1]}
normal_accessor = {"bufferView": 1, "componentType": 5126, "count": 24, "type": "VEC3"}
index_accessor = {"bufferView": 2, "componentType": 5123, "count": 36, "type": "SCALAR"}


def material(color, metallic=0.1, roughness=0.3):
    return {"pbrMetallicRoughness": {"baseColorFactor": color, "metallicFactor": metallic, "roughnessFactor": roughness}}


def write(name, document):
    (OUTPUT / name).write_text(json.dumps(document, indent=2) + "\n")


embedded = position_bytes + normal_bytes + index_bytes
write("cube-embedded.gltf", {
    "asset": {"version": "2.0", "generator": "lm3d example generator"},
    "scene": 0,
    "scenes": [{"nodes": [0]}],
    "nodes": [{"name": "Embedded cube", "mesh": 0, "translation": [0, 1.25, 0], "rotation": [0, 0.3826834324, 0, 0.9238795325]}],
    "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "material": 0}]}],
    "materials": [material([0.38, 0.84, 0.77, 1], 0.15, 0.25)],
    "accessors": [position_accessor, normal_accessor, index_accessor],
    "bufferViews": [
        {"buffer": 0, "byteOffset": 0, "byteLength": len(position_bytes), "target": 34962},
        {"buffer": 0, "byteOffset": len(position_bytes), "byteLength": len(normal_bytes), "target": 34962},
        {"buffer": 0, "byteOffset": len(position_bytes) + len(normal_bytes), "byteLength": len(index_bytes), "target": 34963},
    ],
    "buffers": [{"uri": "data:application/octet-stream;base64," + base64.b64encode(embedded).decode(), "byteLength": len(embedded)}],
})

external = position_bytes + index_bytes
(OUTPUT / "hierarchy.bin").write_bytes(external)
write("hierarchy.gltf", {
    "asset": {"version": "2.0", "generator": "lm3d example generator"},
    "scene": 0,
    "scenes": [{"nodes": [0]}],
    "nodes": [
        {"name": "Hierarchy root", "translation": [0, 1.1, 0], "children": [1, 2]},
        {"name": "Mint child", "mesh": 0, "translation": [-1.45, 0, 0], "rotation": [0, 0, 0.2588190451, 0.9659258263], "scale": [0.7, 0.7, 0.7]},
        {"name": "Gold child", "mesh": 1, "translation": [1.45, 0, 0], "rotation": [0, 0, -0.2588190451, 0.9659258263], "scale": [0.7, 0.7, 0.7]},
    ],
    "meshes": [
        {"primitives": [{"attributes": {"POSITION": 0}, "indices": 1, "material": 0}]},
        {"primitives": [{"attributes": {"POSITION": 0}, "indices": 1, "material": 1}]},
    ],
    "materials": [material([0.38, 0.84, 0.77, 1]), material([1, 0.65, 0.25, 1], 0.4, 0.35)],
    "accessors": [position_accessor, {"bufferView": 1, "componentType": 5123, "count": 36, "type": "SCALAR"}],
    "bufferViews": [
        {"buffer": 0, "byteOffset": 0, "byteLength": len(position_bytes), "target": 34962},
        {"buffer": 0, "byteOffset": len(position_bytes), "byteLength": len(index_bytes), "target": 34963},
    ],
    "buffers": [{"uri": "hierarchy.bin", "byteLength": len(external)}],
})

radial_segments = 32
tubular_segments = 16
radius = 1.15
tube = 0.42
vertices = []
torus_indices = []
for j in range(tubular_segments + 1):
    v = j / tubular_segments * math.tau
    for i in range(radial_segments + 1):
        u = i / radial_segments * math.tau
        cv, sv = math.cos(v), math.sin(v)
        cu, su = math.cos(u), math.sin(u)
        vertices.extend(((radius + tube * cv) * cu, tube * sv, (radius + tube * cv) * su, cv * cu, sv, cv * su))
for j in range(tubular_segments):
    for i in range(radial_segments):
        a = j * (radial_segments + 1) + i
        b = a + 1
        d = (j + 1) * (radial_segments + 1) + i
        c = d + 1
        torus_indices.extend((a, d, b, b, d, c))
vertex_bytes = struct.pack("<" + "f" * len(vertices), *vertices)
torus_index_bytes = struct.pack("<" + "H" * len(torus_indices), *torus_indices)
torus_buffer = vertex_bytes + torus_index_bytes
(OUTPUT / "torus.bin").write_bytes(torus_buffer)
write("torus-interleaved.gltf", {
    "asset": {"version": "2.0", "generator": "lm3d example generator"},
    "scenes": [{"nodes": [0]}],
    "nodes": [{"name": "Matrix torus", "mesh": 0, "matrix": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,1.4,0,1]}],
    "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2}]}],
    "accessors": [
        {"bufferView": 0, "byteOffset": 0, "componentType": 5126, "count": 561, "type": "VEC3", "min": [-1.57,-0.42,-1.57], "max": [1.57,0.42,1.57]},
        {"bufferView": 0, "byteOffset": 12, "componentType": 5126, "count": 561, "type": "VEC3"},
        {"bufferView": 1, "componentType": 5123, "count": 3072, "type": "SCALAR"},
    ],
    "bufferViews": [
        {"buffer": 0, "byteOffset": 0, "byteLength": len(vertex_bytes), "byteStride": 24, "target": 34962},
        {"buffer": 0, "byteOffset": len(vertex_bytes), "byteLength": len(torus_index_bytes), "target": 34963},
    ],
    "buffers": [{"uri": "torus.bin", "byteLength": len(torus_buffer)}],
})

print("Generated cube-embedded.gltf, hierarchy.gltf + hierarchy.bin, torus-interleaved.gltf + torus.bin")
